package br.com.certifiquese.exception;

public class RecuperacaoSenhaInvalidaException extends RuntimeException {

    public RecuperacaoSenhaInvalidaException(String message) {
        super(message);
    }
}