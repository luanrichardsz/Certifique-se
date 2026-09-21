package br.com.certifiquese.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

import br.com.certifiquese.dto.EsqueciSenhaRequestDTO;
import br.com.certifiquese.dto.RedefinirSenhaRequestDTO;
import br.com.certifiquese.dto.RecuperacaoSenhaResponseDTO;
import br.com.certifiquese.exception.RecuperacaoSenhaInvalidaException;
import br.com.certifiquese.model.RecuperacaoSenhaEntity;
import br.com.certifiquese.model.Role;
import br.com.certifiquese.model.UsuarioEntity;
import br.com.certifiquese.repository.RecuperacaoSenhaRepository;
import br.com.certifiquese.repository.UsuarioRepository;
import br.com.certifiquese.service.email.EmailSender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

class RecuperacaoSenhaServiceTest {

    private final Map<Long, UsuarioEntity> usuariosPorId = new HashMap<>();
    private final Map<String, UsuarioEntity> usuariosPorEmail = new HashMap<>();
    private final Map<Long, RecuperacaoSenhaEntity> tokensPorId = new HashMap<>();
    private final Map<String, RecuperacaoSenhaEntity> tokensPorHash = new HashMap<>();
    private final List<MensagemEnviada> emailsEnviados = new ArrayList<>();

    private RecuperacaoSenhaService service;
    private long proximoUsuarioId;
    private long proximoTokenId;

    @BeforeEach
    void setUp() {
        usuariosPorId.clear();
        usuariosPorEmail.clear();
        tokensPorId.clear();
        tokensPorHash.clear();
        emailsEnviados.clear();
        proximoUsuarioId = 1L;
        proximoTokenId = 1L;

        UsuarioRepository usuarioRepository = criarUsuarioRepository();
        RecuperacaoSenhaRepository recuperacaoSenhaRepository = criarRecuperacaoSenhaRepository();
        PasswordEncoder passwordEncoder = new PasswordEncoder() {
            @Override
            public String encode(CharSequence rawPassword) {
                return "hash-" + rawPassword;
            }

            @Override
            public boolean matches(CharSequence rawPassword, String encodedPassword) {
                return encodedPassword.equals(encode(rawPassword));
            }
        };
        EmailSender emailSender = (destinatario, assunto, corpo) -> emailsEnviados.add(new MensagemEnviada(destinatario, assunto, corpo));

        service = new RecuperacaoSenhaService(usuarioRepository, recuperacaoSenhaRepository, passwordEncoder, emailSender, "http://frontend.local");
    }

    @Test
    void deveGerarTokenESalvarSomenteHash() {
        UsuarioEntity usuario = novoUsuario("alice@exemplo.com");
        salvarUsuario(usuario);

        RecuperacaoSenhaResponseDTO resposta = service.solicitarRecuperacaoSenha(new EsqueciSenhaRequestDTO("alice@exemplo.com"));

        assertThat(resposta.mensagem()).isEqualTo("Se o e-mail estiver cadastrado, você receberá as instruções para redefinir a senha.");
        assertThat(emailsEnviados).hasSize(1);

        String tokenRaw = extrairTokenDaMensagem(emailsEnviados.get(0).corpo());
        RecuperacaoSenhaEntity tokenSalvo = tokensPorId.values().iterator().next();

        assertThat(tokenSalvo.getHashToken()).hasSize(64);
        assertThat(tokenSalvo.getHashToken()).isEqualTo(hashSha256Hex(tokenRaw));
        assertThat(tokenSalvo.getExpiraEm()).isAfter(tokenSalvo.getCriadoEm());
        assertThat(tokenSalvo.getUtilizadoEm()).isNull();
        assertThat(emailsEnviados.get(0).corpo()).contains("http://frontend.local/redefinir-senha?token=");
    }

    @Test
    void deveRetornarMensagemGenericaQuandoEmailNaoExistir() {
        RecuperacaoSenhaResponseDTO resposta = service.solicitarRecuperacaoSenha(new EsqueciSenhaRequestDTO("inexistente@exemplo.com"));

        assertThat(resposta.mensagem()).isEqualTo("Se o e-mail estiver cadastrado, você receberá as instruções para redefinir a senha.");
        assertThat(emailsEnviados).isEmpty();
        assertThat(tokensPorId).isEmpty();
    }

