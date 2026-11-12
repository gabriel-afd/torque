package br.com.torque;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.torque.shared.IntegracaoTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

class TorqueApplicationTests extends IntegracaoTestBase {

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void deveSubirContextoEAplicarMigracoesFlyway() {
        Integer versoes = jdbc.queryForObject(
                "SELECT count(*) FROM flyway_schema_history WHERE success", Integer.class);
        assertThat(versoes).isGreaterThanOrEqualTo(1);
    }
}
