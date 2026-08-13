package br.com.certifiquese.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import br.com.certifiquese.dto.EsqueciSenhaRequestDTO;
import br.com.certifiquese.dto.LoginRequestDTO;
import br.com.certifiquese.dto.LoginResponseDTO;
import br.com.certifiquese.dto.RedefinirSenhaRequestDTO;
import br.com.certifiquese.dto.RecuperacaoSenhaResponseDTO;
import br.com.certifiquese.service.AuthService;
import br.com.certifiquese.service.RecuperacaoSenhaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AuthControllerTest {

    private AuthServiceStub authService;
    private RecuperacaoSenhaServiceStub recuperacaoSenhaService;
    private AuthController controller;

    @BeforeEach
    void setUp() {
        authService = new AuthServiceStub();
        recuperacaoSenhaService = new RecuperacaoSenhaServiceStub();
        controller = new AuthController(authService, recuperacaoSenhaService);
    }

    @Test
    void deveDelegarSolicitacaoDeRecuperacaoDeSenha() {
        var resposta = controller.esqueciSenha(new EsqueciSenhaRequestDTO("teste@exemplo.com"));

        assertThat(resposta.getStatusCode().value()).isEqualTo(200);
        assertThat(recuperacaoSenhaService.esqueciChamadas).hasSize(1);
        assertThat(recuperacaoSenhaService.esqueciChamadas.get(0).email()).isEqualTo("teste@exemplo.com");
    }

    @Test
    void deveDelegarRedefinicaoDeSenha() {
        var resposta = controller.redefinirSenha(new RedefinirSenhaRequestDTO("token", "NovaSenha123", "NovaSenha123"));

        assertThat(resposta.getStatusCode().value()).isEqualTo(200);
        assertThat(recuperacaoSenhaService.redefinirChamadas).hasSize(1);
        assertThat(recuperacaoSenhaService.redefinirChamadas.get(0).token()).isEqualTo("token");
    }

    @Test
    void deveManterLoginFuncional() {
        var resposta = controller.login(new LoginRequestDTO("teste@exemplo.com", "senha"));

        assertThat(resposta.getStatusCode().value()).isEqualTo(200);
        assertThat(authService.chamadas).hasSize(1);
        assertThat(authService.chamadas.get(0).email()).isEqualTo("teste@exemplo.com");
    }

    private static final class AuthServiceStub extends AuthService {

        private final List<LoginRequestDTO> chamadas = new ArrayList<>();

        private AuthServiceStub() {
            super(null, null);
        }

        @Override
        public LoginResponseDTO autenticar(LoginRequestDTO dto) {
            chamadas.add(dto);
            return new LoginResponseDTO("token", "Bearer", 3600);
        }
    }

    private static final class RecuperacaoSenhaServiceStub extends RecuperacaoSenhaService {

        private final List<EsqueciSenhaRequestDTO> esqueciChamadas = new ArrayList<>();
        private final List<RedefinirSenhaRequestDTO> redefinirChamadas = new ArrayList<>();

        private RecuperacaoSenhaServiceStub() {
            super(null, null, null, null, "http://frontend.local");
        }

        @Override
        public RecuperacaoSenhaResponseDTO solicitarRecuperacaoSenha(EsqueciSenhaRequestDTO dto) {
            esqueciChamadas.add(dto);
            return new RecuperacaoSenhaResponseDTO("Se o e-mail estiver cadastrado, você receberá as instruções para redefinir a senha.");
        }

        @Override
        public RecuperacaoSenhaResponseDTO redefinirSenha(RedefinirSenhaRequestDTO dto) {
            redefinirChamadas.add(dto);
            return new RecuperacaoSenhaResponseDTO("Senha redefinida com sucesso.");
        }
    }
}