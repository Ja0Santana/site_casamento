package com.joaopaulo.Site_Casamento.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.joaopaulo.Site_Casamento.enums.StatusPagamento;
import com.joaopaulo.Site_Casamento.dto.in.PagamentoRequestDTO;
import com.joaopaulo.Site_Casamento.dto.out.PagamentoResponseDTO;
import com.joaopaulo.Site_Casamento.mapper.PagamentoMapper;
import com.joaopaulo.Site_Casamento.model.Pagamento;
import com.joaopaulo.Site_Casamento.repository.PagamentoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PagamentoService {

    private final RestClient restClient;
    private final PagamentoRepository pagamentoRepository;
    private final PagamentoMapper pagamentoMapper;
    private final NotificacaoService notificacaoService; // SRP: Delegamos a notificação

    @Value("${mercadopago.public-key}")
    private String publicKey;

    public PagamentoResponseDTO criarPagamentoPix(PagamentoRequestDTO dto) {
        // Agora usamos o dto.descricao() que você adicionou
        Map<String, Object> requestBody = Map.of(
                "transaction_amount", dto.valor(),
                "description", "Presente: " + dto.descricao(),
                "payment_method_id", "pix",
                "payer", Map.of("email", dto.email()));

        JsonNode response = restClient.post()
                .uri("/v1/payments")
                .header("X-Idempotency-Key", UUID.randomUUID().toString())
                .body(requestBody)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, (req, res) -> {
                    byte[] body = res.getBody().readAllBytes();
                    log.error("Erro MP ao criar: {}", new String(body));
                    throw new RuntimeException("Erro ao processar pagamento no Mercado Pago");
                })
                .body(JsonNode.class);

        if (response == null) {
            throw new RuntimeException("Resposta nula do Mercado Pago");
        }

        Long mpId = response.get("id").asLong();
        String statusStr = response.get("status").asText();
        String qrCode = response.get("point_of_interaction").get("transaction_data").get("qr_code").asText();
        String qrCodeBase64 = response.get("point_of_interaction").get("transaction_data").get("qr_code_base64")
                .asText();
        String dataCriacao = response.get("date_created").asText();

        // Salvando com a descrição vinda do DTO
        Pagamento pagamento = Pagamento.builder()
                .mercadoPagoId(mpId)
                .valor(dto.valor())
                .statusPagamento(StatusPagamento.fromString(statusStr))
                .email(dto.email())
                .nome(dto.nome())
                .descricao(dto.descricao()) // IMPORTANTE: Salvar para usar no e-mail depois
                .qrCode(qrCode)
                .dataPagamento(dataCriacao)
                .build();

        pagamentoRepository.save(pagamento);
        log.info("Pagamento criado. ID MP: {}, Status: {}", mpId, pagamento.getStatusPagamento());

        return pagamentoMapper.toResponseDTO(pagamento, qrCode, qrCodeBase64);
    }

    public void atualizarStatusPagamento(Long mercadoPagoId, Map<String, Object> payload) {
        Optional<Pagamento> pagamentoOpt = pagamentoRepository.findByMercadoPagoId(mercadoPagoId);

        if (pagamentoOpt.isEmpty()) {
            log.warn("Pagamento com ID MP {} não encontrado para atualização", mercadoPagoId);
            return;
        }

        Pagamento pagamento = pagamentoOpt.get();
        String statusStr = obterStatusMercadoPago(mercadoPagoId, payload);
        StatusPagamento novoStatus = StatusPagamento.fromString(statusStr);

        // Lógica de transição de status
        if (pagamento.getStatusPagamento() != novoStatus) {
            pagamento.setStatusPagamento(novoStatus);
            pagamentoRepository.save(pagamento);
            log.info("Pagamento {} atualizado para {}", mercadoPagoId, novoStatus);

            // REGRA DE OURO: Notificar apenas na aprovação
            if (novoStatus == StatusPagamento.APROVADO) {
                log.info("Disparando e-mails de confirmação para o ID {}", mercadoPagoId);
                notificacaoService.enviarNotificacoesSucesso(pagamento);
            }
        }
    }

    public void processarPagamentoCard(Map<String, Object> payload) {
        log.info("Processando pagamento via Cartão: {}", payload);

        // Extraímos os dados do site que enviamos no Objeto 'siteData' no JS
        Map<String, Object> siteData = (Map<String, Object>) payload.get("siteData");

        // Removemos o siteData do payload para enviar o restante para o MP
        payload.remove("siteData");

        JsonNode response = restClient.post()
                .uri("/v1/payments")
                .header("X-Idempotency-Key", UUID.randomUUID().toString())
                .body(payload)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, (req, res) -> {
                    byte[] body = res.getBody().readAllBytes();
                    log.error("Erro MP ao criar cartão: {}", new String(body));
                    throw new RuntimeException("Erro ao processar pagamento de cartão no Mercado Pago");
                })
                .body(JsonNode.class);

        if (response != null && response.has("id")) {
            Long mpId = response.get("id").asLong();
            String statusMP = response.get("status").asText();

            // Salvamos o pagamento com os dados que vieram do site
            Pagamento pagamento = Pagamento.builder()
                    .mercadoPagoId(mpId)
                    .valor(new java.math.BigDecimal(siteData.get("valor").toString()))
                    .email(siteData.get("email").toString())
                    .nome(siteData.get("nome").toString())
                    .descricao(siteData.get("descricao").toString())
                    .statusPagamento(StatusPagamento.fromString(statusMP))
                    .dataPagamento(response.get("date_created").asText())
                    .build();

            pagamentoRepository.save(pagamento);

            // Se for aprovado agora, já notifica
            if (pagamento.getStatusPagamento() == StatusPagamento.APROVADO) {
                notificacaoService.enviarNotificacoesSucesso(pagamento);
            }
        }
    }

    private String obterStatusMercadoPago(Long mercadoPagoId, Map<String, Object> payload) {
        if (payload.containsKey("status") && payload.get("status") != null) {
            return payload.get("status").toString();
        }

        JsonNode response = restClient.get()
                .uri("/v1/payments/" + mercadoPagoId)
                .retrieve()
                .body(JsonNode.class);

        return (response != null && response.has("status")) ? response.get("status").asText() : "pending";
    }

    public String obterPublicKey() {
        if (publicKey == null || publicKey.isBlank()) {
            log.error("Configuração Crítica: mercado-pago.public-key não encontrada no application.properties");
            throw new IllegalStateException("Chave pública do Mercado Pago não configurada.");
        }
        log.info("Enviando chave pública do Mercado Pago para o frontend");
        return publicKey;
    }
}