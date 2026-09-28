package br.com.certifiquese.dto;

import br.com.certifiquese.model.Role;

import java.time.LocalDateTime;

public record UsuarioResponseDTO(
        Long idUsuario,
        String nomeUsuario,
        String username,
        String email,
        String headline,
        String biografia,
        String foto,
        Boolean perfilPublico,
        Role role,
        LocalDateTime criadoEm
) {
}
