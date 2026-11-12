package br.com.torque.auth;

import br.com.torque.auth.dto.LoginRequest;
import br.com.torque.auth.dto.LoginResponse;
import br.com.torque.auth.jwt.JwtService;
import java.util.Optional;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    public static final String MENSAGEM_CREDENCIAIS_INVALIDAS = "E-mail ou senha inválidos";

    private final UsuarioRepository usuarios;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    /** Hash usado quando o e-mail não existe, para o tempo de resposta não revelar se o e-mail está cadastrado. */
    private final String hashFicticio;

    public AuthService(UsuarioRepository usuarios, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.usuarios = usuarios;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.hashFicticio = passwordEncoder.encode("senha-ficticia-para-equalizar-tempo");
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        Optional<Usuario> encontrado = usuarios.findByEmail(Usuario.normalizarEmail(request.email()));
        String hash = encontrado.map(Usuario::getSenhaHash).orElse(hashFicticio);
        boolean senhaConfere = passwordEncoder.matches(request.senha(), hash);

        Usuario usuario = encontrado
                .filter(u -> senhaConfere && u.isAtivo())
                .orElseThrow(() -> new BadCredentialsException(MENSAGEM_CREDENCIAIS_INVALIDAS));
        return new LoginResponse(jwtService.gerarToken(usuario), usuario.getNome(), usuario.getPerfil());
    }
}
