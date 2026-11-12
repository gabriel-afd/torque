package br.com.torque.auth;

/**
 * Usuário logado, disponível como principal do Spring Security
 * ({@code @AuthenticationPrincipal UsuarioAutenticado usuario}).
 */
public record UsuarioAutenticado(Long id, String nome, String email, Perfil perfil) {

    public static UsuarioAutenticado de(Usuario usuario) {
        return new UsuarioAutenticado(usuario.getId(), usuario.getNome(), usuario.getEmail(), usuario.getPerfil());
    }

    public boolean isAdministrador() {
        return perfil == Perfil.ADMINISTRADOR;
    }
}
