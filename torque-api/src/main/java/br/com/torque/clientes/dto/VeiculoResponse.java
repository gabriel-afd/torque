package br.com.torque.clientes.dto;

import br.com.torque.clientes.Veiculo;

public record VeiculoResponse(Long id, Long clienteId, String placa, String marca, String modelo,
                              Integer ano, String cor) {

    /** Usa só o id do cliente (não dispara carga do proxy lazy). */
    public static VeiculoResponse de(Veiculo veiculo) {
        return new VeiculoResponse(veiculo.getId(), veiculo.getCliente().getId(), veiculo.getPlaca(),
                veiculo.getMarca(), veiculo.getModelo(), veiculo.getAno(), veiculo.getCor());
    }
}
