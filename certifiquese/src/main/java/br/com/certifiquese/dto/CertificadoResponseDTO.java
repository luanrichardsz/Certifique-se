package br.com.certifiquese.dto;

import java.time.LocalDate;
import java.util.List;

public record CertificadoResponseDTO(
        Long idCertificado,
        String hashCertificado,
        String foto,
        String nome,
        String empresa,
        LocalDate dataConclusao,
        List<String> tags
) {
}
