package br.com.torque.clientes.dto;

import br.com.torque.clientes.Cliente;
import br.com.torque.clientes.Veiculo;

/** Resultado da busca por placa: veículo e dados de contato do dono. */
public record VeiculoComDonoResponse(Long id, String placa, String marca, String modelo, Integer ano, String cor,
                                     Dono dono) {

    public record Dono(Long id, String nome, String documento, String telefone, String email, boolean ativo) {
    }

    public static VeiculoComDonoResponse de(Veiculo veiculo) {
        Cliente c = veiculo.getCliente();
        return new VeiculoComDonoResponse(veiculo.getId(), veiculo.getPlaca(), veiculo.getMarca(),
                veiculo.getModelo(), veiculo.getAno(), veiculo.getCor(),
                new Dono(c.getId(), c.getNome(), c.getDocumento(), c.getTelefone(), c.getEmail(), c.isAtivo()));
    }
}
