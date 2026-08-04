package br.com.certifiquese.dto;

import jakarta.validation.constraints.NotBlank;

public record ExcluirContaRequestDTO(
        @NotBlank(message = "A senha atual é obrigatória")
        String senhaAtual
) {
}