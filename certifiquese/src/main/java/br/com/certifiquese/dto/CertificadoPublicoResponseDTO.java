package br.com.certifiquese.dto;

import java.time.LocalDate;
import java.util.List;

public record CertificadoPublicoResponseDTO(
        String hashCertificado,
        String foto,
        String nome,
        String empresa,
        LocalDate dataConclusao,
        List<String> tags,
        Integer cargaHoraria,
        String descricao,
        String linkValidacao
) {
}
