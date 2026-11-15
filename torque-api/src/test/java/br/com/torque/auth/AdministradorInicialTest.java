package br.com.torque.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.password.PasswordEncoder;

class AdministradorInicialTest {

    private final UsuarioRepository usuarios = mock(UsuarioRepository.class);
    private final PasswordEncoder encoder = mock(PasswordEncoder.class);

    @Test
    void deveCriarAdministradorQuandoNaoHaUsuariosEVariaveisDefinidas() {
        when(usuarios.count()).thenReturn(0L);
        when(encoder.encode("senhaInicial123")).thenReturn("$2a$hash");

        new AdministradorInicial(usuarios, encoder, "Admin@Oficina.teste", "senhaInicial123").run(null);

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarios).save(captor.capture());
        assertThat(captor.getValue().getEmail()).isEqualTo("admin@oficina.teste");
        assertThat(captor.getValue().getPerfil()).isEqualTo(Perfil.ADMINISTRADOR);
        assertThat(captor.getValue().getSenhaHash()).isEqualTo("$2a$hash");
    }

    @Test
    void naoDeveCriarQuandoVariaveisAusentes() {
        when(usuarios.count()).thenReturn(0L);

        new AdministradorInicial(usuarios, encoder, "", "").run(null);
        new AdministradorInicial(usuarios, encoder, "admin@oficina.teste", null).run(null);

        verify(usuarios, never()).save(any());
    }

    @Test
    void naoDeveCriarQuandoJaExistemUsuarios() {
        when(usuarios.count()).thenReturn(1L);

        new AdministradorInicial(usuarios, encoder, "admin@oficina.teste", "senhaInicial123").run(null);

        verify(usuarios, never()).save(any());
    }

    @Test
    void naoDeveCriarComSenhaCurta() {
        when(usuarios.count()).thenReturn(0L);

        new AdministradorInicial(usuarios, encoder, "admin@oficina.teste", "1234").run(null);

        verify(usuarios, never()).save(any());
    }
}
