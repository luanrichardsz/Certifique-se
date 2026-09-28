package br.com.certifiquese.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import br.com.certifiquese.dto.UsuarioUpdateDTO;
import br.com.certifiquese.exception.RecursoEmConflitoException;
import br.com.certifiquese.exception.RecursoNaoEncontradoException;
import br.com.certifiquese.exception.SenhaIncorretaException;
import br.com.certifiquese.model.CertificadoEntity;
import br.com.certifiquese.model.Role;
import br.com.certifiquese.model.UsuarioEntity;
import br.com.certifiquese.repository.CertificadoRepository;
import br.com.certifiquese.repository.UsuarioRepository;
import br.com.certifiquese.service.storage.CertificadoStorage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;

class UsuarioServiceTest {

    private final Map<Long, UsuarioEntity> usuarios = new HashMap<>();
    private final Map<Long, List<CertificadoEntity>> certificadosPorUsuario = new HashMap<>();
    private final List<UsuarioEntity> usuariosRemovidos = new ArrayList<>();
    private final List<List<CertificadoEntity>> certificadosRemovidos = new ArrayList<>();
    private final List<String> fotosRemovidas = new ArrayList<>();

    private UsuarioService usuarioService;
    private boolean senhaValida;

    @BeforeEach
    void setUp() {
        usuarios.clear();
        certificadosPorUsuario.clear();
        usuariosRemovidos.clear();
        certificadosRemovidos.clear();
        fotosRemovidas.clear();
        senhaValida = false;

        UsuarioRepository usuarioRepository = criarUsuarioRepository();
        CertificadoRepository certificadoRepository = criarCertificadoRepository();
        PasswordEncoder passwordEncoder = new PasswordEncoder() {
            @Override
            public String encode(CharSequence rawPassword) {
                return rawPassword.toString();
            }

            @Override
            public boolean matches(CharSequence rawPassword, String encodedPassword) {
                return senhaValida;
            }
        };
        CertificadoStorage certificadoStorage = new CertificadoStorage() {
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
                fotosRemovidas.add(referencia);
            }
        };

