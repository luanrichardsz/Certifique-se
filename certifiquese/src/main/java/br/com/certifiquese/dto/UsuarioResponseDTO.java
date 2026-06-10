package br.com.certifiquese.dto;

public record UsuarioResponseDTO(
        Long idUsuario,
        String nomeUsuario,
        String email,
        String biografia
) {
}
