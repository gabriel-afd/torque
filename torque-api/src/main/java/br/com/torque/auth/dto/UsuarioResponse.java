package br.com.torque.auth.dto;

import br.com.torque.auth.Perfil;
import br.com.torque.auth.Usuario;

/** Usuário exposto pela API — nunca inclui a senha. */
public record UsuarioResponse(Long id, String nome, String email, Perfil perfil, boolean ativo) {

    public static UsuarioResponse de(Usuario usuario) {
        return new UsuarioResponse(usuario.getId(), usuario.getNome(), usuario.getEmail(),
                usuario.getPerfil(), usuario.isAtivo());
    }
}
