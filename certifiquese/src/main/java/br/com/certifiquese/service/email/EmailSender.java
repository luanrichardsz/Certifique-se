package br.com.certifiquese.service.email;

public interface EmailSender {

    void enviar(String destinatario, String assunto, String corpo);

    default void enviarHtml(String destinatario, String assunto, String corpoTexto, String corpoHtml) {
        enviar(destinatario, assunto, corpoTexto);
    }
}