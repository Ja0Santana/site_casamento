package com.joaopaulo.Site_Casamento.service;

import com.joaopaulo.Site_Casamento.model.CodigoVerificacao;
import com.joaopaulo.Site_Casamento.repository.CodigoVerificacaoRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.Random;

@Service
@RequiredArgsConstructor
@Slf4j // Adicione o log para monitorar o Railway
public class CodigoVerificacaoService {

    private final CodigoVerificacaoRepository repository;
    private final EmailService emailService;

    @Transactional
    public Map<String, String> gerarEnviarCodigo(String email) {
        // Gerar código de 5 dígitos
        String codigo = String.format("%05d", new Random().nextInt(100000));

        // Busca existente ou cria novo (Estratégia de Upsert)
        CodigoVerificacao cv = repository.findByEmail(email).orElse(new CodigoVerificacao());
        cv.setEmail(email);
        cv.setCodigo(codigo);
        cv.setDataExpiracao(LocalDateTime.now().plusMinutes(10));

        repository.save(cv);

        try {
            emailService.enviarEmailVerificacao(email, codigo);
            return Map.of("mensagem", "Código enviado com sucesso para " + email);
        } catch (Exception e) {
            log.error("Falha ao enviar e-mail de código: {}", e.getMessage());
            // Opcional: deletar o código do banco se o envio falhar
            throw new RuntimeException("Falha ao enviar e-mail. Tente novamente em instantes.");
        }
    }

    @Transactional
    public Map<String, Object> validarCodigo(String email, String codigo) {
        Optional<CodigoVerificacao> opt = repository.findByEmail(email);

        if (opt.isEmpty()) {
            return Map.of("valido", false, "mensagem", "Código não encontrado.");
        }

        CodigoVerificacao cv = opt.get();

        // Verifica expiração
        if (cv.getDataExpiracao().isBefore(LocalDateTime.now())) {
            repository.delete(cv);
            return Map.of("valido", false, "mensagem", "Código expirado.");
        }

        // Verifica correspondência
        if (cv.getCodigo().equals(codigo)) {
            repository.delete(cv); // Consome o código após o uso
            return Map.of("valido", true, "mensagem", "Sucesso!");
        }

        return Map.of("valido", false, "mensagem", "Código incorreto.");
    }

    @Scheduled(cron = "0 0 * * * *") // Roda a cada hora
    @Transactional
    public void limparCodigosExpirados() {
        log.info("Executando limpeza de códigos expirados...");
        repository.deleteByDataExpiracaoBefore(LocalDateTime.now());
    }
}