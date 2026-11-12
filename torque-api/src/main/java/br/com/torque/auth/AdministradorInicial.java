package br.com.torque.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Na subida, se não houver nenhum usuário, cria o primeiro administrador a partir de
 * ADMIN_EMAIL e ADMIN_SENHA. Sem as variáveis, não cria nada e registra aviso.
 */
@Component
public class AdministradorInicial implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdministradorInicial.class);
    static final int TAMANHO_MINIMO_SENHA = 8;

    private final UsuarioRepository usuarios;
    private final PasswordEncoder passwordEncoder;
    private final String email;
    private final String senha;

    public AdministradorInicial(UsuarioRepository usuarios, PasswordEncoder passwordEncoder,
                                @Value("${torque.admin.email:}") String email,
                                @Value("${torque.admin.senha:}") String senha) {
        this.usuarios = usuarios;
        this.passwordEncoder = passwordEncoder;
        this.email = email;
        this.senha = senha;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (usuarios.count() > 0) {
            return;
        }
        if (email == null || email.isBlank() || senha == null || senha.isBlank()) {
            log.warn("Nenhum usuário cadastrado e ADMIN_EMAIL/ADMIN_SENHA não definidos: administrador inicial não criado.");
            return;
        }
        if (senha.length() < TAMANHO_MINIMO_SENHA) {
            log.warn("ADMIN_SENHA tem menos de {} caracteres: administrador inicial não criado.", TAMANHO_MINIMO_SENHA);
            return;
        }
        Usuario admin = new Usuario("Administrador", email, passwordEncoder.encode(senha), Perfil.ADMINISTRADOR);
        usuarios.save(admin);
        log.info("Administrador inicial criado: {}", admin.getEmail());
    }
}
