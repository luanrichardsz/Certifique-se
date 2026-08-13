package br.com.certifiquese.service.email;

public interface EmailSender {

    void enviar(String destinatario, String assunto, String corpo);
}