package br.com.torque.shared;

import br.com.torque.auth.Perfil;
import br.com.torque.auth.Usuario;
import br.com.torque.auth.UsuarioRepository;
import br.com.torque.auth.jwt.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
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
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(IntegracaoTestBase.Conteineres.class)
public abstract class IntegracaoTestBase {

    protected static final String SENHA_PADRAO = "senha-ficticia-123";

    private static final String TABELAS = String.join(", ",
            "historico_status", "orcamento_item_peca", "orcamento_item_servico", "orcamento",
            "ordem_servico", "veiculo", "cliente", "servico", "peca", "usuario");

    @Autowired
    protected JdbcTemplate jdbc;

    @Autowired
    protected MockMvc mvc;

    @Autowired
    protected ObjectMapper json;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

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

    // Apoio: usuários e tokens fictícios ------------------------------------

    protected Usuario criarUsuario(String nome, String email, Perfil perfil) {
        return usuarioRepository.saveAndFlush(
                new Usuario(nome, email, passwordEncoder.encode(SENHA_PADRAO), perfil));
    }

    protected Usuario criarUsuario(Perfil perfil) {
        String apelido = perfil.name().toLowerCase();
        long sequencial = usuarioRepository.count() + 1;
        return criarUsuario("Usuário " + apelido + " " + sequencial,
                apelido + sequencial + "@oficina.teste", perfil);
    }

    protected String bearer(Usuario usuario) {
        return "Bearer " + jwtService.gerarToken(usuario);
    }

    protected String bearer(Perfil perfil) {
        return bearer(criarUsuario(perfil));
    }

    protected String paraJson(Object objeto) throws Exception {
        return json.writeValueAsString(objeto);
    }
}
