package br.com.torque.relatorios;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.torque.ordens.OrdemServicoApiTestBase;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.ResultActions;

/** CT47: painel do mês com contagens e faturamento (soma dos totais aprovados das OS entregues no mês). */
class PainelMesIntegracaoTest extends OrdemServicoApiTestBase {

    private ResultActions painel(String query, String token) throws Exception {
        return mvc.perform(get("/api/relatorios/mes" + query).header(HttpHeaders.AUTHORIZATION, token));
    }

    private long entregue() throws Exception {
        long os = osAprovadaComMecanico();
        mudarStatus(os, "EM_EXECUCAO", tokenMecanico).andExpect(status().isOk());
        mudarStatus(os, "CONCLUIDA", tokenMecanico).andExpect(status().isOk());
        mudarStatus(os, "ENTREGUE", tokenAtendente).andExpect(status().isOk());
        return os;
    }

    /** Move a OS no tempo: abertura, entrega (se houver) e todo o histórico de status. */
    private void datar(long os, LocalDateTime abertura, LocalDateTime ultimaMudanca) {
        jdbc.update("update ordem_servico set data_abertura = ?, data_entrega = case when data_entrega is null"
                + " then null else ? end where id = ?", abertura, ultimaMudanca, os);
        jdbc.update("update historico_status set data_hora = ? where ordem_servico_id = ?", ultimaMudanca, os);
    }

    /** Cenário de outubro/2026 com uma OS de setembro e uma de novembro nas bordas. */
    private void montarCenario() throws Exception {
        long entregueEmOutubro = entregue(); // R$ 190,00
        datar(entregueEmOutubro, LocalDateTime.of(2026, 10, 5, 9, 0), LocalDateTime.of(2026, 10, 20, 17, 0));

        // RN07: v1 aprovada (190) vira SUBSTITUIDO; v2 com desconto de 10 (180) é a que fatura.
        long comNovaVersao = osAprovadaComMecanico();
        putOrcamento(comNovaVersao, 1, orcamentoPadrao("10.00"), tokenAtendente).andExpect(status().isOk());
        decidir(comNovaVersao, 2, "aprovacao", tokenAtendente).andExpect(status().isOk());
        mudarStatus(comNovaVersao, "EM_EXECUCAO", tokenMecanico).andExpect(status().isOk());
        mudarStatus(comNovaVersao, "CONCLUIDA", tokenMecanico).andExpect(status().isOk());
        mudarStatus(comNovaVersao, "ENTREGUE", tokenAtendente).andExpect(status().isOk());
        datar(comNovaVersao, LocalDateTime.of(2026, 10, 3, 8, 0), LocalDateTime.of(2026, 10, 25, 16, 0));

        long entregueEmSetembro = entregue();
        datar(entregueEmSetembro, LocalDateTime.of(2026, 9, 10, 8, 0), LocalDateTime.of(2026, 9, 30, 23, 59, 59));

        long concluidaSemEntrega = osAprovadaComMecanico();
        mudarStatus(concluidaSemEntrega, "EM_EXECUCAO", tokenMecanico).andExpect(status().isOk());
        mudarStatus(concluidaSemEntrega, "CONCLUIDA", tokenMecanico).andExpect(status().isOk());
        datar(concluidaSemEntrega, LocalDateTime.of(2026, 10, 12, 10, 0), LocalDateTime.of(2026, 10, 28, 11, 0));

        long abertaNoUltimoSegundo = abrirOs();
        LocalDateTime fimDeOutubro = LocalDateTime.of(2026, 10, 31, 23, 59, 59);
        datar(abertaNoUltimoSegundo, fimDeOutubro, fimDeOutubro);

        long abertaEmNovembro = abrirOs();
        LocalDateTime inicioDeNovembro = LocalDateTime.of(2026, 11, 1, 0, 0);
        datar(abertaEmNovembro, inicioDeNovembro, inicioDeNovembro);
    }

    @Test
    void deveContarOsDoMesESomarFaturamentoDasEntregues() throws Exception {
        montarCenario();

        painel("?ano=2026&mes=10", tokenAdmin)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ano").value(2026))
                .andExpect(jsonPath("$.mes").value(10))
                .andExpect(jsonPath("$.abertas").value(4))
                .andExpect(jsonPath("$.concluidas").value(3))
                .andExpect(jsonPath("$.entregues").value(2))
                .andExpect(jsonPath("$.faturamento").value(370.00));
    }

    @Test
    void deveConsiderarSoOMesPedido() throws Exception {
        montarCenario();

        painel("?ano=2026&mes=9", tokenAdmin)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.abertas").value(1))
                .andExpect(jsonPath("$.concluidas").value(1))
                .andExpect(jsonPath("$.entregues").value(1))
                .andExpect(jsonPath("$.faturamento").value(190.00));
    }

    @Test
    void deveZerarPainelDeMesSemMovimento() throws Exception {
        painel("?ano=2026&mes=1", tokenAdmin)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.abertas").value(0))
                .andExpect(jsonPath("$.concluidas").value(0))
                .andExpect(jsonPath("$.entregues").value(0))
                .andExpect(jsonPath("$.faturamento").value(0.00));
    }

    @Test
    void deveNegarPainelAAtendenteEMecanico() throws Exception {
        painel("?ano=2026&mes=10", tokenAtendente).andExpect(status().isForbidden());
        painel("?ano=2026&mes=10", tokenMecanico).andExpect(status().isForbidden());
    }

    @Test
    void deveExigirLoginParaPainel() throws Exception {
        mvc.perform(get("/api/relatorios/mes?ano=2026&mes=10")).andExpect(status().isUnauthorized());
    }

    @Test
    void deveRecusarMesInvalidoOuAusente() throws Exception {
        painel("?ano=2026&mes=13", tokenAdmin)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
        painel("?ano=2026&mes=0", tokenAdmin).andExpect(status().isBadRequest());
        painel("?mes=10", tokenAdmin).andExpect(status().isBadRequest());
        painel("?ano=abc&mes=10", tokenAdmin).andExpect(status().isBadRequest());
    }
}
