package br.com.certifiquese.security.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import java.io.IOException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import jakarta.servlet.ServletException;

class RateLimitFilterTest {

    private RateLimiterService rateLimiterService;
    private RateLimitFilter filter;

    @BeforeEach
    void setUp() {
        rateLimiterService = new RateLimiterService();
        filter = new RateLimitFilter(rateLimiterService, true);
    }

    @Test
    @DisplayName("Deve permitir requisições de login normais abaixo do limite")
    void devePermitirRequisicoesNormais() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/auth/login");
        request.setRemoteAddr("192.168.1.100");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    @DisplayName("Deve bloquear requisições com 429 Too Many Requests ao exceder o limite de login")
    void deveBloquearCom429AoExcederLimite() throws ServletException, IOException {
        String ip = "192.168.1.200";

        for (int i = 0; i < 10; i++) {
            MockHttpServletRequest req = new MockHttpServletRequest("POST", "/auth/login");
            req.setRemoteAddr(ip);
            MockHttpServletResponse res = new MockHttpServletResponse();
            filter.doFilter(req, res, new MockFilterChain());
            assertThat(res.getStatus()).isEqualTo(200);
        }

        // 11ª requisição deve retornar 429
        MockHttpServletRequest reqBloqueada = new MockHttpServletRequest("POST", "/auth/login");
        reqBloqueada.setRemoteAddr(ip);
        MockHttpServletResponse resBloqueada = new MockHttpServletResponse();

        filter.doFilter(reqBloqueada, resBloqueada, new MockFilterChain());

        assertThat(resBloqueada.getStatus()).isEqualTo(429);
        assertThat(resBloqueada.getHeader("Retry-After")).isNotNull();
        assertThat(resBloqueada.getContentAsString()).contains("Muitas tentativas de login");
    }

    @Test
    @DisplayName("Deve sempre permitir requisições OPTIONS (preflight CORS)")
    void devePermitirPreflightCors() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("OPTIONS", "/auth/login");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    @DisplayName("Deve respeitar flag disabled")
    void deveRespeitarFlagDisabled() throws ServletException, IOException {
        RateLimitFilter filterDisabled = new RateLimitFilter(rateLimiterService, false);
        String ip = "192.168.1.250";

        for (int i = 0; i < 15; i++) {
            MockHttpServletRequest req = new MockHttpServletRequest("POST", "/auth/login");
            req.setRemoteAddr(ip);
            MockHttpServletResponse res = new MockHttpServletResponse();
            filterDisabled.doFilter(req, res, new MockFilterChain());
            assertThat(res.getStatus()).isEqualTo(200);
        }
    }
}
