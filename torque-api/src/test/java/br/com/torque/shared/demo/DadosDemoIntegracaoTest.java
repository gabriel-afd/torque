package br.com.torque.shared.demo;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.torque.ordens.StatusOS;
import br.com.torque.shared.IntegracaoTestBase;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;

/** Perfil demo: carrega dados fictícios coerentes com as regras (passando pelo domínio). */
@ActiveProfiles({"test", "demo"})
class DadosDemoIntegracaoTest extends IntegracaoTestBase {

    @Autowired
    private DadosDemo dadosDemo;

    private long contar(String tabela) {
        return jdbc.queryForObject("select count(*) from " + tabela, Long.class);
    }

    @Test
    void deveCarregarUsuariosClientesVeiculosEOsEmTodosOsStatus() {
        dadosDemo.run(null);

        assertThat(jdbc.queryForList("select perfil from usuario", String.class))
                .containsExactlyInAnyOrder("ADMINISTRADOR", "ATENDENTE", "MECANICO", "MECANICO");
        assertThat(contar("cliente")).isEqualTo(10);
        assertThat(contar("veiculo")).isEqualTo(15);
        List<String> status = jdbc.queryForList("select distinct status from ordem_servico", String.class);
        assertThat(status).containsExactlyInAnyOrderElementsOf(Arrays.stream(StatusOS.values()).map(Enum::name).toList());
    }

    @Test
    void deveGravarHistoricoENovaVersaoDeOrcamentoPeloDominio() {
        dadosDemo.run(null);

        assertThat(contar("historico_status")).isGreaterThan(contar("ordem_servico"));
        assertThat(jdbc.queryForObject("select count(*) from orcamento where situacao = 'SUBSTITUIDO'", Long.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject(
                "select count(*) from ordem_servico where status in ('APROVADA','EM_EXECUCAO','AGUARDANDO_PECA','CONCLUIDA','ENTREGUE')"
                        + " and mecanico_id is null", Long.class)).isZero();
    }

    @Test
    void naoDeveDuplicarQuandoOBancoJaTemClientes() {
        dadosDemo.run(null);
        dadosDemo.run(null);

        assertThat(contar("cliente")).isEqualTo(10);
    }
}
