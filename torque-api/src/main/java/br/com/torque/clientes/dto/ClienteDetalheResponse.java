package br.com.torque.clientes.dto;

import br.com.torque.clientes.Cliente;
import br.com.torque.clientes.Veiculo;
import java.util.List;

/** Cliente com seus veículos. */
public record ClienteDetalheResponse(Long id, String nome, String documento, String telefone, String email,
                                     boolean ativo, List<VeiculoResponse> veiculos) {

    public static ClienteDetalheResponse de(Cliente cliente, List<Veiculo> veiculos) {
        return new ClienteDetalheResponse(cliente.getId(), cliente.getNome(), cliente.getDocumento(),
                cliente.getTelefone(), cliente.getEmail(), cliente.isAtivo(),
                veiculos.stream().map(VeiculoResponse::de).toList());
    }
}
