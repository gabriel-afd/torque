package br.com.torque.clientes.dto;

import br.com.torque.clientes.Cliente;
import com.fasterxml.jackson.annotation.JsonInclude;
import br.com.torque.clientes.Veiculo;

/** Resultado da busca por placa: veículo e dados de contato do dono. */
public record VeiculoComDonoResponse(Long id, String placa, String marca, String modelo, Integer ano, String cor,
                                     Dono dono) {

    /** Documento, telefone e e-mail ficam de fora (null) para o MECANICO. */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Dono(Long id, String nome, String documento, String telefone, String email, boolean ativo) {
    }

    public static VeiculoComDonoResponse de(Veiculo veiculo) {
        Cliente c = veiculo.getCliente();
        return new VeiculoComDonoResponse(veiculo.getId(), veiculo.getPlaca(), veiculo.getMarca(),
                veiculo.getModelo(), veiculo.getAno(), veiculo.getCor(),
                new Dono(c.getId(), c.getNome(), c.getDocumento(), c.getTelefone(), c.getEmail(), c.isAtivo()));
    }

    /** Cópia sem CPF/CNPJ, telefone e e-mail do dono (minimização de dados para o MECANICO). */
    public VeiculoComDonoResponse semDadosPessoais() {
        return new VeiculoComDonoResponse(id, placa, marca, modelo, ano, cor,
                new Dono(dono.id(), dono.nome(), null, null, null, dono.ativo()));
    }
}
