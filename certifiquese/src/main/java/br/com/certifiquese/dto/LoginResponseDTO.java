package br.com.certifiquese.dto;

public record LoginResponseDTO (
    String token,
    String tipo,
    long expiracao
) {
}