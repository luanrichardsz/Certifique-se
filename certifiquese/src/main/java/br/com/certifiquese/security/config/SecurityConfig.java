package br.com.certifiquese.security.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import java.util.List;


import jakarta.servlet.DispatcherType;

import br.com.certifiquese.model.UsuarioEntity;
import br.com.certifiquese.repository.UsuarioRepository;
import br.com.certifiquese.security.authentication.UsuarioDetailsService;
import br.com.certifiquese.security.ratelimit.RateLimitFilter;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import org.springframework.beans.factory.annotation.Value;
import java.util.ArrayList;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

	@Value("${app.frontend-url:}")
	private String frontendUrl;

	@Bean
	public SecurityFilterChain securityFilterChain(
			HttpSecurity http,
			DaoAuthenticationProvider authenticationProvider,
			UsuarioRepository usuarioRepository,
			RateLimitFilter rateLimitFilter) throws Exception {
		return http
				.cors(cors -> cors.configurationSource(corsConfigurationSource()))
				.csrf(csrf -> csrf.disable())
				.headers(headers -> headers
						.frameOptions(frame -> frame.sameOrigin())
						.contentTypeOptions(contentType -> {})
						.httpStrictTransportSecurity(hsts -> hsts.includeSubDomains(true).maxAgeInSeconds(31536000))
				)
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.addFilterBefore(rateLimitFilter, UsernamePasswordAuthenticationFilter.class)
				.authorizeHttpRequests(auth -> auth
						.dispatcherTypeMatchers(DispatcherType.ERROR)
						.permitAll()
						.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
						.requestMatchers(
								"/v3/api-docs/**",
								"/swagger-ui/**",
								"/swagger-ui.html")
						.permitAll()
							.requestMatchers(HttpMethod.GET, "/public/**")
							.permitAll()
							.requestMatchers(HttpMethod.HEAD, "/public/**")
							.permitAll()
							.requestMatchers(HttpMethod.GET, "/certificados/imagens/**")
							.permitAll()
							.requestMatchers(HttpMethod.HEAD, "/certificados/imagens/**")
							.permitAll()
							.requestMatchers(HttpMethod.GET, "/certificados")
						.hasRole("ADMIN")
						.requestMatchers(HttpMethod.POST, "/usuarios", "/auth/login", "/auth/esqueci-senha", "/auth/redefinir-senha")
						.permitAll()
						.requestMatchers(HttpMethod.GET, "/usuarios")
						.hasRole("ADMIN")
						.anyRequest()
						.authenticated())
				.oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter(usuarioRepository))))
				.authenticationProvider(authenticationProvider)
				.build();
	}

	@Bean
	public DaoAuthenticationProvider authenticationProvider(
			UsuarioDetailsService usuarioDetailsService,
			PasswordEncoder passwordEncoder) {
		DaoAuthenticationProvider provider = new DaoAuthenticationProvider(usuarioDetailsService);
		provider.setPasswordEncoder(passwordEncoder);
		return provider;
	}

	@Bean
	public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
		return authenticationConfiguration.getAuthenticationManager();
	}

	@Bean
	public JwtAuthenticationConverter jwtAuthenticationConverter(UsuarioRepository usuarioRepository){
		JwtGrantedAuthoritiesConverter authoritiesConverter = new JwtGrantedAuthoritiesConverter();
		
		authoritiesConverter.setAuthorityPrefix("");
		authoritiesConverter.setAuthoritiesClaimName("roles");

		JwtAuthenticationConverter authenticationConverter = new JwtAuthenticationConverter();
		authenticationConverter.setJwtGrantedAuthoritiesConverter(jwt -> {
			validarTokenVersao(jwt, usuarioRepository);
			return authoritiesConverter.convert(jwt);
		});

		return authenticationConverter;
	}

	private void validarTokenVersao(Jwt jwt, UsuarioRepository usuarioRepository) {
		Number usuarioIdClaim = jwt.getClaim("usuarioId");
		Number tokenVersionClaim = jwt.getClaim("tokenVersion");

		if (usuarioIdClaim == null) {
			throw new JwtException("Token inválido.");
		}

		UsuarioEntity usuario = usuarioRepository.findById(usuarioIdClaim.longValue())
				.orElseThrow(() -> new JwtException("Token inválido."));

		int tokenVersion = tokenVersionClaim == null ? 0 : tokenVersionClaim.intValue();
		if (usuario.getTokenVersion() != tokenVersion) {
			throw new JwtException("Token inválido.");
		}
	}

	@Bean
	public CorsConfigurationSource corsConfigurationSource() {
		CorsConfiguration configuration = new CorsConfiguration();
		List<String> origins = new ArrayList<>(List.of(
				"http://localhost:*",
				"http://127.0.0.1:*",
				"https://*.github.io",
				"https://certifique-se.app",
				"https://*.certifique-se.app",
				"https://*.vercel.app",
				"https://*.onrender.com"
		));
		if (frontendUrl != null && !frontendUrl.isBlank()) {
			String urlSemBarra = frontendUrl.trim().replaceAll("/+$", "");
			if (!origins.contains(urlSemBarra)) {
				origins.add(urlSemBarra);
			}
		}
		configuration.setAllowedOriginPatterns(origins);
		configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
		configuration.setAllowedHeaders(List.of("*"));
		configuration.setExposedHeaders(List.of("*"));
		configuration.setAllowCredentials(true);

		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", configuration);
		return source;
	}

	@Bean
	public FilterRegistrationBean<RateLimitFilter> rateLimitFilterRegistration(RateLimitFilter filter) {
		FilterRegistrationBean<RateLimitFilter> registration = new FilterRegistrationBean<>(filter);
		registration.setEnabled(false);
		return registration;
	}
}
