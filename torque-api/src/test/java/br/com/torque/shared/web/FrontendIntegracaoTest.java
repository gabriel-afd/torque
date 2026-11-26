package br.com.torque.shared.web;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.torque.auth.Perfil;
import br.com.torque.shared.IntegracaoTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;

/**
 * Deploy em imagem única: a API serve também o build do Angular (copiado para {@code static/}).
 * Nos testes, {@code src/test/resources/static/index.html} faz o papel do build.
 */
class FrontendIntegracaoTest extends IntegracaoTestBase {

    private static final String MARCA_DO_INDEX = "frontend-de-teste";

    @Test
    void deveServirIndexHtmlSemLogin() throws Exception {
        mvc.perform(get("/index.html"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(MARCA_DO_INDEX)));
    }

    @Test
    void deveServirIndexHtmlNaRaiz() throws Exception {
        // Welcome page do Spring Boot: forward interno para index.html (o MockMvc não segue forwards).
        mvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(forwardedUrl("index.html"));
    }

    @Test
    void deveEncaminharRotaDoAngularParaIndexHtml() throws Exception {
        for (String rota : new String[] {"/login", "/quadro", "/ordens/17", "/veiculos/3/historico", "/admin/usuarios"}) {
            mvc.perform(get(rota))
                    .andExpect(status().isOk())
                    .andExpect(content().string(containsString(MARCA_DO_INDEX)));
        }
    }

    @Test
    void deveResponder404ParaArquivoEstaticoInexistente() throws Exception {
        mvc.perform(get("/main-inexistente.js"))
                .andExpect(status().isNotFound())
                .andExpect(content().string(not(containsString(MARCA_DO_INDEX))));
    }

    @Test
    void deveManterApiProtegidaSemToken() throws Exception {
        mvc.perform(get("/api/ordens")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/rota-inexistente")).andExpect(status().isUnauthorized());
    }

    @Test
    void naoDeveEncaminharRotaApiInexistenteParaIndexHtml() throws Exception {
        mvc.perform(get("/api/rota-inexistente").header(HttpHeaders.AUTHORIZATION, bearer(Perfil.ADMINISTRADOR)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(content().string(not(containsString(MARCA_DO_INDEX))));
    }

    @Test
    void naoDeveLiberarEscritaForaDaApiSemToken() throws Exception {
        mvc.perform(post("/quadro")).andExpect(status().isUnauthorized());
    }
}
