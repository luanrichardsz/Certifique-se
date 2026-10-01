package br.com.certifiquese.security.ratelimit;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

import org.springframework.stereotype.Service;

@Service
public class RateLimiterService {

    private final Map<String, ConcurrentLinkedDeque<Long>> requestLogs = new ConcurrentHashMap<>();

    public boolean tentarConsumir(String chave, int maximoRequisicoes, long janelaSegundos) {
        long agora = Instant.now().toEpochMilli();
        long limiteJanela = agora - (janelaSegundos * 1000L);

        ConcurrentLinkedDeque<Long> timestamps = requestLogs.computeIfAbsent(chave, k -> new ConcurrentLinkedDeque<>());

        synchronized (timestamps) {
            while (!timestamps.isEmpty() && timestamps.peekFirst() <= limiteJanela) {
                timestamps.pollFirst();
            }

            if (timestamps.size() < maximoRequisicoes) {
                timestamps.addLast(agora);
                return true;
            }

            return false;
        }
    }

    public long tempoParaProximaRequisicaoSegundos(String chave, long janelaSegundos) {
        long agora = Instant.now().toEpochMilli();
        ConcurrentLinkedDeque<Long> timestamps = requestLogs.get(chave);
        if (timestamps == null || timestamps.isEmpty()) {
            return 0;
        }
        synchronized (timestamps) {
            Long maisAntigo = timestamps.peekFirst();
            if (maisAntigo == null) {
                return 0;
            }
            long esperaMillis = (maisAntigo + (janelaSegundos * 1000L)) - agora;
            return Math.max(1, (esperaMillis + 999) / 1000);
        }
    }

    public void limparChavesAntigas() {
        long agora = Instant.now().toEpochMilli();
        long limiteInatividade = agora - (10 * 60 * 1000L);
        requestLogs.entrySet().removeIf(entry -> {
            ConcurrentLinkedDeque<Long> queue = entry.getValue();
            synchronized (queue) {
                return queue.isEmpty() || queue.peekLast() < limiteInatividade;
            }
        });
    }

    public void resetar() {
        requestLogs.clear();
    }
}
