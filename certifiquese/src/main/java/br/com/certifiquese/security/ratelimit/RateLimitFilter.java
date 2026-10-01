package br.com.certifiquese.security.ratelimit;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private final RateLimiterService rateLimiterService;
    private final boolean enabled;

    public RateLimitFilter(
            RateLimiterService rateLimiterService,
            @Value("${security.rate-limit.enabled:true}") boolean enabled) {
        this.rateLimiterService = rateLimiterService;
        this.enabled = enabled;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        if (!enabled || "OPTIONS".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        String path = request.getRequestURI();
        String method = request.getMethod();

        // 1. Proteção contra Força Bruta no Login (máx 10 tentativas por minuto por IP)
        if ("POST".equalsIgnoreCase(method) && path.endsWith("/auth/login")) {
            if (!verificarLimite(request, response, "login", 10, 60, "Muitas tentativas de login. Aguarde um momento antes de tentar novamente.")) {
                return;
            }
        }
        // 2. Proteção contra Spam de E-mails na Recuperação de Senha (máx 5 solicitações a cada 2 minutos por IP)
        else if ("POST".equalsIgnoreCase(method) && path.endsWith("/auth/esqueci-senha")) {
            if (!verificarLimite(request, response, "esqueci_senha", 5, 120, "Muitas solicitações de recuperação de senha. Aguarde alguns instantes.")) {
                return;
            }
        }
        // 3. Proteção contra Abuso de Cota de IA / OCR do Gemini (máx 15 extrações por minuto por IP)
        else if ("POST".equalsIgnoreCase(method) && path.endsWith("/certificados/extrair-dados")) {
            if (!verificarLimite(request, response, "extrair_dados", 15, 60, "Limite de extrações de certificados por minuto excedido. Aguarde antes de extrair novamente.")) {
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private boolean verificarLimite(HttpServletRequest request, HttpServletResponse response,
                                    String acao, int maximo, long janelaSegundos, String mensagem) throws IOException {
        String ip = extrairIpCliente(request);
        String chave = acao + ":" + ip;

        if (!rateLimiterService.tentarConsumir(chave, maximo, janelaSegundos)) {
            long retryAfter = rateLimiterService.tempoParaProximaRequisicaoSegundos(chave, janelaSegundos);
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType("application/problem+json");
            response.setCharacterEncoding("UTF-8");
            response.setHeader("Retry-After", String.valueOf(retryAfter));

            String json = """
                    {
                        "type": "about:blank",
                        "title": "Too Many Requests",
                        "status": 429,
                        "detail": "%s",
                        "mensagem": "%s"
                    }
                    """.formatted(mensagem, mensagem);

            response.getWriter().write(json);
            return false;
        }

        return true;
    }

    private String extrairIpCliente(HttpServletRequest request) {
        String cfConnectingIp = request.getHeader("CF-Connecting-IP");
        if (cfConnectingIp != null && !cfConnectingIp.isBlank()) {
            return cfConnectingIp.trim();
        }

        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }

        return request.getRemoteAddr();
    }
}
