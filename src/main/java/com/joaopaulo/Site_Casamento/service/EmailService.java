package com.joaopaulo.Site_Casamento.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value; // Importante para ler do properties
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender javaMailSender;
    private final TemplateEngine templateEngine;

    // Injetamos o e-mail configurado no application.properties para evitar o erro de sintaxe
    @Value("${spring.mail.username}")
    private String emailRemetente;

    public void enviarEmail(String destinatario, String assunto, String mensagem) {
        try {
            log.info("Enviando email simples para: {}", destinatario);
            SimpleMailMessage email = new SimpleMailMessage();

            // Forçamos o remetente para evitar Pauli@Jão
            email.setFrom(emailRemetente);
            email.setTo(destinatario);
            email.setSubject(assunto);
            email.setText(mensagem);

            javaMailSender.send(email);
            log.info("Email enviado com sucesso para: {}", destinatario);
        } catch (Exception e) {
            log.error("Erro ao enviar email simples para: {}, erro: {}", destinatario, e.getMessage());
        }
    }

    public void enviarEmailHtml(String para, String assunto, Context context, String templateName) {
        try {
            log.info("Processando template HTML: {} para: {}", templateName, para);

            String corpoHtml = templateEngine.process(templateName, context);
            MimeMessage mimeMessage = javaMailSender.createMimeMessage();

            // Usamos UTF-8 para garantir que acentos no corpo do e-mail funcionem
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            // RESOLUÇÃO DO ERRO 555:
            // Define o remetente explicitamente. Isso substitui o MAIL FROM:<Pauli@Jão>
            helper.setFrom(emailRemetente);

            helper.setTo(para);
            helper.setSubject(assunto);
            helper.setText(corpoHtml, true);

            javaMailSender.send(mimeMessage);
            log.info("Email HTML enviado com sucesso para: {}", para);

        } catch (MessagingException e) {
            log.error("Erro de protocolo/mensageria ao enviar e-mail: {}", e.getMessage());
            throw new RuntimeException("Falha ao processar e enviar o e-mail HTML: " + e.getMessage());
        } catch (Exception e) {
            log.error("Erro inesperado no envio de e-mail: {}", e.getMessage());
            throw new RuntimeException("Erro inesperado: " + e.getMessage());
        }
    }

    @Async
    public void enviarEmailVerificacao(String para, String codigo) {
        Context context = new Context();
        context.setVariable("CODIGO_VERIFICACAO", codigo);

        enviarEmailHtml(
                para,
                "Verifique seu E-mail | João Paulo & Elen Aparecida",
                context,
                "verificacao-email" // <--- O nome deve ser idêntico ao arquivo .html
        );
    }
}