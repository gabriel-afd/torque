package br.com.torque;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.torque.shared.IntegracaoTestBase;
import org.junit.jupiter.api.Test;

class TorqueApplicationTests extends IntegracaoTestBase {

    @Test
    void deveSubirContextoEAplicarMigracoesFlyway() {
        String versao = jdbc.queryForObject(
                "SELECT max(version) FROM flyway_schema_history WHERE success", String.class);
        assertThat(versao).isNotNull();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM usuario", Integer.class)).isZero();
    }
}
