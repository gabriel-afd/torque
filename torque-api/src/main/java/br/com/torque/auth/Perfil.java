package br.com.torque.auth;

/** Perfis de acesso. O nome é gravado na coluna {@code usuario.perfil}. */
public enum Perfil {
    ADMINISTRADOR,
    ATENDENTE,
    MECANICO;

    /** Autoridade usada pelo Spring Security ({@code hasRole('ADMINISTRADOR')}). */
    public String autoridade() {
        return "ROLE_" + name();
    }
}
