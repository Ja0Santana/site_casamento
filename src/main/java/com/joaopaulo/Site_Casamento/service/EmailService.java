package com.joaopaulo.Site_Casamento.service;

import com.resend.Resend;
import com.resend.services.emails.model.CreateEmailOptions;
import com.resend.services.emails.model.CreateEmailResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final TemplateEngine templateEngine;

    @Value("${resend.api.key}")
    private String resendApiKey;

    @Value("Elen e Joao Paulo<contato@casamentoelenejoaopaulo.men>")
    private String emailRemetente;

    private Resend resend;

    private Resend getResendClient() {
        if (this.resend == null) {
            this.resend = new Resend(resendApiKey);
        }
        return this.resend;
    }

    /**
     * Envio de E-mail HTML utilizando o Resend SDK (API HTTP)
     * Isso ignora o bloqueio de portas SMTP do Railway.
     */
    public void enviarEmailHtml(String para, String assunto, Context context, String templateName) {
        try {
            log.info("Processando template HTML: {} para: {}", templateName, para);

            // 1. Gera o HTML usando o Thymeleaf normalmente
            String corpoHtml = templateEngine.process(templateName, context);

            // 2. Obtém o cliente Resend (Lazy initialization)
            Resend resendClient = getResendClient();

            // 3. Monta a requisição de envio via API
            CreateEmailOptions sendEmailRequest = CreateEmailOptions.builder()
                    .from(emailRemetente)
                    .to(para)
                    .subject(assunto)
                    .html(corpoHtml)
                    .build();

            // 4. Envia
            CreateEmailResponse response = resendClient.emails().send(sendEmailRequest);
            log.info("Email enviado via Resend com sucesso! ID: {}", response.getId());
        } catch (Exception e) {
            log.error("Erro ao enviar e-mail via Resend para {}: {}", para, e.getMessage());
        }
    }

    @Async
    public void enviarEmailVerificacao(String para, String codigo) {
        log.info("Iniciando envio assíncrono de verificação para: {}", para);
        Context context = new Context();
        context.setVariable("CODIGO_VERIFICACAO", codigo);

        enviarEmailHtml(
                para,
                "Verifique seu E-mail | João Paulo & Elen Aparecida",
                context,
                "verificacao-email");
    }
}