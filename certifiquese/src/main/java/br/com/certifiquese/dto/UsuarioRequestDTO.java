package br.com.certifiquese.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UsuarioRequestDTO(
        @NotBlank(message = "O nome do usuário é obrigatório")
        @Size(max = 100, message = "O nome do usuário deve ter no máximo 100 caracteres")
        String nomeUsuario,

        @NotBlank(message = "O e-mail é obrigatório")
        @Email(message = "O e-mail deve ter um formato válido")
        @Size(max = 150, message = "O e-mail deve ter no máximo 150 caracteres")
        String email,

        @NotBlank(message = "A senha é obrigatória")
        @Size(min = 6, max = 100, message = "A senha deve ter entre 6 e 100 caracteres")
        String senha,

        @Size(max = 500, message = "A biografia deve ter no máximo 500 caracteres")
        String biografia
) {
}
