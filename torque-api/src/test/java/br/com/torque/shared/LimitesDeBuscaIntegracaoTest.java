package br.com.torque.shared;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.torque.auth.Perfil;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;

/** Auditoria A06: termos de busca têm tamanho máximo (evita LIKE com textos enormes). */
class LimitesDeBuscaIntegracaoTest extends IntegracaoTestBase {

    private static final String BUSCA_LONGA = "x".repeat(101);

    @Test
    void deveRecusarBuscaLongaDemaisNasListagens() throws Exception {
        String token = bearer(Perfil.ADMINISTRADOR);
        for (String rota : new String[] {"/api/clientes", "/api/ordens", "/api/pecas", "/api/servicos"}) {
            mvc.perform(get(rota).param("busca", BUSCA_LONGA).header(HttpHeaders.AUTHORIZATION, token))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400));
        }
    }

    @Test
    void deveAceitarBuscaDeAteCemCaracteres() throws Exception {
        String token = bearer(Perfil.ADMINISTRADOR);
        mvc.perform(get("/api/clientes").param("busca", "x".repeat(100)).header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isOk());
    }
}
