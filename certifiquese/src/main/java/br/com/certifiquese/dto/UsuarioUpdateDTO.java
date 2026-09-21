package br.com.certifiquese.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UsuarioUpdateDTO(
        @NotBlank(message = "O nome do usuário é obrigatório")
        @Size(max = 100, message = "O nome do usuário deve ter no máximo 100 caracteres")
        String nomeUsuario,

        @NotBlank(message = "O e-mail é obrigatório")
        @Email(message = "O e-mail deve ter um formato válido")
        @Size(max = 150, message = "O e-mail deve ter no máximo 150 caracteres")
        String email,

        @Pattern(regexp = "^[a-zA-Z0-9._-]+$", message = "O username só pode conter letras, números, ponto, hífen e underline")
        @Size(min = 3, max = 50, message = "O username deve ter entre 3 e 50 caracteres")
        String username,

        @Size(max = 150, message = "O título profissional deve ter no máximo 150 caracteres")
        String headline,

        @Size(max = 500, message = "A biografia deve ter no máximo 500 caracteres")
        String biografia,

        Boolean perfilPublico
) {
}
