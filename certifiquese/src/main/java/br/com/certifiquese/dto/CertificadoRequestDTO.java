package br.com.certifiquese.dto;

import java.time.LocalDate;
import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

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
        List<@NotBlank(message = "A tag não pode estar vazia") String> tags,

        @Positive(message = "A carga horária deve ser um número positivo")
        Integer cargaHoraria,

        @Size(max = 2000, message = "A descrição deve ter no máximo 2000 caracteres")
        String descricao,

        @Size(max = 500, message = "O link de validação deve ter no máximo 500 caracteres")
        String linkValidacao,

        Boolean publico
) implements HashCertificadoProvider {
    public CertificadoRequestDTO {
        if (publico == null) {
            publico = true;
        }
    }
}
