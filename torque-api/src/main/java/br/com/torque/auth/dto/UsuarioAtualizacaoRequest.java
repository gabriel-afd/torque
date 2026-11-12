package br.com.torque.auth.dto;

import br.com.torque.auth.Perfil;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Edição de usuário. {@code senha} é opcional: quando vazia, a senha atual é mantida. */
public record UsuarioAtualizacaoRequest(
        @NotBlank @Size(max = 120) String nome,
        @NotBlank @Email @Size(max = 160) String email,
        @Size(min = 8, max = 72, message = "A senha deve ter entre 8 e 72 caracteres") String senha,
        @NotNull Perfil perfil) {

    @Override
    public String toString() {
        return "UsuarioAtualizacaoRequest[nome=" + nome + ", email=" + email + ", senha=***, perfil=" + perfil + "]";
    }
}