    @Test
    void deveRedefinirSenhaEInvalidarOutrosTokensDoUsuario() {
        UsuarioEntity usuario = novoUsuario("bob@exemplo.com");
        salvarUsuario(usuario);

        service.solicitarRecuperacaoSenha(new EsqueciSenhaRequestDTO("bob@exemplo.com"));
        String tokenValido = extrairTokenDaMensagem(emailsEnviados.get(0).corpo());

        RecuperacaoSenhaEntity tokenAntigo = salvarToken(usuario, "token-antigo", null, LocalDateTime.now().minusMinutes(2), LocalDateTime.now().plusMinutes(20));

        RecuperacaoSenhaResponseDTO resposta = service.redefinirSenha(new RedefinirSenhaRequestDTO(tokenValido, "NovaSenha123", "NovaSenha123"));

        assertThat(resposta.mensagem()).isEqualTo("Senha redefinida com sucesso.");
        assertThat(usuario.getSenha()).isEqualTo("hash-NovaSenha123");
        assertThat(usuario.getTokenVersion()).isEqualTo(1);
        assertThat(tokensPorId.values()).allSatisfy(token -> assertThat(token.getUtilizadoEm()).isNotNull());
        assertThat(tokenAntigo.getUtilizadoEm()).isNotNull();
    }

