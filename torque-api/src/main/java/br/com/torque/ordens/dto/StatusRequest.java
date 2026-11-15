package br.com.torque.ordens.dto;

import br.com.torque.ordens.StatusOS;
import jakarta.validation.constraints.NotNull;

/** @param versaoOs versão da OS que o cliente carregou (opcional); diferente da atual → 409 */
public record StatusRequest(@NotNull StatusOS status, Integer versaoOs) {
}
