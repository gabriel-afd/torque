package br.com.torque.auth.jwt;

import br.com.torque.auth.Usuario;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/** Emite e valida tokens JWT (HS256). */
@Service
public class JwtService {

    static final int TAMANHO_MINIMO_CHAVE = 32;
    public static final String CLAIM_PERFIL = "perfil";
    public static final String CLAIM_NOME = "nome";

    private final SecretKey chave;
    private final Duration validade;
    private final Clock clock;

    @Autowired
    public JwtService(JwtProperties propriedades) {
        this(propriedades, Clock.systemUTC());
    }

    JwtService(JwtProperties propriedades, Clock clock) {
        String secret = propriedades.secret();
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < TAMANHO_MINIMO_CHAVE) {
            throw new IllegalStateException(
                    "JWT_SECRET ausente ou com menos de " + TAMANHO_MINIMO_CHAVE + " bytes. Defina a variável de ambiente.");
        }
        if (propriedades.expiracaoHoras() <= 0) {
            throw new IllegalStateException("torque.jwt.expiracao-horas deve ser maior que zero.");
        }
        this.chave = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.validade = Duration.ofHours(propriedades.expiracaoHoras());
        this.clock = clock;
    }

    public String gerarToken(Usuario usuario) {
        Instant agora = clock.instant();
        return Jwts.builder()
                .subject(String.valueOf(usuario.getId()))
                .claim(CLAIM_PERFIL, usuario.getPerfil().name())
                .claim(CLAIM_NOME, usuario.getNome())
                .issuedAt(Date.from(agora))
                .expiration(Date.from(agora.plus(validade)))
                .signWith(chave)
                .compact();
    }

    /** Devolve as claims se o token for válido e não estiver expirado. */
    public Optional<Claims> validar(String token) {
        try {
            return Optional.of(Jwts.parser()
                    .verifyWith(chave)
                    .clock(() -> Date.from(clock.instant()))
                    .build()
                    .parseSignedClaims(token)
                    .getPayload());
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
