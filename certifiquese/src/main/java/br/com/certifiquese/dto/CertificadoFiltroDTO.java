package br.com.certifiquese.dto;

import java.time.LocalDate;

public record CertificadoFiltroDTO(
        String nome,
        String empresa,
        LocalDate dataConclusao,
        String tags
) {
}