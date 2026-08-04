package br.com.certifiquese.security.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import br.com.certifiquese.model.Role;
import br.com.certifiquese.model.UsuarioEntity;
import br.com.certifiquese.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;

class SecurityConfigTest {

    @Test
    void deveAceitarJwtComTokenVersionCompativel() {
        UsuarioRepository usuarioRepository = criarUsuarioRepository(usuarioComVersao(0));
        JwtAuthenticationConverter converter = new SecurityConfig().jwtAuthenticationConverter(usuarioRepository);

        Authentication authentication = converter.convert(jwtComVersao(0));

        assertThat(authentication).isNotNull();
        assertThat(authentication.getAuthorities())
                .extracting("authority")
                .contains("ROLE_USER");
    }

    @Test
    void deveRejeitarJwtAntigoQuandoTokenVersionMudar() {
        UsuarioRepository usuarioRepository = criarUsuarioRepository(usuarioComVersao(1));
        JwtAuthenticationConverter converter = new SecurityConfig().jwtAuthenticationConverter(usuarioRepository);

        assertThatThrownBy(() -> converter.convert(jwtComVersao(0)))
                .isInstanceOf(JwtException.class);
    }

    private UsuarioRepository criarUsuarioRepository(UsuarioEntity usuario) {
        Map<Long, UsuarioEntity> usuarios = new HashMap<>();
        usuarios.put(usuario.getIdUsuario(), usuario);

        InvocationHandler handler = (proxy, method, args) -> switch (method.getName()) {
            case "findById" -> Optional.ofNullable(usuarios.get((Long) args[0]));
            default -> null;
        };

        return (UsuarioRepository) Proxy.newProxyInstance(
                UsuarioRepository.class.getClassLoader(),
                new Class<?>[] { UsuarioRepository.class },
                handler);
    }

    private UsuarioEntity usuarioComVersao(int tokenVersion) {
        UsuarioEntity usuario = new UsuarioEntity();
        usuario.setIdUsuario(1L);
        usuario.setNomeUsuario("Usuário Teste");
        usuario.setEmail("teste@exemplo.com");
        usuario.setSenha("hash-senha");
        usuario.setRole(Role.USER);
        usuario.setTokenVersion(tokenVersion);
        usuario.setCriadoEm(LocalDateTime.now());
        return usuario;
    }

    private Jwt jwtComVersao(int tokenVersion) {
        return Jwt.withTokenValue("token")
                .header("alg", "HS256")
                .claim("usuarioId", 1L)
                .claim("tokenVersion", tokenVersion)
                .claim("roles", java.util.List.of("ROLE_USER"))
                .build();
    }
}