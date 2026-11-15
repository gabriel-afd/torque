package br.com.torque.ordens;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.torque.auth.Perfil;
import br.com.torque.auth.Usuario;
import br.com.torque.clientes.Cliente;
import br.com.torque.clientes.Veiculo;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.EntityManagerFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;

/** Quadro de OS (CT42, CT43), histórico do veículo (CT44) e ausência de N+1. */
class OrdemServicoConsultaIntegracaoTest extends OrdemServicoApiTestBase {

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    private JsonNode listar(String query, String token) throws Exception {
        return corpo(mvc.perform(get("/api/ordens" + query).header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isOk()));
    }

    private List<Long> ids(JsonNode pagina) {
        List<Long> ids = new ArrayList<>();
        pagina.get("content").forEach(n -> ids.add(n.get("id").asLong()));
        return ids;
    }

    @Test
    void deveFiltrarQuadroPorStatusComPaginacao() throws Exception {
        long emExecucao1 = osAprovadaComMecanico();
        mudarStatus(emExecucao1, "EM_EXECUCAO", tokenMecanico);
        long emExecucao2 = osAprovadaComMecanico();
        mudarStatus(emExecucao2, "EM_EXECUCAO", tokenMecanico);
        long aberta = abrirOs();

        JsonNode pagina = listar("?status=EM_EXECUCAO&size=1", tokenAtendente);
        assertThat(pagina.at("/page/totalElements").asInt()).isEqualTo(2);
        assertThat(pagina.get("content")).hasSize(1);
        assertThat(pagina.at("/content/0/status").asText()).isEqualTo("EM_EXECUCAO");

        assertThat(ids(listar("?status=EM_EXECUCAO", tokenAtendente))).containsExactlyInAnyOrder(emExecucao1, emExecucao2);
        assertThat(ids(listar("?status=ABERTA&status=EM_EXECUCAO", tokenAtendente)))
                .containsExactlyInAnyOrder(emExecucao1, emExecucao2, aberta);
    }

    @Test
    void deveMontarCartaoDoQuadroComTotalEDataDoStatus() throws Exception {
        long os = osAprovadaComMecanico();

        JsonNode cartao = listar("", tokenAtendente).at("/content/0");
        assertThat(cartao.get("id").asLong()).isEqualTo(os);
        assertThat(cartao.get("placa").asText()).isEqualTo("ABC1D23");
        assertThat(cartao.get("veiculo").asText()).isEqualTo("Fiat Uno");
        assertThat(cartao.get("cliente").asText()).isEqualTo("Joana Fictícia");
        assertThat(cartao.get("mecanico").asText()).isEqualTo("Mário Mecânico");
        assertThat(cartao.get("total").decimalValue()).isEqualByComparingTo("190.00");
        assertThat(cartao.get("dataStatus").isNull()).isFalse();
    }

    @Test
    void deveFiltrarQuadroPorMecanico() throws Exception {
        long doMario = osAprovadaComMecanico();
        Usuario pedro = criarUsuario("Pedro Mecânico", "pedro@oficina.teste", Perfil.MECANICO);
        long doPedro = abrirOs();
        atribuir(doPedro, pedro, tokenAdmin).andExpect(status().isOk());
        abrirOs(); // sem mecânico

        assertThat(ids(listar("?mecanico=" + mecanico.getId(), tokenAdmin))).containsExactly(doMario);
        assertThat(ids(listar("?mecanico=" + pedro.getId(), tokenAtendente))).containsExactly(doPedro);
    }

    @Test
    void deveMostrarAoMecanicoSoAsPropriasOsMesmoPedindoOutro() throws Exception {
        long doMario = osAprovadaComMecanico();
        Usuario pedro = criarUsuario("Pedro Mecânico", "pedro@oficina.teste", Perfil.MECANICO);
        long doPedro = abrirOs();
        atribuir(doPedro, pedro, tokenAdmin).andExpect(status().isOk());

        assertThat(ids(listar("", tokenMecanico))).containsExactly(doMario);
        assertThat(ids(listar("?mecanico=" + pedro.getId(), tokenMecanico))).containsExactly(doMario);
    }

