package br.com.torque.clientes.dto;

import br.com.torque.clientes.Cliente;

public record ClienteResponse(Long id, String nome, String documento, String telefone, String email, boolean ativo) {

    public static ClienteResponse de(Cliente cliente) {
        return new ClienteResponse(cliente.getId(), cliente.getNome(), cliente.getDocumento(),
                cliente.getTelefone(), cliente.getEmail(), cliente.isAtivo());
    }
}
