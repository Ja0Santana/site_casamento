package com.joaopaulo.Site_Casamento.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.joaopaulo.Site_Casamento.dto.in.PagamentoRequestDTO;
import com.joaopaulo.Site_Casamento.dto.out.PagamentoResponseDTO;
import com.joaopaulo.Site_Casamento.enums.StatusPagamento;
import com.joaopaulo.Site_Casamento.exceptions.NullDataException;
import com.joaopaulo.Site_Casamento.mapper.PagamentoMapper;
import com.joaopaulo.Site_Casamento.model.Pagamento;
import com.joaopaulo.Site_Casamento.repository.PagamentoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PagamentoService {

    private final RestClient restClient;
    private final PagamentoRepository pagamentoRepository;
    private final PagamentoMapper pagamentoMapper;
    private final NotificacaoService notificacaoService;

    @Value("${mercadopago.public-key}")
    private String publicKey;

    @Value("${app.base-url:https://sitecasamento-production.up.railway.app}")
    private String appBaseUrl;

    public PagamentoResponseDTO criarPagamentoPix(PagamentoRequestDTO dto) {
        if (dto.valor().compareTo(new java.math.BigDecimal("10.00")) < 0)
            throw new RuntimeException("O valor mínimo para presente é de R$ 10,00");

        Map<String, String> nomes = extrairNomeSobrenome(dto.nome());

        Map<String, Object> requestBody = Map.of(
                "transaction_amount", dto.valor(),
                "description", "Presente: " + dto.descricao(),
                "payment_method_id", "pix",
                "notification_url", appBaseUrl + "/v1/webhooks/mercado-pago",
                "external_reference", UUID.randomUUID().toString(),
                "payer", Map.of(
                        "email", dto.email(),
                        "first_name", nomes.get("first_name"),
                        "last_name", nomes.get("last_name")
                ),
                "additional_info", Map.of(
                        "items", List.of(Map.of(
                                "title", "Presente: " + dto.descricao(),
                                "description", "Presente para a lista de casamento",
                                "quantity", 1,
                                "unit_price", dto.valor()
                        ))
                )
        );

        JsonNode response = restClient.post()
                .uri("/v1/payments")
                .header("X-Idempotency-Key", UUID.randomUUID().toString())
                .header("X-Melinet-SRC", "sdk-js-v2")
                .body(requestBody)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, (req, res) -> {
                    log.error("Erro MP ao criar Pix");
                    throw new RuntimeException("Erro ao processar pagamento no Mercado Pago");
                })
                .body(JsonNode.class);

        if (response == null)
            throw new RuntimeException("Resposta nula do Mercado Pago");

        Long mpId = response.get("id").asLong();
        String statusStr = response.get("status").asText();
        JsonNode txData = response.get("point_of_interaction").get("transaction_data");

        String qrCode = txData.get("qr_code").asText();
        String qrCodeBase64 = txData.get("qr_code_base64").asText();
        String dataCriacao = response.get("date_created").asText();

        Pagamento pagamento = Pagamento.builder()
                .mercadoPagoId(mpId)
                .valor(dto.valor())
                .statusPagamento(StatusPagamento.fromString(statusStr))
                .email(dto.email())
                .nome(dto.nome())
                .descricao(dto.descricao())
                .qrCode(qrCode)
                .dataPagamento(dataCriacao)
                .build();

        pagamentoRepository.save(pagamento);
        log.info("Pagamento criado. ID MP: {}, Status: {}", mpId, pagamento.getStatusPagamento());

        return pagamentoMapper.toResponseDTO(pagamento, qrCode, qrCodeBase64);
    }

    @Transactional
    public void atualizarStatusPagamento(Long mercadoPagoId, Map<String, Object> payload) {
        Pagamento pagamento = pagamentoRepository.findByMercadoPagoId(mercadoPagoId)
                .orElseThrow(() -> new NullDataException("Pagamento ID " + mercadoPagoId + " não encontrado"));

        String statusStr = obterStatusMercadoPago(mercadoPagoId, payload);
        StatusPagamento novoStatus = StatusPagamento.fromString(statusStr);

        if (pagamento.getStatusPagamento() == novoStatus)
            return;

        pagamento.setStatusPagamento(novoStatus);
        pagamentoRepository.save(pagamento);
        log.info("Pagamento {} atualizado para {}", mercadoPagoId, novoStatus);

        if (novoStatus == StatusPagamento.APROVADO) {
            notificacaoService.enviarNotificacoesSucesso(pagamento);
        }
    }

    @Scheduled(cron = "0 0 * * * *")
    public void verificarPagamentosPendentes() {
        List<Pagamento> pendentes = pagamentoRepository.findAllByStatusPagamento(StatusPagamento.PENDENTE);
        for (Pagamento p : pendentes) {
            try {
                this.atualizarStatusPagamento(p.getMercadoPagoId(), null);
            } catch (Exception ignored) {
            }
        }
    }

    public void processarPagamentoCard(Map<String, Object> payload) {
        if (payload == null || !payload.containsKey("siteData")) {
            throw new NullDataException("Payload de cartão inválido ou sem dados do site");
        }

        log.info("Processando pagamento via Cartão...");
        Map<String, Object> siteData = (Map<String, Object>) payload.remove("siteData");

        Map<String, Object> requestBody = new java.util.HashMap<>(payload);

        String fullNome = siteData.get("nome").toString();
        Map<String, String> nomes = extrairNomeSobrenome(fullNome);

        Map<String, Object> payer = (Map<String, Object>) requestBody.get("payer");
        Map<String, Object> newPayer = new java.util.HashMap<>();
        if (payer != null) {
            newPayer.putAll(payer);
        }
        newPayer.put("email", siteData.get("email").toString());
        newPayer.put("first_name", nomes.get("first_name"));
        newPayer.put("last_name", nomes.get("last_name"));
        requestBody.put("payer", newPayer);

        Map<String, Object> additionalInfo = new java.util.HashMap<>();
        additionalInfo.put("items", List.of(Map.of(
                "title", "Presente: " + siteData.get("descricao").toString(),
                "description", "Presente para a lista de casamento",
                "quantity", 1,
                "unit_price", new java.math.BigDecimal(siteData.get("valor").toString())
        )));
        requestBody.put("additional_info", additionalInfo);

        requestBody.put("notification_url", appBaseUrl + "/v1/webhooks/mercado-pago");
        requestBody.put("external_reference", UUID.randomUUID().toString());
        requestBody.put("statement_descriptor", "CasamentoElenJP");

        JsonNode response = restClient.post()
                .uri("/v1/payments")
                .header("X-Idempotency-Key", UUID.randomUUID().toString())
                .header("X-Melinet-SRC", "sdk-js-v2")
                .body(requestBody)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, (req, res) -> {
                    log.error("Erro MP ao criar pagamento de cartão");
                    throw new RuntimeException("Erro ao processar pagamento de cartão no Mercado Pago");
                })
                .body(JsonNode.class);

        if (response == null || !response.has("id")) {
            throw new RuntimeException("Falha na criação do pagamento via cartão no Mercado Pago");
        }

        Pagamento pagamento = Pagamento.builder()
                .mercadoPagoId(response.get("id").asLong())
                .valor(new java.math.BigDecimal(siteData.get("valor").toString()))
                .email(siteData.get("email").toString())
                .nome(siteData.get("nome").toString())
                .descricao(siteData.get("descricao").toString())
                .statusPagamento(StatusPagamento.fromString(response.get("status").asText()))
                .dataPagamento(response.get("date_created").asText())
                .build();

        pagamentoRepository.save(pagamento);

        if (pagamento.getStatusPagamento() == StatusPagamento.APROVADO) {
            notificacaoService.enviarNotificacoesSucesso(pagamento);
        }
    }

    private String obterStatusMercadoPago(Long mercadoPagoId, Map<String, Object> payload) {
        log.info("Consultando status oficial para o pagamento: {}", mercadoPagoId);

        JsonNode response = restClient.get()
                .uri("/v1/payments/" + mercadoPagoId)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, (req, res) -> {
                    log.error("Erro ao consultar status oficial do pagamento {}", mercadoPagoId);
                })
                .body(JsonNode.class);

        return (response != null && response.has("status")) ? response.get("status").asText() : "pending";
    }

    public String obterPublicKey() {
        if (publicKey == null || publicKey.isBlank()) {
            throw new NullDataException("Configuração Crítica: key não encontrada");
        }
        return publicKey;
    }

    public StatusPagamento buscarStatusPorMercadoPagoId(Long mercadoPagoId) {
        return pagamentoRepository.findByMercadoPagoId(mercadoPagoId)
                .map(Pagamento::getStatusPagamento)
                .orElseThrow(() -> new NullDataException("Pagamento ID " + mercadoPagoId + " não encontrado"));
    }


    public void receberNotificacao(Map<String, Object> payload) {
        log.info("Webhook recebido: type={}, action={}", payload.get("type"), payload.get("action"));

        String type = (String) payload.get("type");

        if (!"payment".equals(type)) {
            log.warn("Notificação recebida para tipo não suportado: {}", type);
            return;
        }

        Map<String, Object> data = (Map<String, Object>) payload.get("data");

        if (data != null && data.get("id") != null) {
            try {
                Long mpId = Long.valueOf(data.get("id").toString());
                this.atualizarStatusPagamento(mpId, payload);
            } catch (Exception e) {
                log.error("Erro ao processar ID {}: {}", data.get("id"), e.getMessage());
            }
        }
    }

    private Map<String, String> extrairNomeSobrenome(String nomeCompleto) {
        String nome = nomeCompleto != null ? nomeCompleto.trim() : "";
        String firstName = nome;
        String lastName = "Silva";
        int lastSpace = nome.lastIndexOf(" ");
        if (lastSpace > 0) {
            firstName = nome.substring(0, lastSpace);
            lastName = nome.substring(lastSpace + 1);
        }
        return Map.of("first_name", firstName, "last_name", lastName);
    }
}