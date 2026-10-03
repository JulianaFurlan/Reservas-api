package com.SistemaReservas.reservas_api.security;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * Rate limiting simples em memória para o endpoint de login, usado para mitigar
 * força bruta de senha. Bloqueia por chave (email ou IP) após N falhas consecutivas.
 * Em memória: suficiente para uma única instância; não sobrevive a múltiplas réplicas.
 */
@Component
public class LoginAttemptService {

    private static final int MAX_FALHAS = 5;
    private static final Duration DURACAO_BLOQUEIO = Duration.ofMinutes(15);
    private static final Duration JANELA_INATIVIDADE = Duration.ofHours(2);

    private record Estado(int falhas, Instant bloqueadoAte, Instant ultimaTentativa) {}

    private final Map<String, Estado> estadosPorChave = new ConcurrentHashMap<>();

    public void verificarBloqueio(String chave) {
        Estado estado = estadosPorChave.get(chave);
        if (estado != null && estado.bloqueadoAte() != null && Instant.now().isBefore(estado.bloqueadoAte())) {
            long minutosRestantes = Duration.between(Instant.now(), estado.bloqueadoAte()).toMinutes() + 1;
            throw new LoginBloqueadoException(
                    "Muitas tentativas de login. Tente novamente em " + minutosRestantes + " minuto(s).");
        }
    }

    public void registrarFalha(String chave) {
        estadosPorChave.compute(chave, (k, atual) -> {
            int falhas = (atual == null ? 0 : atual.falhas()) + 1;
            Instant bloqueadoAte = falhas >= MAX_FALHAS ? Instant.now().plus(DURACAO_BLOQUEIO) : null;
            return new Estado(falhas, bloqueadoAte, Instant.now());
        });
    }

    public void registrarSucesso(String chave) {
        estadosPorChave.remove(chave);
    }

    @Scheduled(fixedRate = 30 * 60 * 1000L)
    void limparEstadosAntigos() {
        Instant limite = Instant.now().minus(JANELA_INATIVIDADE);
        estadosPorChave.entrySet().removeIf(entry -> entry.getValue().ultimaTentativa().isBefore(limite));
    }
}
