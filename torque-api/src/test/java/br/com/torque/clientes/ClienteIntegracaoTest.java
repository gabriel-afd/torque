package br.com.torque.clientes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.torque.auth.Perfil;
import br.com.torque.shared.IntegracaoTestBase;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

/** Dados fictícios: CPFs/CNPJs gerados só para teste. */
class ClienteIntegracaoTest extends IntegracaoTestBase {

    private String token;

    @BeforeEach
    void logarAtendente() {
        token = bearer(Perfil.ATENDENTE);
    }

    private static Map<String, Object> cliente(String nome, String documento) {
        Map<String, Object> corpo = new HashMap<>();
        corpo.put("nome", nome);
        corpo.put("documento", documento);
        corpo.put("telefone", "(83) 99999-0001");
        corpo.put("email", "cliente@exemplo.teste");
        return corpo;
    }

    private ResultActions postCliente(Map<String, Object> corpo) throws Exception {
        return mvc.perform(post("/api/clientes").header(HttpHeaders.AUTHORIZATION, token)
                .contentType(MediaType.APPLICATION_JSON).content(paraJson(corpo)));
    }

    private long criarCliente(String nome, String documento) throws Exception {
        String resposta = postCliente(cliente(nome, documento)).andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return json.readTree(resposta).get("id").asLong();
    }

    private int contarClientes() {
        return jdbc.queryForObject("SELECT count(*) FROM cliente", Integer.class);
    }

    @Test
    void deveCadastrarClienteComCpfValidoEEncontrarNaBusca() throws Exception {
        postCliente(cliente("João da Silva Fictício", "529.982.247-25"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.documento").value("52998224725"))
                .andExpect(jsonPath("$.telefone").value("83999990001"))
                .andExpect(jsonPath("$.ativo").value(true));

        mvc.perform(get("/api/clientes?busca=joão da").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].nome").value("João da Silva Fictício"));
        mvc.perform(get("/api/clientes?busca=529.982").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(jsonPath("$.content.length()").value(1));
        mvc.perform(get("/api/clientes?busca=inexistente").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(jsonPath("$.content.length()").value(0));
    }

    @Test
    void deveCadastrarClienteComCnpjValido() throws Exception {
        postCliente(cliente("Transportes Fictícios Ltda", "11.222.333/0001-81"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.documento").value("11222333000181"));
    }

    @Test
    void deveRecusarCpfComDigitoInvalidoCom400NoCampoDocumento() throws Exception {
        postCliente(cliente("Maria", "111.111.111-12"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.documento").value("CPF ou CNPJ inválido"));
        assertThat(contarClientes()).isZero();
    }

    @Test
    void deveRecusarCpfDuplicadoCom409SemGravarDuplicado() throws Exception {
        criarCliente("João", "529.982.247-25");

        postCliente(cliente("Outro João", "52998224725"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.mensagem").value(ClienteService.MENSAGEM_DOCUMENTO_DUPLICADO));
        assertThat(contarClientes()).isEqualTo(1);
    }

    @Test
    void deveRecusarDocumentoDeOutroClienteNaEdicaoCom409() throws Exception {
        criarCliente("João", "529.982.247-25");
        long maria = criarCliente("Maria", "111.444.777-35");

        mvc.perform(put("/api/clientes/" + maria).header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON).content(paraJson(cliente("Maria", "529.982.247-25"))))
                .andExpect(status().isConflict());
    }

    @Test
    void deveEditarEInativarCliente() throws Exception {
        long id = criarCliente("João", "529.982.247-25");
        Map<String, Object> alterado = cliente("João Souza", "529.982.247-25");
        alterado.put("email", null);

        mvc.perform(put("/api/clientes/" + id).header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON).content(paraJson(alterado)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("João Souza"))
                .andExpect(jsonPath("$.email").doesNotExist());

        mvc.perform(patch("/api/clientes/" + id + "/inativar").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ativo").value(false));
    }

    @Test
    void deveDetalharClienteComVeiculos() throws Exception {
        long id = criarCliente("João", "529.982.247-25");
        mvc.perform(post("/api/clientes/" + id + "/veiculos").header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"placa\":\"ABC1D23\",\"marca\":\"Fiat\",\"modelo\":\"Uno\"}"))
                .andExpect(status().isCreated());

        mvc.perform(get("/api/clientes/" + id).header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.veiculos.length()").value(1))
                .andExpect(jsonPath("$.veiculos[0].placa").value("ABC1D23"));
    }

    @Test
    void deveValidarTelefoneENomeObrigatorios() throws Exception {
        Map<String, Object> corpo = cliente("", "529.982.247-25");
        corpo.put("telefone", "123");
        postCliente(corpo)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.nome").exists())
                .andExpect(jsonPath("$.campos.telefone").exists());
    }

    @Test
    void deveRecusarMecanicoNoCadastroDeClienteCom403() throws Exception {
        mvc.perform(post("/api/clientes").header(HttpHeaders.AUTHORIZATION, bearer(Perfil.MECANICO))
                        .contentType(MediaType.APPLICATION_JSON).content(paraJson(cliente("João", "529.982.247-25"))))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/clientes").header(HttpHeaders.AUTHORIZATION, bearer(Perfil.MECANICO)))
                .andExpect(status().isForbidden());
    }

    @Test
    void deveDevolver404ParaClienteInexistente() throws Exception {
        mvc.perform(get("/api/clientes/999999").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isNotFound());
    }
}