    @Test
    void deveBuscarPorPlacaOuNomeDoCliente() throws Exception {
        long daJoana = abrirOs();
        Cliente outro = clientes.save(new Cliente("Carlos Fictício", "11144477735", "83988887777", null));
        Veiculo gol = veiculos.save(outro.novoVeiculo("XYZ9876", "VW", "Gol", 2010, null));
        long doCarlos = corpo(abrirOs(gol.getId(), tokenAtendente)).get("id").asLong();

        assertThat(ids(listar("?busca=abc-1d", tokenAtendente))).containsExactly(daJoana);
        assertThat(ids(listar("?busca=carlos", tokenAtendente))).containsExactly(doCarlos);
        assertThat(ids(listar("?busca=FICTÍCI", tokenAtendente))).containsExactlyInAnyOrder(daJoana, doCarlos);
        assertThat(ids(listar("?busca=inexistente", tokenAtendente))).isEmpty();
    }

    @Test
    void deveListarHistoricoDoVeiculoDaMaisRecenteParaAMaisAntiga() throws Exception {
        long primeira = osAprovadaComMecanico();
        long segunda = abrirOs();
        putOrcamento(segunda, 1, Map.of("servicos", List.of(Map.of("servicoId", trocaDeOleo.getId())),
                "pecas", List.of(), "desconto", 0), tokenAtendente);
        long terceira = abrirOs();
        Veiculo outro = veiculos.save(clientes.findAll().get(0).novoVeiculo("QWE1234", "Ford", "Ka", 2012, null));
        abrirOs(outro.getId(), tokenAtendente); // de outro veículo: não aparece

        JsonNode pagina = corpo(mvc.perform(get("/api/veiculos/" + veiculo.getId() + "/ordens")
                        .header(HttpHeaders.AUTHORIZATION, tokenMecanico))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.totalElements").value(3)));

        assertThat(ids(pagina)).containsExactly(terceira, segunda, primeira);
        JsonNode antiga = pagina.at("/content/2");
        assertThat(antiga.get("quilometragem").asInt()).isEqualTo(85000);
        assertThat(antiga.get("dataAbertura").isNull()).isFalse();
        assertThat(antiga.get("servicos").get(0).asText()).isEqualTo("Troca de óleo");
        assertThat(antiga.get("total").decimalValue()).isEqualByComparingTo("190.00");
        assertThat(antiga.get("status").asText()).isEqualTo("APROVADA");
        assertThat(pagina.at("/content/0/servicos")).isEmpty();
    }

    @Test
    void deveDevolver404NoHistoricoDeVeiculoInexistente() throws Exception {
        mvc.perform(get("/api/veiculos/999999/ordens").header(HttpHeaders.AUTHORIZATION, tokenAtendente))
                .andExpect(status().isNotFound());
    }

    /** O número de comandos SQL da listagem não pode crescer com a quantidade de OS na página. */
    @Test
    void naoDeveFazerConsultasNMaisUmNasListagens() throws Exception {
        Statistics estatisticas = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        estatisticas.setStatisticsEnabled(true);
        for (int i = 0; i < 2; i++) {
            osAprovadaComMecanico();
        }
        long comDuas = contarConsultas(estatisticas);
        for (int i = 0; i < 4; i++) {
            Cliente cliente = clientes.save(new Cliente("Cliente " + i, documentoFicticio(i), "83900000000", null));
            Veiculo v = veiculos.save(cliente.novoVeiculo("TST" + i + "A00", "Marca", "Modelo " + i, 2000, null));
            abrirOs(v.getId(), tokenAtendente);
            Usuario m = criarUsuario("Mecânico " + i, "mec" + i + "@oficina.teste", Perfil.MECANICO);
            long os = osAprovadaComMecanico();
            atribuir(os, m, tokenAdmin);
        }
        long comDez = contarConsultas(estatisticas);

        assertThat(comDez).isEqualTo(comDuas);
        assertThat(comDuas).isLessThanOrEqualTo(6); // usuário do token + página + count + totais + datas (+ histórico)
    }

    private long contarConsultas(Statistics estatisticas) throws Exception {
        estatisticas.clear();
        listar("?size=50", tokenAdmin);
        long quadro = estatisticas.getPrepareStatementCount();
        estatisticas.clear();
        mvc.perform(get("/api/veiculos/" + veiculo.getId() + "/ordens").header(HttpHeaders.AUTHORIZATION, tokenAdmin))
                .andExpect(status().isOk());
        long historico = estatisticas.getPrepareStatementCount();
        return Math.max(quadro, historico);
    }

    private static String documentoFicticio(int i) {
        return List.of("39053344705", "71428793860", "86288366757", "45317828791").get(i);
    }
}
