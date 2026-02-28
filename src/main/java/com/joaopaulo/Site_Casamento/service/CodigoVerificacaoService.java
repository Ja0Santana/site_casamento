package com.joaopaulo.Site_Casamento.service;

import com.joaopaulo.Site_Casamento.model.CodigoVerificacao;
import com.joaopaulo.Site_Casamento.repository.CodigoVerificacaoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class CodigoVerificacaoService {

    private final CodigoVerificacaoRepository repository;
    private final EmailService emailService;

    @Transactional
    public Map<String, String> gerarEnviarCodigo(String email) {
        String codigo = String.format("%05d", new Random().nextInt(100000));

        CodigoVerificacao cv = repository.findByEmail(email).orElse(new CodigoVerificacao());
        cv.setEmail(email);
        cv.setCodigo(codigo);
        cv.setDataExpiracao(LocalDateTime.now().plusMinutes(10));
        repository.save(cv);

        emailService.enviarEmailVerificacao(email, codigo);

        return Map.of("mensagem", "Código enviado com sucesso!");
    }

    public Map<String, Object> validarCodigo(String email, String codigo) {
        boolean eValido = repository.findByEmail(email)
                .filter(cv -> cv.getCodigo().equals(codigo))
                .filter(cv -> cv.getDataExpiracao().isAfter(LocalDateTime.now()))
                .map(cv -> {
                    repository.delete(cv);
                    return true;
                }).orElse(false);

        return Map.of("valido", eValido);
    }

    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void limparCodigosExpirados() {
        repository.deleteByDataExpiracaoBefore(LocalDateTime.now());
    }
}