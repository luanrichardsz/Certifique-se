package br.com.certifiquese.dto;

import java.time.LocalDateTime;

public record PerfilPublicoResponseDTO(
        String nomeUsuario,
        String username,
        String headline,
        String biografia,
        LocalDateTime membroDesde,
        int totalCertificados,
        int totalHoras
) {
}
