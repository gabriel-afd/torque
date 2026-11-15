package br.com.torque.relatorios.dto;

import java.math.BigDecimal;

/**
 * Painel do mês (RF16). {@code faturamento} = soma dos totais dos orçamentos aprovados das OS
 * entregues no mês.
 */
public record PainelMesResponse(int ano, int mes, long abertas, long concluidas, long entregues,
                                BigDecimal faturamento) {
}
