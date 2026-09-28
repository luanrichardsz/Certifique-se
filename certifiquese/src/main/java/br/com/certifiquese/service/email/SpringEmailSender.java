package br.com.certifiquese.service.email;

import java.nio.charset.StandardCharsets;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class SpringEmailSender implements EmailSender {

    private static final Logger log = LoggerFactory.getLogger(SpringEmailSender.class);

    private final ObjectProvider<JavaMailSender> javaMailSenderProvider;
    private final String mailFrom;

    public SpringEmailSender(ObjectProvider<JavaMailSender> javaMailSenderProvider,
                             @Value("${app.mail.from:Certifique-se <nao-responda@certifique-se.app>}") String mailFrom) {
        this.javaMailSenderProvider = javaMailSenderProvider;
        this.mailFrom = mailFrom;
    }

    @Override
    @Async
    public void enviar(String destinatario, String assunto, String corpo) {
        enviarHtml(destinatario, assunto, corpo, corpo);
    }

    @Override
    @Async
    public void enviarHtml(String destinatario, String assunto, String corpoTexto, String corpoHtml) {
        JavaMailSender javaMailSender = javaMailSenderProvider.getIfAvailable();
        if (javaMailSender == null) {
            log.warn("Serviço de e-mail não configurado. E-mail para {} não foi enviado.", destinatario);
            throw new IllegalStateException("Serviço de e-mail não configurado.");
        }

        try {
            MimeMessage mimeMessage = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, StandardCharsets.UTF_8.name());
            helper.setFrom(mailFrom);
            helper.setTo(destinatario);
            helper.setSubject(assunto);
            helper.setText(corpoTexto, corpoHtml);

            javaMailSender.send(mimeMessage);
            log.info("E-mail enviado com sucesso para: {}", destinatario);
        } catch (MessagingException | MailException ex) {
            log.error("Erro ao enviar e-mail para {}: {}", destinatario, ex.getMessage(), ex);
            throw new IllegalStateException("Falha ao enviar e-mail.", ex);
        }
    }
}