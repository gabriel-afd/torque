package br.com.torque.auth.jwt;

import br.com.torque.auth.Usuario;
import br.com.torque.auth.UsuarioAutenticado;
import br.com.torque.auth.UsuarioRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Lê o cabeçalho {@code Authorization: Bearer <token>} e autentica a requisição.
 * O usuário é recarregado do banco: se foi inativado ou mudou de perfil, vale na hora, sem esperar o token expirar.
 * Registrado apenas na cadeia do Spring Security (não é {@code @Component} para não virar filtro de servlet duplicado).
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String PREFIXO = "Bearer ";

    private final JwtService jwtService;
    private final UsuarioRepository usuarios;

    public JwtAuthenticationFilter(JwtService jwtService, UsuarioRepository usuarios) {
        this.jwtService = jwtService;
        this.usuarios = usuarios;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String cabecalho = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (cabecalho != null && cabecalho.startsWith(PREFIXO)) {
            jwtService.validar(cabecalho.substring(PREFIXO.length()))
                    .flatMap(claims -> idDoUsuario(claims.getSubject()))
                    .flatMap(usuarios::findById)
                    .filter(Usuario::isAtivo)
                    .map(UsuarioAutenticado::de)
                    .ifPresent(usuario -> {
                        var autenticacao = new UsernamePasswordAuthenticationToken(usuario, null,
                                List.of(new SimpleGrantedAuthority(usuario.perfil().autoridade())));
                        SecurityContext contexto = SecurityContextHolder.createEmptyContext();
                        contexto.setAuthentication(autenticacao);
                        SecurityContextHolder.setContext(contexto);
                    });
        }
        chain.doFilter(request, response);
    }

    private static Optional<Long> idDoUsuario(String subject) {
        try {
            return Optional.of(Long.valueOf(subject));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }
}
