package br.com.torque.ordens.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Projeções usadas para montar listagens sem N+1. */
public final class ConsultasOrdem {

    private ConsultasOrdem() {
    }

    public record TotalVigente(Long ordemId, BigDecimal total) {
    }

    public record UltimaMudanca(Long ordemId, LocalDateTime dataHora) {
    }
}
