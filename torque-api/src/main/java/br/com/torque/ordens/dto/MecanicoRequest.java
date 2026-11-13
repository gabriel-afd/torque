package br.com.torque.ordens.dto;

import jakarta.validation.constraints.NotNull;

/** @param versaoOs versão da OS que o cliente carregou (opcional); diferente da atual → 409 */
public record MecanicoRequest(@NotNull Long mecanicoId, Integer versaoOs) {
}
