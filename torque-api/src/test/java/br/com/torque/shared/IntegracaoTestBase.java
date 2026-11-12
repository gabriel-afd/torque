package br.com.torque.shared;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Base dos testes de integração.
 *
 * <p>Por padrão sobe um PostgreSQL 16 real com Testcontainers ({@code @ServiceConnection}); o contêiner é
 * compartilhado por todas as classes que usam o mesmo contexto Spring. As migrações Flyway são aplicadas
 * na subida e as tabelas de dados são esvaziadas antes de cada teste.
 *
 * <p>Alternativa para máquinas sem Docker: defina {@code TORQUE_TEST_DB_URL} (e opcionalmente
 * {@code TORQUE_TEST_DB_USER}/{@code TORQUE_TEST_DB_PASSWORD}) apontando para um PostgreSQL
 * <b>descartável</b> — os dados são apagados a cada teste.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
@Import(IntegracaoTestBase.Conteineres.class)
public abstract class IntegracaoTestBase {

    private static final String TABELAS = String.join(", ",
            "historico_status", "orcamento_item_peca", "orcamento_item_servico", "orcamento",
            "ordem_servico", "veiculo", "cliente", "servico", "peca", "usuario");

    @Autowired
    protected JdbcTemplate jdbc;

    @TestConfiguration(proxyBeanMethods = false)
    static class Conteineres {

        @Bean
        @ServiceConnection
        @ConditionalOnExpression("'${TORQUE_TEST_DB_URL:}'.isEmpty()")
        PostgreSQLContainer<?> postgres() {
            return new PostgreSQLContainer<>("postgres:16-alpine");
        }
    }

    @DynamicPropertySource
    static void bancoExterno(DynamicPropertyRegistry registry) {
        String url = System.getenv("TORQUE_TEST_DB_URL");
        if (url != null && !url.isBlank()) {
            registry.add("spring.datasource.url", () -> url);
            registry.add("spring.datasource.username", () -> env("TORQUE_TEST_DB_USER", "torque"));
            registry.add("spring.datasource.password", () -> env("TORQUE_TEST_DB_PASSWORD", ""));
        }
    }

    private static String env(String nome, String padrao) {
        String valor = System.getenv(nome);
        return valor == null ? padrao : valor;
    }

    @BeforeEach
    void limparBanco() {
        jdbc.execute("TRUNCATE " + TABELAS + " RESTART IDENTITY CASCADE");
    }
}
