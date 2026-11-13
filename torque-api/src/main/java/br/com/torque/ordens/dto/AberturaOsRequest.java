package br.com.torque.ordens.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record AberturaOsRequest(
        @NotNull Long veiculoId,
        @NotBlank @Size(max = 2000) String problemaRelatado,
        @PositiveOrZero Integer quilometragem) {
}
