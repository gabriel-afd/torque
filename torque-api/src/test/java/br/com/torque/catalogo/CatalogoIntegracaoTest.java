package br.com.torque.catalogo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.torque.auth.Perfil;
import br.com.torque.shared.IntegracaoTestBase;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

class CatalogoIntegracaoTest extends IntegracaoTestBase {

    private String admin;

    @BeforeEach
    void logarAdministrador() {
        admin = bearer(Perfil.ADMINISTRADOR);
    }

    private ResultActions enviar(String metodo, String url, String token, String corpo) throws Exception {
        var requisicao = switch (metodo) {
            case "POST" -> post(url);
            case "PUT" -> put(url);
            default -> throw new IllegalArgumentException(metodo);
        };
        return mvc.perform(requisicao.header(HttpHeaders.AUTHORIZATION, token)
                .contentType(MediaType.APPLICATION_JSON).content(corpo));
    }

    private long criar(String url, String corpo) throws Exception {
        String resposta = enviar("POST", url, admin, corpo).andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return json.readTree(resposta).get("id").asLong();
    }

    // Serviços --------------------------------------------------------------

    @Test
    void deveFazerCrudDeServicoComoAdministrador() throws Exception {
        long id = criar("/api/servicos", "{\"descricao\":\"Troca de óleo\",\"precoMaoDeObra\":80}");

        mvc.perform(get("/api/servicos/" + id).header(HttpHeaders.AUTHORIZATION, bearer(Perfil.MECANICO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.descricao").value("Troca de óleo"))
                .andExpect(jsonPath("$.precoMaoDeObra").value(80.00));
        assertThat(jdbc.queryForObject("SELECT preco_mao_de_obra FROM servico", BigDecimal.class))
                .isEqualByComparingTo("80.00");

        enviar("PUT", "/api/servicos/" + id, admin, "{\"descricao\":\"Troca de óleo e filtro\",\"precoMaoDeObra\":95.5}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.precoMaoDeObra").value(95.50));

        mvc.perform(patch("/api/servicos/" + id + "/inativar").header(HttpHeaders.AUTHORIZATION, admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ativo").value(false));
    }

    @Test
    void deveRecusarAtendenteCriandoServicoCom403() throws Exception {
        enviar("POST", "/api/servicos", bearer(Perfil.ATENDENTE), "{\"descricao\":\"Alinhamento\",\"precoMaoDeObra\":60}")
                .andExpect(status().isForbidden());
        assertThat(jdbc.queryForObject("SELECT count(*) FROM servico", Integer.class)).isZero();
    }

    @Test
    void deveRecusarMecanicoEditandoOuInativandoCom403() throws Exception {
        long id = criar("/api/servicos", "{\"descricao\":\"Alinhamento\",\"precoMaoDeObra\":60}");
        String mecanico = bearer(Perfil.MECANICO);
        enviar("PUT", "/api/servicos/" + id, mecanico, "{\"descricao\":\"X\",\"precoMaoDeObra\":1}")
                .andExpect(status().isForbidden());
        mvc.perform(patch("/api/servicos/" + id + "/inativar").header(HttpHeaders.AUTHORIZATION, mecanico))
                .andExpect(status().isForbidden());
    }

    @Test
    void deveRecusarPrecoNegativoCom400() throws Exception {
        enviar("POST", "/api/servicos", admin, "{\"descricao\":\"Alinhamento\",\"precoMaoDeObra\":-1}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.precoMaoDeObra").exists());
        enviar("POST", "/api/pecas", admin, "{\"codigo\":\"F-1\",\"descricao\":\"Filtro\",\"precoVenda\":-0.01}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.precoVenda").exists());
    }

    @Test
    void deveRecusarPrecoComMaisDeDuasCasasDecimaisCom400() throws Exception {
        enviar("POST", "/api/servicos", admin, "{\"descricao\":\"Alinhamento\",\"precoMaoDeObra\":10.555}")
                .andExpect(status().isBadRequest());
    }

    @Test
    void deveListarSoAtivosPorPadraoEInativosSoParaAdministrador() throws Exception {
        criar("/api/servicos", "{\"descricao\":\"Alinhamento\",\"precoMaoDeObra\":60}");
        long inativo = criar("/api/servicos", "{\"descricao\":\"Balanceamento\",\"precoMaoDeObra\":40}");
        mvc.perform(patch("/api/servicos/" + inativo + "/inativar").header(HttpHeaders.AUTHORIZATION, admin))
                .andExpect(status().isOk());

        mvc.perform(get("/api/servicos").header(HttpHeaders.AUTHORIZATION, admin))
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].descricao").value("Alinhamento"));
        mvc.perform(get("/api/servicos?incluirInativos=true").header(HttpHeaders.AUTHORIZATION, admin))
                .andExpect(jsonPath("$.content.length()").value(2));
        mvc.perform(get("/api/servicos?incluirInativos=true").header(HttpHeaders.AUTHORIZATION, bearer(Perfil.ATENDENTE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1));
    }

    @Test
    void deveBuscarServicoPorDescricaoPaginado() throws Exception {
        criar("/api/servicos", "{\"descricao\":\"Troca de óleo\",\"precoMaoDeObra\":80}");
        criar("/api/servicos", "{\"descricao\":\"Troca de pastilhas\",\"precoMaoDeObra\":120}");
        criar("/api/servicos", "{\"descricao\":\"Alinhamento\",\"precoMaoDeObra\":60}");

        mvc.perform(get("/api/servicos?busca=TROCA&size=1").header(HttpHeaders.AUTHORIZATION, bearer(Perfil.ATENDENTE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.page.totalElements").value(2));
    }

    // Peças -----------------------------------------------------------------

    @Test
    void deveFazerCrudDePecaComoAdministrador() throws Exception {
        long id = criar("/api/pecas", "{\"codigo\":\" flt-001 \",\"descricao\":\"Filtro de óleo\",\"precoVenda\":35}");

        mvc.perform(get("/api/pecas?busca=flt").header(HttpHeaders.AUTHORIZATION, bearer(Perfil.MECANICO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].codigo").value("FLT-001"))
                .andExpect(jsonPath("$.content[0].precoVenda").value(35.00));

        enviar("PUT", "/api/pecas/" + id, admin, "{\"codigo\":\"FLT-001\",\"descricao\":\"Filtro de óleo premium\",\"precoVenda\":42.9}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.descricao").value("Filtro de óleo premium"));

        mvc.perform(patch("/api/pecas/" + id + "/inativar").header(HttpHeaders.AUTHORIZATION, admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ativo").value(false));
        mvc.perform(get("/api/pecas").header(HttpHeaders.AUTHORIZATION, admin))
                .andExpect(jsonPath("$.content.length()").value(0));
    }

    @Test
    void deveRecusarCodigoDePecaDuplicadoCom409() throws Exception {
        criar("/api/pecas", "{\"codigo\":\"FLT-001\",\"descricao\":\"Filtro de óleo\",\"precoVenda\":35}");

        enviar("POST", "/api/pecas", admin, "{\"codigo\":\"flt-001\",\"descricao\":\"Outro filtro\",\"precoVenda\":30}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensagem").value(CatalogoService.MENSAGEM_CODIGO_DUPLICADO));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM peca", Integer.class)).isEqualTo(1);
    }

    @Test
    void deveRecusarCodigoDeOutraPecaNaEdicaoCom409() throws Exception {
        criar("/api/pecas", "{\"codigo\":\"FLT-001\",\"descricao\":\"Filtro de óleo\",\"precoVenda\":35}");
        long id = criar("/api/pecas", "{\"codigo\":\"FLT-002\",\"descricao\":\"Filtro de ar\",\"precoVenda\":45}");

        enviar("PUT", "/api/pecas/" + id, admin, "{\"codigo\":\"FLT-001\",\"descricao\":\"Filtro de ar\",\"precoVenda\":45}")
                .andExpect(status().isConflict());
    }

    @Test
    void deveRecusarAtendenteCriandoPecaCom403() throws Exception {
        enviar("POST", "/api/pecas", bearer(Perfil.ATENDENTE), "{\"codigo\":\"F-1\",\"descricao\":\"Filtro\",\"precoVenda\":10}")
                .andExpect(status().isForbidden());
    }

    @Test
    void deveDevolver404ParaItemInexistente() throws Exception {
        mvc.perform(get("/api/pecas/999999").header(HttpHeaders.AUTHORIZATION, admin)).andExpect(status().isNotFound());
        mvc.perform(get("/api/servicos/999999").header(HttpHeaders.AUTHORIZATION, admin)).andExpect(status().isNotFound());
    }
}
