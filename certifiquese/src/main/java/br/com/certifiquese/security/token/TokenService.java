package br.com.certifiquese.security.token;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.stereotype.Service;

import br.com.certifiquese.security.authentication.UsuarioAutenticado;

@Service
public class TokenService {

    private final JwtEncoder jwtEncoder;
    private final String issuer;
    private final long expirationMinutes;

    public TokenService(JwtEncoder jwtEncoder, @Value("${jwt.issuer}") String issuer, @Value("${jwt.expiration-minutes}") long expirationMinutes) {
        this.jwtEncoder = jwtEncoder;
        this.issuer = issuer;
        this.expirationMinutes = expirationMinutes;
    }

    public String gerarToken(UsuarioAutenticado usuarioAutenticado) {

        Instant agora = Instant.now();

        Instant expiracao = agora.plus(Duration.ofMinutes(expirationMinutes));

        List<String> roles = usuarioAutenticado.getAuthorities().stream().map(authority -> authority.getAuthority()).toList();

        JwtClaimsSet claims = JwtClaimsSet.builder()
                                          .issuer(issuer)
                                          .issuedAt(agora)
                                          .expiresAt(expiracao)
                                          .subject(usuarioAutenticado.getUsername())
                                          .claim("usuarioId", usuarioAutenticado.getId())
                                          .claim("roles", roles)
                                          .build();

        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).type("JWT").build();

        JwtEncoderParameters parametros = JwtEncoderParameters.from(header, claims);

        return jwtEncoder.encode(parametros).getTokenValue();
    }

    public long getExpirationSeconds() {
        return Duration.ofMinutes(expirationMinutes).toSeconds();
    }
}