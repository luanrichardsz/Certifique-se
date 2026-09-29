package br.com.certifiquese.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import br.com.certifiquese.dto.CertificadoRequestDTO;
import br.com.certifiquese.dto.CertificadoUpdateDTO;
import br.com.certifiquese.exception.LimitePlanoExcedidoException;
import br.com.certifiquese.exception.OperacaoNaoPermitidaException;
import br.com.certifiquese.model.CertificadoEntity;
import br.com.certifiquese.model.Role;
import br.com.certifiquese.model.UsuarioEntity;
import br.com.certifiquese.repository.CertificadoRepository;
import br.com.certifiquese.repository.UsuarioRepository;
import br.com.certifiquese.service.storage.CertificadoStorage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CertificadoServiceTest {

    private final Map<String, CertificadoEntity> certificadosPorHash = new HashMap<>();
    private final Map<Long, UsuarioEntity> usuariosPorId = new HashMap<>();

    private CertificadoService certificadoService;

    @BeforeEach
    void setUp() {
        certificadosPorHash.clear();
        usuariosPorId.clear();

        certificadosPorHash.put("hash-antigo", novoCertificado(10L, "hash-antigo", 1L));
        usuariosPorId.put(1L, novoUsuario(1L));
        usuariosPorId.put(2L, novoUsuario(2L));

        certificadoService = new CertificadoService(criarCertificadoRepository(), criarUsuarioRepository(), criarStorage());
    }

    @Test
    void deveAtualizarCertificadoDoUsuario() {
        var resposta = certificadoService.atualizar(1L, "hash-antigo", new CertificadoUpdateDTO("foto-nova", "Nome novo", "Empresa nova", LocalDate.of(2024, 2, 2), List.of("tag-b", "tag-a"), 40, "Descricao", "http://link", true));

        assertThat(resposta.nome()).isEqualTo("Nome novo");
        assertThat(resposta.hashCertificado()).isNotEqualTo("hash-antigo");
        assertThat(certificadosPorHash.values()).anyMatch(certificado -> "Nome novo".equals(certificado.getNome()));
    }

    @Test
    void deveBloquearEdicaoQuandoCertificadoNaoPertencerAoUsuario() {
        assertThatThrownBy(() -> certificadoService.atualizar(2L, "hash-antigo", new CertificadoUpdateDTO("foto-nova", "Nome novo", "Empresa nova", LocalDate.of(2024, 2, 2), List.of("tag-b"), 40, "Descricao", "http://link", true)))
                .isInstanceOf(OperacaoNaoPermitidaException.class);
    }

    @Test
    void deveBloquearCadastroQuandoAtingirLimiteTotal15Certificados() {
        certificadosPorHash.clear();
        for (int i = 1; i <= 15; i++) {
            certificadosPorHash.put("hash-" + i, novoCertificado((long) i, "hash-" + i, 1L));
        }

        CertificadoRequestDTO dto = new CertificadoRequestDTO("foto", "Certificado 16", "Empresa", LocalDate.of(2024, 1, 1), List.of("tag"), 10, "Desc", "http://link", false);

        assertThatThrownBy(() -> certificadoService.cadastrar(1L, dto))
                .isInstanceOf(LimitePlanoExcedidoException.class)
                .hasMessageContaining("limite de 15 certificados do plano gratuito");
    }

    @Test
    void deveBloquearCadastroQuandoAtingirLimite8CertificadosPublicos() {
        certificadosPorHash.clear();
        for (int i = 1; i <= 8; i++) {
            CertificadoEntity cert = novoCertificado((long) i, "hash-" + i, 1L);
            cert.setPublico(true);
            certificadosPorHash.put("hash-" + i, cert);
        }

        CertificadoRequestDTO dto = new CertificadoRequestDTO("foto", "Certificado 9", "Empresa", LocalDate.of(2024, 1, 1), List.of("tag"), 10, "Desc", "http://link", true);

        assertThatThrownBy(() -> certificadoService.cadastrar(1L, dto))
                .isInstanceOf(LimitePlanoExcedidoException.class)
                .hasMessageContaining("limite de 8 certificados públicos no plano gratuito");
    }

    @Test
    void devePermitirCadastrarPrivadoQuandoAtingir8PublicosMasMenosDe15Totais() {
        certificadosPorHash.clear();
        for (int i = 1; i <= 8; i++) {
            CertificadoEntity cert = novoCertificado((long) i, "hash-" + i, 1L);
            cert.setPublico(true);
            certificadosPorHash.put("hash-" + i, cert);
        }

        CertificadoRequestDTO dto = new CertificadoRequestDTO("foto-privada", "Certificado Privado", "Empresa", LocalDate.of(2024, 1, 1), List.of("tag"), 10, "Desc", "http://link", false);

        var resposta = certificadoService.cadastrar(1L, dto);
        assertThat(resposta.nome()).isEqualTo("Certificado Privado");
        assertThat(resposta.publico()).isFalse();
    }

    @Test
    void deveBloquearAtualizacaoParaPublicoQuandoJaPossuir8Publicos() {
        certificadosPorHash.clear();
        for (int i = 1; i <= 8; i++) {
            CertificadoEntity cert = novoCertificado((long) i, "hash-" + i, 1L);
            cert.setPublico(true);
            certificadosPorHash.put("hash-" + i, cert);
        }

        CertificadoEntity certPrivado = novoCertificado(9L, "hash-privado", 1L);
        certPrivado.setPublico(false);
        certificadosPorHash.put("hash-privado", certPrivado);

        CertificadoUpdateDTO dto = new CertificadoUpdateDTO("foto-nova", "Certificado Tornando Publico", "Empresa", LocalDate.of(2024, 1, 1), List.of("tag"), 10, "Desc", "http://link", true);

        assertThatThrownBy(() -> certificadoService.atualizar(1L, "hash-privado", dto))
                .isInstanceOf(LimitePlanoExcedidoException.class)
                .hasMessageContaining("limite de 8 certificados públicos no plano gratuito");
    }

    @Test
    void devePermitirAtualizarCertificadoJaPublicoMesmoCom8Publicos() {
        certificadosPorHash.clear();
        for (int i = 1; i <= 8; i++) {
            CertificadoEntity cert = novoCertificado((long) i, "hash-" + i, 1L);
            cert.setPublico(true);
            certificadosPorHash.put("hash-" + i, cert);
        }

        CertificadoUpdateDTO dto = new CertificadoUpdateDTO("foto-editada", "Nome Editado", "Empresa", LocalDate.of(2024, 1, 1), List.of("tag"), 10, "Desc", "http://link", true);

        var resposta = certificadoService.atualizar(1L, "hash-1", dto);
        assertThat(resposta.nome()).isEqualTo("Nome Editado");
        assertThat(resposta.publico()).isTrue();
    }

    @Test
    void adminNaoDeveTerLimiteDeCertificados() {
        UsuarioEntity admin = novoUsuario(99L);
        admin.setRole(Role.ADMIN);
        usuariosPorId.put(99L, admin);

        certificadosPorHash.clear();
        for (int i = 1; i <= 20; i++) {
            CertificadoEntity cert = novoCertificado((long) i, "hash-" + i, 99L);
            cert.setPublico(true);
            certificadosPorHash.put("hash-" + i, cert);
        }

        CertificadoRequestDTO dto = new CertificadoRequestDTO("foto-admin", "Certificado Admin", "Empresa", LocalDate.of(2024, 1, 1), List.of("tag"), 10, "Desc", "http://link", true);

        var resposta = certificadoService.cadastrar(99L, dto);
        assertThat(resposta.nome()).isEqualTo("Certificado Admin");
    }

    private UsuarioEntity novoUsuario(Long idUsuario) {
        UsuarioEntity usuario = new UsuarioEntity();
        usuario.setIdUsuario(idUsuario);
        usuario.setNomeUsuario("Usuário " + idUsuario);
        usuario.setUsername("usuario" + idUsuario);
        usuario.setEmail("usuario" + idUsuario + "@exemplo.com");
        usuario.setPerfilPublico(true);
        return usuario;
    }

    private CertificadoEntity novoCertificado(Long idCertificado, String hashCertificado, Long idUsuario) {
        CertificadoEntity certificado = new CertificadoEntity();
        certificado.setIdCertificado(idCertificado);
        certificado.setHashCertificado(hashCertificado);
        certificado.setFoto("foto-antiga");
        certificado.setNome("Nome antigo");
        certificado.setEmpresa("Empresa antiga");
        certificado.setDataConclusao(LocalDate.of(2024, 1, 1));
        certificado.setTags(new ArrayList<>(List.of("tag-a")));
        certificado.setCargaHoraria(20);
        certificado.setDescricao("Desc");
        certificado.setLinkValidacao("http://link");
        certificado.setPublico(true);
        certificado.setUsuario(usuariosPorId.computeIfAbsent(idUsuario, this::novoUsuario));
        return certificado;
    }

    private CertificadoRepository criarCertificadoRepository() {
        InvocationHandler handler = (proxy, method, args) -> switch (method.getName()) {
            case "findByHashCertificado" -> Optional.ofNullable(certificadosPorHash.get((String) args[0]));
            case "existsByHashCertificado" -> certificadosPorHash.containsKey((String) args[0]);
            case "save" -> {
                CertificadoEntity certificado = (CertificadoEntity) args[0];
                certificadosPorHash.values().removeIf(item -> item.getIdCertificado().equals(certificado.getIdCertificado()));
                certificadosPorHash.put(certificado.getHashCertificado(), certificado);
                yield certificado;
            }
            case "countByUsuarioIdUsuario" -> {
                Long id = (Long) args[0];
                yield certificadosPorHash.values().stream()
                        .filter(c -> c.getUsuario() != null && id.equals(c.getUsuario().getIdUsuario()))
                        .count();
            }
            case "countByUsuarioIdUsuarioAndPublicoTrue" -> {
                Long id = (Long) args[0];
                yield certificadosPorHash.values().stream()
                        .filter(c -> c.getUsuario() != null && id.equals(c.getUsuario().getIdUsuario()) && Boolean.TRUE.equals(c.getPublico()))
                        .count();
            }
            case "findAll" -> new ArrayList<>(certificadosPorHash.values());
            default -> valorPadrao(method.getReturnType());
        };

        return (CertificadoRepository) Proxy.newProxyInstance(
                CertificadoRepository.class.getClassLoader(),
                new Class<?>[] { CertificadoRepository.class },
                handler);
    }

    private UsuarioRepository criarUsuarioRepository() {
        InvocationHandler handler = (proxy, method, args) -> switch (method.getName()) {
            case "findById" -> Optional.ofNullable(usuariosPorId.get((Long) args[0]));
            default -> valorPadrao(method.getReturnType());
        };

        return (UsuarioRepository) Proxy.newProxyInstance(
                UsuarioRepository.class.getClassLoader(),
                new Class<?>[] { UsuarioRepository.class },
                handler);
    }

    private CertificadoStorage criarStorage() {
        return new CertificadoStorage() {
            @Override
            public ImagemArmazenada armazenar(org.springframework.web.multipart.MultipartFile arquivo) {
                throw new UnsupportedOperationException();
            }

            @Override
            public ArquivoArmazenado buscar(String chave) {
                throw new UnsupportedOperationException();
            }

            @Override
            public void remover(String referencia) {
            }
        };
    }

    private Object valorPadrao(Class<?> type) {
        if (type.equals(boolean.class)) {
            return false;
        }
        if (type.equals(int.class) || type.equals(short.class) || type.equals(byte.class) || type.equals(long.class)) {
            return 0;
        }
        if (type.equals(double.class) || type.equals(float.class)) {
            return 0.0;
        }
        return null;
    }
}
