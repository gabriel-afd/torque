package br.com.torque.clientes.dto;

import br.com.torque.shared.validacao.CpfCnpj;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Cadastro e edição de cliente. Documento e telefone aceitam máscara. */
public record ClienteRequest(
        @NotBlank @Size(max = 120) String nome,
        @NotBlank @CpfCnpj String documento,
        @NotBlank
        @Pattern(regexp = "^[\\s()+\\-.]*(\\d[\\s()+\\-.]*){10,11}$",
                message = "Telefone deve ter DDD e número (10 ou 11 dígitos)")
        String telefone,
        @Email @Size(max = 160) String email) {
}
