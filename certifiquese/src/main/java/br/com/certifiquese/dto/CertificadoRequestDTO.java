package br.com.certifiquese.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

public record CertificadoRequestDTO(
        @NotBlank(message = "A foto do certificado é obrigatória")
        String foto,

        @NotBlank(message = "O nome do certificado é obrigatório")
        @Size(max = 150, message = "O nome do certificado deve ter no máximo 150 caracteres")
        String nome,

        @NotBlank(message = "A empresa emissora é obrigatória")
        @Size(max = 150, message = "A empresa emissora deve ter no máximo 150 caracteres")
        String empresa,

        @NotNull(message = "A data de conclusão é obrigatória")
        LocalDate dataConclusao,

        @NotEmpty(message = "Informe pelo menos uma tag")
        List<@NotBlank(message = "A tag não pode estar vazia") String> tags
) {
}
