package br.com.torque.auth.dto;

import br.com.torque.auth.Perfil;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Cadastro de usuário. */
public record UsuarioRequest(
        @NotBlank @Size(max = 120) String nome,
        @NotBlank @Email @Size(max = 160) String email,
        @NotBlank @Size(min = 8, max = 72, message = "A senha deve ter entre 8 e 72 caracteres") String senha,
        @NotNull Perfil perfil) {

    @Override
    public String toString() {
        return "UsuarioRequest[nome=" + nome + ", email=" + email + ", senha=***, perfil=" + perfil + "]";
    }
}
