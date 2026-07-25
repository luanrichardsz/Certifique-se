package br.com.certifiquese.dto;

import br.com.certifiquese.model.Role;

import java.time.LocalDateTime;

public record UsuarioResponseDTO(
        Long idUsuario,
        String nomeUsuario,
        String email,
        String biografia,
        Role role,
        LocalDateTime criadoEm
) {
}
