package br.com.certifiquese.service.email;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
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
    private static final String RESEND_API_URL = "https://api.resend.com/emails";

    private final ObjectProvider<JavaMailSender> javaMailSenderProvider;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final String mailFrom;
    private final String mailPassword;
    private final HttpClient httpClient;

    public SpringEmailSender(ObjectProvider<JavaMailSender> javaMailSenderProvider,
                             @Value("${app.mail.from:Certifique-se <nao-responda@certifique-se.app>}") String mailFrom,
                             @Value("${spring.mail.password:}") String mailPassword) {
        this.javaMailSenderProvider = javaMailSenderProvider;
        this.mailFrom = mailFrom;
        this.mailPassword = mailPassword;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    @Override
    @Async
    public void enviar(String destinatario, String assunto, String corpo) {
        enviarHtml(destinatario, assunto, corpo, corpo);
    }

    @Override
    @Async
    public void enviarHtml(String destinatario, String assunto, String corpoTexto, String corpoHtml) {
        if (mailPassword != null && mailPassword.trim().startsWith("re_")) {
            enviarViaResendHttpApi(destinatario, assunto, corpoTexto, corpoHtml);
            return;
        }

        enviarViaSmtp(destinatario, assunto, corpoTexto, corpoHtml);
    }

    private void enviarViaResendHttpApi(String destinatario, String assunto, String corpoTexto, String corpoHtml) {
        try {
            Map<String, Object> payload = Map.of(
                    "from", mailFrom,
                    "to", List.of(destinatario),
                    "subject", assunto,
                    "html", corpoHtml,
                    "text", corpoTexto
            );

            String json = objectMapper.writeValueAsString(payload);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(RESEND_API_URL))
                    .header("Authorization", "Bearer " + mailPassword.trim())
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(10))
                    .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                log.info("E-mail enviado via Resend HTTP API com sucesso para: {} (Status: {})", destinatario, response.statusCode());
            } else {
                log.error("Erro na resposta do Resend HTTP API ({}): {}", response.statusCode(), response.body());
                throw new IllegalStateException("Falha ao enviar e-mail via Resend: " + response.body());
            }
        } catch (IOException | InterruptedException ex) {
            if (ex instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            log.error("Erro de comunicação com Resend HTTP API para {}: {}", destinatario, ex.getMessage(), ex);
            throw new IllegalStateException("Falha ao enviar e-mail.", ex);
        }
    }

    private void enviarViaSmtp(String destinatario, String assunto, String corpoTexto, String corpoHtml) {
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
            log.info("E-mail enviado via SMTP com sucesso para: {}", destinatario);
        } catch (MessagingException | MailException ex) {
            log.error("Erro ao enviar e-mail via SMTP para {}: {}", destinatario, ex.getMessage(), ex);
            throw new IllegalStateException("Falha ao enviar e-mail.", ex);
        }
    }
}