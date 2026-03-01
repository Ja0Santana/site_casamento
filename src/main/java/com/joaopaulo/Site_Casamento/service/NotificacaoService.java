package com.joaopaulo.Site_Casamento.service;

import com.joaopaulo.Site_Casamento.model.Pagamento;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificacaoService {

    @Value("${EMAIL_CASAL:paulo.jp806@gmail.com}")
    private String emailCasal;

    private final EmailService emailService;

    @Async
    public void enviarNotificacoesSucesso(Pagamento pagamento) {
        String valorFormatado = String.format("R$ %.2f", pagamento.getValor());

        Context ctxConvidado = getContext(pagamento, valorFormatado);

        emailService.enviarEmailHtml(
                pagamento.getEmail(),
                "Obrigado pelo seu presente! 💕",
                ctxConvidado,
                "agradecimento-presente");

        // --- 2. CONFIGURAÇÃO PARA O CASAL (Notificação) ---
        Context ctxCasal = getContext(pagamento, valorFormatado);

        emailService.enviarEmailHtml(
                emailCasal,
                "Vocês receberam um presente! 🎁",
                ctxCasal,
                "notificacao-presente");
    }

    private static Context getContext(Pagamento pagamento, String valorFormatado) {
        Context ctxConvidadoOuCasal = new Context();
        ctxConvidadoOuCasal.setVariable("nomePresente", pagamento.getNome());
        ctxConvidadoOuCasal.setVariable("nomeProduto", pagamento.getDescricao());
        ctxConvidadoOuCasal.setVariable("valorProduto", valorFormatado);
        ctxConvidadoOuCasal.setVariable("dataEvento", "21 de Junho de 2026"); // Valor fixo
        ctxConvidadoOuCasal.setVariable("localEvento", "Igreja Matriz de Nossa Senhora do Patrocínio");
        ctxConvidadoOuCasal.setVariable("nomesCasal", "Nome & Nome");
        ctxConvidadoOuCasal.setVariable("mensagemCarinho", ""); // Se não tiver no banco, mande vazio
        return ctxConvidadoOuCasal;
    }
}
