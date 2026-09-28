package br.com.certifiquese.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;

import br.com.certifiquese.dto.CertificadoRequestDTO;
import br.com.certifiquese.dto.CertificadoResponseDTO;
import br.com.certifiquese.dto.CertificadoUpdateDTO;
import br.com.certifiquese.service.CertificadoService;
import br.com.certifiquese.service.storage.CertificadoStorage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

class CertificadoControllerTest {

    private CertificadoServiceStub certificadoService;
    private CertificadoController controller;

    @BeforeEach
    void setUp() {
        certificadoService = new CertificadoServiceStub();
        controller = new CertificadoController(certificadoService, new StorageStub());
    }

    @Test
    void deveRetornar201AoCadastrarCertificado() {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "HS256")
                .claim("usuarioId", 7L)
                .build();

        var resposta = controller.cadastrar(jwt, new CertificadoRequestDTO("foto", "Nome", "Empresa", LocalDate.of(2024, 1, 1), List.of("tag-a"), 40, "Desc", "http://link", true));

        assertThat(resposta.getStatusCode().value()).isEqualTo(201);
        assertThat(certificadoService.cadastro.nome()).isEqualTo("Nome");
    }

    @Test
    void deveRetornar200AoAtualizarCertificado() {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "HS256")
                .claim("usuarioId", 7L)
                .build();

        var resposta = controller.atualizar(jwt, "hash-antigo", new CertificadoUpdateDTO("foto2", "Nome 2", "Empresa 2", LocalDate.of(2024, 2, 2), List.of("tag-b"), 40, "Desc", "http://link", true));

        assertThat(resposta.getStatusCode().value()).isEqualTo(200);
        assertThat(certificadoService.atualizacao.nome()).isEqualTo("Nome 2");
    }

    private static final class CertificadoServiceStub extends CertificadoService {

        private CertificadoRequestDTO cadastro;
        private CertificadoUpdateDTO atualizacao;

        private CertificadoServiceStub() {
            super(null, null, null);
        }

        @Override
        public CertificadoResponseDTO cadastrar(Long idUsuario, CertificadoRequestDTO dto) {
            cadastro = dto;
            return new CertificadoResponseDTO(1L, "hash", dto.foto(), dto.nome(), dto.empresa(), dto.dataConclusao(), dto.tags(), dto.cargaHoraria(), dto.descricao(), dto.linkValidacao(), dto.publico());
        }

        @Override
        public CertificadoResponseDTO atualizar(Long idUsuario, String hashCertificado, CertificadoUpdateDTO dto) {
            atualizacao = dto;
            return new CertificadoResponseDTO(1L, "hash-novo", dto.foto(), dto.nome(), dto.empresa(), dto.dataConclusao(), dto.tags(), dto.cargaHoraria(), dto.descricao(), dto.linkValidacao(), dto.publico());
        }
    }

    private static final class StorageStub implements CertificadoStorage {
        @Override
        public ImagemArmazenada armazenar(org.springframework.web.multipart.MultipartFile arquivo) {
            return new ImagemArmazenada("imagem.png", "/certificados/imagens/imagem.png");
        }

        @Override
        public ArquivoArmazenado buscar(String chave) {
            return new ArquivoArmazenado(new byte[0], "image/png");
        }

        @Override
        public void remover(String referencia) {
        }
    }
}
