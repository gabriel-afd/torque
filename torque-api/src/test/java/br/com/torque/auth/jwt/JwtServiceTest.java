package br.com.torque.auth.jwt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.torque.auth.Perfil;
import br.com.torque.auth.Usuario;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.util.ReflectionTestUtils;

class JwtServiceTest {

    private static final String CHAVE = "chave-de-teste-ficticia-com-mais-de-32-bytes";
    private static final Instant AGORA = Instant.parse("2026-11-12T12:00:00Z");

    private static Usuario usuario() {
        Usuario usuario = new Usuario("Mário", "mario@oficina.teste", "hash", Perfil.MECANICO);
        ReflectionTestUtils.setField(usuario, "id", 7L);
        return usuario;
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"curta", "0123456789012345678901234567890"}) // 31 bytes
    void deveRecusarSubirComChaveAusenteOuMenorQue32Bytes(String chave) {
        assertThatThrownBy(() -> new JwtService(new JwtProperties(chave, 8)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("JWT_SECRET");
    }

    @Test
    void deveAceitarChaveDeExatamente32Bytes() {
        new JwtService(new JwtProperties("01234567890123456789012345678901", 8));
    }

    @Test
    void deveGerarTokenComPerfilEIdDoUsuario() {
        JwtService service = new JwtService(new JwtProperties(CHAVE, 8), Clock.fixed(AGORA, ZoneOffset.UTC));
        var claims = service.validar(service.gerarToken(usuario())).orElseThrow();

        assertThat(claims.getSubject()).isEqualTo("7");
        assertThat(claims.get(JwtService.CLAIM_PERFIL)).isEqualTo("MECANICO");
        assertThat(claims.getExpiration().toInstant()).isEqualTo(AGORA.plus(Duration.ofHours(8)));
    }

    @Test
    void deveRecusarTokenExpirado() {
        JwtService emissor = new JwtService(new JwtProperties(CHAVE, 8), Clock.fixed(AGORA, ZoneOffset.UTC));
        String token = emissor.gerarToken(usuario());

        JwtService noFuturo = new JwtService(new JwtProperties(CHAVE, 8),
                Clock.fixed(AGORA.plus(Duration.ofHours(9)), ZoneOffset.UTC));
        assertThat(noFuturo.validar(token)).isEmpty();
    }

    @Test
    void deveRecusarTokenAssinadoComOutraChave() {
        String token = new JwtService(new JwtProperties(CHAVE, 8)).gerarToken(usuario());
        JwtService outro = new JwtService(new JwtProperties(CHAVE + "-diferente", 8));
        assertThat(outro.validar(token)).isEmpty();
        assertThat(outro.validar("lixo")).isEmpty();
    }
}
