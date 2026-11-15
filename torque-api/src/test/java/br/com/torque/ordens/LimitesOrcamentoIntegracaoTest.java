package br.com.torque.ordens;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.torque.catalogo.Peca;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Auditoria A04: valores que não cabem em NUMERIC(10,2) não podem chegar ao banco (antes viravam
 * 409 "conflito" por violação de integridade).
 */
class LimitesOrcamentoIntegracaoTest extends OrdemServicoApiTestBase {

    @Test
    void deveRecusarQuantidadeDePecaAcimaDoLimiteCom400() throws Exception {
        long os = abrirOs();

        putOrcamento(os, 1, Map.of("servicos", List.of(),
                        "pecas", List.of(Map.of("pecaId", filtro.getId(), "quantidade", 3_000_000)),
                        "desconto", BigDecimal.ZERO), tokenAtendente)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos['pecas[0].quantidade']").exists());
    }

    @Test
    void deveRecusarOrcamentoComTotalAcimaDoLimiteDoBancoCom422() throws Exception {
        Peca motor = pecas.save(new Peca("MOT-1", "Motor completo", new BigDecimal("50000.00")));
        long os = abrirOs();

        putOrcamento(os, 1, Map.of("servicos", List.of(),
                        "pecas", List.of(Map.of("pecaId", motor.getId(), "quantidade", 9_999)),
                        "desconto", BigDecimal.ZERO), tokenAdmin)
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.mensagem").value(org.hamcrest.Matchers.containsString("limite")));
    }
}
