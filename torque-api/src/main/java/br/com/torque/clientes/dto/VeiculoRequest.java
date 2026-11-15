package br.com.torque.clientes.dto;

import br.com.torque.shared.validacao.Placa;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Cadastro e edição de veículo. A placa aceita minúsculas e hífen; é gravada normalizada. */
public record VeiculoRequest(
        @NotBlank @Placa String placa,
        @NotBlank @Size(max = 60) String marca,
        @NotBlank @Size(max = 80) String modelo,
        @Min(1900) @Max(2100) Integer ano,
        @Size(max = 30) String cor) {
}
