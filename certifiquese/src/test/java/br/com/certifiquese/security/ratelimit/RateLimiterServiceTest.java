package br.com.certifiquese.security.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RateLimiterServiceTest {

    private RateLimiterService rateLimiterService;

    @BeforeEach
    void setUp() {
        rateLimiterService = new RateLimiterService();
    }

    @Test
    @DisplayName("Deve permitir requisições até o limite máximo configurado")
    void devePermitirRequisicoesAteOLimite() {
        String chave = "login:192.168.1.1";

        for (int i = 0; i < 5; i++) {
            assertThat(rateLimiterService.tentarConsumir(chave, 5, 60))
                    .as("Tentativa " + (i + 1) + " deveria ser permitida")
                    .isTrue();
        }

        // A 6ª tentativa deve ser bloqueada
        assertThat(rateLimiterService.tentarConsumir(chave, 5, 60))
                .as("Tentativa após estourar o limite deve ser rejeitada")
                .isFalse();
    }

    @Test
    @DisplayName("Deve isolar limites para chaves/IPs diferentes")
    void deveIsolarLimitesPorChave() {
        String ip1 = "login:10.0.0.1";
        String ip2 = "login:10.0.0.2";

        for (int i = 0; i < 3; i++) {
            rateLimiterService.tentarConsumir(ip1, 3, 60);
        }

        // IP1 estourou
        assertThat(rateLimiterService.tentarConsumir(ip1, 3, 60)).isFalse();

        // IP2 ainda deve poder consumir
        assertThat(rateLimiterService.tentarConsumir(ip2, 3, 60)).isTrue();
    }

    @Test
    @DisplayName("Deve calcular tempo de espera positivo quando limite for excedido")
    void deveCalcularTempoEspera() {
        String chave = "esqueci:192.168.1.10";

        for (int i = 0; i < 2; i++) {
            rateLimiterService.tentarConsumir(chave, 2, 60);
        }

        long tempoEspera = rateLimiterService.tempoParaProximaRequisicaoSegundos(chave, 60);
        assertThat(tempoEspera).isGreaterThan(0).isLessThanOrEqualTo(60);
    }
}
