package br.com.certifiquese.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import br.com.certifiquese.dto.ExcluirContaRequestDTO;
import br.com.certifiquese.dto.UsuarioRequestDTO;
import br.com.certifiquese.dto.UsuarioResponseDTO;
import br.com.certifiquese.dto.UsuarioUpdateDTO;
import br.com.certifiquese.model.Role;
import br.com.certifiquese.service.UsuarioService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

class UsuarioControllerTest {

    private UsuarioServiceStub usuarioService;
    private UsuarioController controller;

    @BeforeEach
    void setUp() {
        usuarioService = new UsuarioServiceStub();
        controller = new UsuarioController(usuarioService);
    }

    @Test
    void deveRetornar201AoCadastrarUsuario() {
        var resposta = controller.cadastrar(new UsuarioRequestDTO("Usuário", "teste@exemplo.com", "senha123", "Bio"));

        assertThat(resposta.getStatusCode().value()).isEqualTo(201);
        assertThat(resposta.getBody()).isNotNull();
        assertThat(usuarioService.cadastro.nomeUsuario()).isEqualTo("Usuário");
    }

    @Test
    void deveRetornar200AoAtualizarUsuarioLogado() {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "HS256")
                .claim("usuarioId", 7L)
                .build();

        var resposta = controller.atualizar(jwt, new UsuarioUpdateDTO("Novo Nome", "novo@exemplo.com", "Bio nova"));

        assertThat(resposta.getStatusCode().value()).isEqualTo(200);
        assertThat(resposta.getBody()).isNotNull();
        assertThat(usuarioService.atualizacao.nomeUsuario()).isEqualTo("Novo Nome");
    }

    @Test
    void deveRetornar204AoExcluirContaDoUsuarioLogado() throws Exception {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "HS256")
                .claim("usuarioId", 7L)
                .build();

        var resposta = controller.excluirMinhaConta(jwt, new ExcluirContaRequestDTO("senha-atual"));

        assertThat(resposta.getStatusCode().value()).isEqualTo(204);

        assertThat(usuarioService.chamadas).hasSize(1);
        assertThat(usuarioService.chamadas.get(0).senhaAtual()).isEqualTo("senha-atual");
        assertThat((Long) usuarioService.chamadas.get(0).jwt().getClaim("usuarioId")).isEqualTo(7L);
    }

    private static final class UsuarioServiceStub extends UsuarioService {

        private final List<Chamada> chamadas = new ArrayList<>();
        private UsuarioRequestDTO cadastro;
        private UsuarioUpdateDTO atualizacao;

        private UsuarioServiceStub() {
            super(null, null, null, null);
        }

        @Override
        public UsuarioResponseDTO cadastrar(UsuarioRequestDTO dto) {
            cadastro = dto;
            return new UsuarioResponseDTO(1L, dto.nomeUsuario(), dto.email(), dto.biografia(), Role.USER, null);
        }

        @Override
        public UsuarioResponseDTO atualizar(Jwt jwt, UsuarioUpdateDTO dto) {
            atualizacao = dto;
            return new UsuarioResponseDTO(7L, dto.nomeUsuario(), dto.email(), dto.biografia(), Role.USER, null);
        }

        @Override
        public void excluirConta(Jwt jwt, String senhaAtual) {
            chamadas.add(new Chamada(jwt, senhaAtual));
        }
    }

    private record Chamada(Jwt jwt, String senhaAtual) {
    }
}