        usuarioService = new UsuarioService(usuarioRepository, certificadoRepository, passwordEncoder, certificadoStorage);
    }

    @Test
    void deveExcluirContaDoUsuarioAutenticadoComSenhaValida() {
        UsuarioEntity usuario = novoUsuario(1L, "hash-senha");
        CertificadoEntity certificado = novoCertificado("https://supabase.co/storage/v1/object/public/certificados/arquivo.png", usuario);

        usuarios.put(1L, usuario);
        certificadosPorUsuario.put(1L, List.of(certificado));
        senhaValida = true;

        usuarioService.excluirConta(jwtComUsuarioId(1L), "senha-atual");

        assertThat(usuariosRemovidos).containsExactly(usuario);
        assertThat(certificadosRemovidos).hasSize(1);
        assertThat(certificadosRemovidos.get(0)).containsExactly(certificado);
        assertThat(fotosRemovidas).containsExactly("https://supabase.co/storage/v1/object/public/certificados/arquivo.png");
    }

    @Test
    void deveBloquearExclusaoQuandoSenhaEstiverIncorreta() {
        UsuarioEntity usuario = novoUsuario(1L, "hash-senha");
        usuarios.put(1L, usuario);

        assertThatThrownBy(() -> usuarioService.excluirConta(jwtComUsuarioId(1L), "senha-errada"))
                .isInstanceOf(SenhaIncorretaException.class);

        assertThat(usuariosRemovidos).isEmpty();
        assertThat(certificadosRemovidos).isEmpty();
        assertThat(fotosRemovidas).isEmpty();
    }

    @Test
    void deveFalharQuandoUsuarioNaoExistir() {
        assertThatThrownBy(() -> usuarioService.excluirConta(jwtComUsuarioId(99L), "senha-atual"))
                .isInstanceOf(RecursoNaoEncontradoException.class);

        assertThat(usuariosRemovidos).isEmpty();
        assertThat(certificadosRemovidos).isEmpty();
        assertThat(fotosRemovidas).isEmpty();
    }

    @Test
    void deveAtualizarUsuarioLogado() {
        UsuarioEntity usuario = novoUsuario(1L, "hash-senha");
        usuarios.put(1L, usuario);

        var resposta = usuarioService.atualizar(jwtComUsuarioId(1L), new UsuarioUpdateDTO("Novo Nome", "novo@exemplo.com", "novonome", "Headline", "Nova bio", true));

        assertThat(resposta.nomeUsuario()).isEqualTo("Novo Nome");
        assertThat(resposta.email()).isEqualTo("novo@exemplo.com");
        assertThat(usuarios.get(1L).getEmail()).isEqualTo("novo@exemplo.com");
    }

    @Test
    void deveBloquearAtualizacaoQuandoEmailJaEstiverEmUsoPorOutroUsuario() {
        UsuarioEntity usuario = novoUsuario(1L, "hash-senha");
        UsuarioEntity outroUsuario = novoUsuario(2L, "outro-hash");
        usuario.setEmail("primeiro@exemplo.com");
        outroUsuario.setEmail("novo@exemplo.com");
        usuarios.put(1L, usuario);
        usuarios.put(2L, outroUsuario);

        assertThatThrownBy(() -> usuarioService.atualizar(jwtComUsuarioId(1L), new UsuarioUpdateDTO("Novo Nome", "novo@exemplo.com", "novonome", "Headline", "Nova bio", true)))
                .isInstanceOf(RecursoEmConflitoException.class);
    }

    private UsuarioEntity novoUsuario(Long idUsuario, String senhaHash) {
        UsuarioEntity usuario = new UsuarioEntity();
        usuario.setIdUsuario(idUsuario);
        usuario.setNomeUsuario("Usuário Teste");
        usuario.setUsername("usuario" + idUsuario);
        usuario.setEmail("teste@exemplo.com");
        usuario.setSenha(senhaHash);
        usuario.setHeadline("Headline");
        usuario.setPerfilPublico(true);
        usuario.setRole(Role.USER);
        usuario.setCriadoEm(LocalDateTime.now());
        return usuario;
    }

    private CertificadoEntity novoCertificado(String foto, UsuarioEntity usuario) {
        CertificadoEntity certificado = new CertificadoEntity();
        certificado.setIdCertificado(10L);
        certificado.setFoto(foto);
        certificado.setUsuario(usuario);
        return certificado;
    }

    private Jwt jwtComUsuarioId(Long usuarioId) {
        return Jwt.withTokenValue("token")
                .header("alg", "HS256")
                .claim("usuarioId", usuarioId)
                .claim("roles", List.of("ROLE_USER"))
                .build();
    }

    private UsuarioRepository criarUsuarioRepository() {
        InvocationHandler handler = (proxy, method, args) -> switch (method.getName()) {
            case "findById" -> Optional.ofNullable(usuarios.get((Long) args[0]));
            case "save" -> {
                UsuarioEntity usuario = (UsuarioEntity) args[0];
                usuarios.put(usuario.getIdUsuario(), usuario);
                yield usuario;
            }
            case "delete" -> {
                usuariosRemovidos.add((UsuarioEntity) args[0]);
                yield null;
            }
            case "flush" -> null;
            case "existsByEmail" -> false;
            case "existsByEmailAndIdUsuarioNot" -> usuarios.values().stream()
                    .anyMatch(usuario -> usuario.getEmail().equals(args[0]) && !usuario.getIdUsuario().equals(args[1]));
            case "existsByUsernameAndIdUsuarioNot" -> false;
            case "existsByUsername" -> false;
            case "findAll" -> new ArrayList<>(usuarios.values());
            case "findByEmail" -> Optional.empty();
            default -> valorPadrao(method.getReturnType());
        };

        return (UsuarioRepository) Proxy.newProxyInstance(
                UsuarioRepository.class.getClassLoader(),
                new Class<?>[] { UsuarioRepository.class },
                handler);
    }

    private CertificadoRepository criarCertificadoRepository() {
        InvocationHandler handler = (proxy, method, args) -> switch (method.getName()) {
            case "findByUsuarioIdUsuario" -> certificadosPorUsuario.getOrDefault((Long) args[0], List.of());
            case "deleteAll" -> {
                List<CertificadoEntity> removidos = new ArrayList<>();
                for (Object objeto : (Iterable<?>) args[0]) {
                    CertificadoEntity certificado = (CertificadoEntity) objeto;
                    removidos.add(certificado);
                }
                certificadosRemovidos.add(removidos);
                yield null;
            }
            case "flush" -> null;
            case "existsByHashCertificado" -> false;
            case "findByHashCertificado" -> Optional.empty();
            case "findAll" -> List.of();
            default -> valorPadrao(method.getReturnType());
        };

        return (CertificadoRepository) Proxy.newProxyInstance(
                CertificadoRepository.class.getClassLoader(),
                new Class<?>[] { CertificadoRepository.class },
                handler);
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
