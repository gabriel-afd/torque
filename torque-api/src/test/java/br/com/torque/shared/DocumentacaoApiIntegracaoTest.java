package br.com.torque.shared;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

/** O Swagger (/api/docs) precisa listar todos os endpoints da tabela do CLAUDE.md com os DTOs certos. */
class DocumentacaoApiIntegracaoTest extends IntegracaoTestBase {

    @Test
    void deveDocumentarTodosOsEndpointsComSeusDtos() throws Exception {
        JsonNode docs = json.readTree(mvc.perform(get("/v3/api-docs")).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());
        JsonNode paths = docs.get("paths");

        String[][] esperados = {
                {"/api/auth/login", "post"},
                {"/api/usuarios", "get"}, {"/api/usuarios", "post"}, {"/api/usuarios/{id}", "put"},
                {"/api/usuarios/{id}/inativar", "patch"},
                {"/api/clientes", "get"}, {"/api/clientes", "post"}, {"/api/clientes/{id}", "put"},
                {"/api/clientes/{id}/inativar", "patch"}, {"/api/clientes/{id}/veiculos", "post"},
                {"/api/veiculos", "get"}, {"/api/veiculos/{id}", "put"}, {"/api/veiculos/{id}/ordens", "get"},
                {"/api/servicos", "get"}, {"/api/servicos", "post"}, {"/api/pecas", "get"}, {"/api/pecas", "post"},
                {"/api/ordens", "get"}, {"/api/ordens", "post"}, {"/api/ordens/{id}", "get"},
                {"/api/ordens/{id}/orcamentos/{versao}", "put"},
                {"/api/ordens/{id}/orcamentos/{versao}/aprovacao", "post"},
                {"/api/ordens/{id}/orcamentos/{versao}/recusa", "post"},
                {"/api/ordens/{id}/status", "patch"}, {"/api/ordens/{id}/mecanico", "patch"},
        };
        for (String[] e : esperados) {
            assertThat(paths.has(e[0])).as("rota %s", e[0]).isTrue();
            assertThat(paths.get(e[0]).has(e[1])).as("%s %s", e[1], e[0]).isTrue();
        }

        assertThat(ref(paths, "/api/ordens/{id}/orcamentos/{versao}", "put", true)).endsWith("OrcamentoRequest");
        assertThat(ref(paths, "/api/ordens/{id}/orcamentos/{versao}", "put", false)).endsWith("OrdemServicoResponse");
        assertThat(ref(paths, "/api/ordens", "post", true)).endsWith("AberturaOsRequest");
        assertThat(ref(paths, "/api/ordens/{id}/status", "patch", true)).endsWith("StatusRequest");
        assertThat(ref(paths, "/api/ordens/{id}/mecanico", "patch", true)).endsWith("MecanicoRequest");
        assertThat(ref(paths, "/api/auth/login", "post", false)).endsWith("LoginResponse");

        JsonNode schemas = docs.at("/components/schemas");
        assertThat(schemas.has("UsuarioResponse")).isTrue();
        assertThat(schemas.at("/UsuarioResponse/properties").has("senha")).isFalse();
        assertThat(schemas.at("/ItemServico/properties").has("referenciaUnica")).isFalse();
        assertThat(docs.at("/components/securitySchemes/bearer/scheme").asText()).isEqualTo("bearer");
    }

    private static String ref(JsonNode paths, String rota, String metodo, boolean entrada) {
        JsonNode operacao = paths.get(rota).get(metodo);
        JsonNode schema = entrada
                ? operacao.at("/requestBody/content/application~1json/schema")
                : operacao.at("/responses").elements().next().at("/content/*~1*/schema");
        return schema.path("$ref").asText();
    }
}