    @Test
    void deveRejeitarConfirmacaoDiferente() {
        UsuarioEntity usuario = novoUsuario("carol@exemplo.com");
        salvarUsuario(usuario);

        service.solicitarRecuperacaoSenha(new EsqueciSenhaRequestDTO("carol@exemplo.com"));
        String tokenValido = extrairTokenDaMensagem(emailsEnviados.get(0).corpo());

        assertThatThrownBy(() -> service.redefinirSenha(new RedefinirSenhaRequestDTO(tokenValido, "NovaSenha123", "OutraSenha123")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("A confirmação da nova senha não confere.");

        assertThat(usuario.getSenha()).isEqualTo("hash-senha-antiga");
        assertThat(usuario.getTokenVersion()).isEqualTo(0);
    }

    @Test
    void deveRejeitarTokenExpiradoOuUtilizado() {
        UsuarioEntity usuario = novoUsuario("dana@exemplo.com");
        salvarUsuario(usuario);

        RecuperacaoSenhaEntity token = salvarToken(usuario, "token-expirado", null, LocalDateTime.now().minusMinutes(40), LocalDateTime.now().minusMinutes(10));

        assertThatThrownBy(() -> service.redefinirSenha(new RedefinirSenhaRequestDTO("token-expirado", "NovaSenha123", "NovaSenha123")))
                .isInstanceOf(RecuperacaoSenhaInvalidaException.class)
                .hasMessage("Token de recuperação inválido, expirado ou já utilizado.");

        token.setUtilizadoEm(LocalDateTime.now());

        assertThatThrownBy(() -> service.redefinirSenha(new RedefinirSenhaRequestDTO("token-expirado", "NovaSenha123", "NovaSenha123")))
                .isInstanceOf(RecuperacaoSenhaInvalidaException.class);
    }

    private UsuarioEntity novoUsuario(String email) {
        UsuarioEntity usuario = new UsuarioEntity();
        usuario.setIdUsuario(proximoUsuarioId++);
        usuario.setNomeUsuario("Usuário Teste");
        usuario.setEmail(email);
        usuario.setSenha("hash-senha-antiga");
        usuario.setRole(Role.USER);
        usuario.setTokenVersion(0);
        usuario.setCriadoEm(LocalDateTime.now());
        return usuario;
    }

    private void salvarUsuario(UsuarioEntity usuario) {
        usuariosPorId.put(usuario.getIdUsuario(), usuario);
        usuariosPorEmail.put(usuario.getEmail(), usuario);
    }

    private RecuperacaoSenhaEntity salvarToken(UsuarioEntity usuario,
                                              String tokenRaw,
                                              LocalDateTime utilizadoEm,
                                              LocalDateTime criadoEm,
                                              LocalDateTime expiraEm) {
        RecuperacaoSenhaEntity token = new RecuperacaoSenhaEntity();
        token.setIdRecuperacaoSenha(proximoTokenId++);
        token.setUsuario(usuario);
        token.setHashToken(hashSha256Hex(tokenRaw));
        token.setCriadoEm(criadoEm);
        token.setExpiraEm(expiraEm);
        token.setUtilizadoEm(utilizadoEm);
        tokensPorId.put(token.getIdRecuperacaoSenha(), token);
        tokensPorHash.put(token.getHashToken(), token);
        return token;
    }

    private UsuarioRepository criarUsuarioRepository() {
        InvocationHandler handler = (proxy, method, args) -> switch (method.getName()) {
            case "save" -> {
                UsuarioEntity usuario = (UsuarioEntity) args[0];
                if (usuario.getIdUsuario() == null) {
                    usuario.setIdUsuario(proximoUsuarioId++);
                }
                salvarUsuario(usuario);
                yield usuario;
            }
            case "findById" -> Optional.ofNullable(usuariosPorId.get((Long) args[0]));
            case "findByEmail" -> Optional.ofNullable(usuariosPorEmail.get((String) args[0]));
            case "existsByEmail" -> usuariosPorEmail.containsKey((String) args[0]);
            case "findAll" -> new ArrayList<>(usuariosPorId.values());
            case "delete", "flush" -> null;
            default -> valorPadrao(method.getReturnType());
        };

        return (UsuarioRepository) Proxy.newProxyInstance(
                UsuarioRepository.class.getClassLoader(),
                new Class<?>[] { UsuarioRepository.class },
                handler);
    }

    private RecuperacaoSenhaRepository criarRecuperacaoSenhaRepository() {
        InvocationHandler handler = (proxy, method, args) -> switch (method.getName()) {
            case "save" -> {
                RecuperacaoSenhaEntity token = (RecuperacaoSenhaEntity) args[0];
                if (token.getIdRecuperacaoSenha() == null) {
                    token.setIdRecuperacaoSenha(proximoTokenId++);
                }
                tokensPorId.put(token.getIdRecuperacaoSenha(), token);
                tokensPorHash.put(token.getHashToken(), token);
                yield token;
            }
            case "saveAll" -> {
                List<RecuperacaoSenhaEntity> tokens = new ArrayList<>();
                for (Object objeto : (Iterable<?>) args[0]) {
                    RecuperacaoSenhaEntity token = (RecuperacaoSenhaEntity) objeto;
                    tokensPorId.put(token.getIdRecuperacaoSenha(), token);
                    tokensPorHash.put(token.getHashToken(), token);
                    tokens.add(token);
                }
                yield tokens;
            }
            case "findByHashToken" -> Optional.ofNullable(tokensPorHash.get((String) args[0]));
            case "findByUsuarioIdUsuarioAndUtilizadoEmIsNull" -> tokensPorId.values().stream()
                    .filter(token -> token.getUsuario() != null)
                    .filter(token -> token.getUsuario().getIdUsuario().equals((Long) args[0]))
                    .filter(token -> token.getUtilizadoEm() == null)
                    .toList();
            case "findAll" -> new ArrayList<>(tokensPorId.values());
            case "delete", "flush" -> null;
            default -> valorPadrao(method.getReturnType());
        };

        return (RecuperacaoSenhaRepository) Proxy.newProxyInstance(
                RecuperacaoSenhaRepository.class.getClassLoader(),
                new Class<?>[] { RecuperacaoSenhaRepository.class },
                handler);
    }

    private String extrairTokenDaMensagem(String corpo) {
        var matcher = Pattern.compile("token=([^\\s]+)").matcher(corpo);
        assertThat(matcher.find()).isTrue();
        return matcher.group(1);
    }

    private String hashSha256Hex(String valor) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return java.util.HexFormat.of().formatHex(digest.digest(valor.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException(ex);
        }
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

    private record MensagemEnviada(String destinatario, String assunto, String corpo) {
    }
}