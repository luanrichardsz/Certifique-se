package br.com.certifiquese.dto;

import java.time.LocalDate;
import java.util.List;

public record CertificadoExtracaoResponseDTO(
        String nome,
        String empresa,
        LocalDate dataConclusao,
        Integer cargaHoraria,
        List<String> tags,
        String descricao,
        String linkValidacao,
        String fotoChave,
        String fotoUrl
) {
    public static CertificadoExtracaoResponseDTO vazio(String fotoChave, String fotoUrl) {
        return new CertificadoExtracaoResponseDTO(
                null,
                null,
                null,
                null,
                List.of(),
                null,
                null,
                fotoChave,
                fotoUrl
        );
    }
}
