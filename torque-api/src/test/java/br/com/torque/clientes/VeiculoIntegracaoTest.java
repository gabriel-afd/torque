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
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

class VeiculoIntegracaoTest extends IntegracaoTestBase {

    private String token;
    private long clienteId;

    @BeforeEach
    void prepararCliente() throws Exception {
        token = bearer(Perfil.ATENDENTE);
        String resposta = mvc.perform(post("/api/clientes").header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(paraJson(Map.of("nome", "Joana Fictícia", "documento", "111.444.777-35",
                                "telefone", "83988887777"))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        clienteId = json.readTree(resposta).get("id").asLong();
    }

    private ResultActions postVeiculo(long cliente, String placa) throws Exception {
        return mvc.perform(post("/api/clientes/" + cliente + "/veiculos").header(HttpHeaders.AUTHORIZATION, token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(paraJson(Map.of("placa", placa, "marca", "Volkswagen", "modelo", "Gol",
                        "ano", 2018, "cor", "Prata"))));
    }

    private int contarVeiculos() {
        return jdbc.queryForObject("SELECT count(*) FROM veiculo", Integer.class);
    }

    @Test
    void deveCadastrarPlacaMercosulNormalizada() throws Exception {
        postVeiculo(clienteId, "abc-1d23")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.placa").value("ABC1D23"))
                .andExpect(jsonPath("$.clienteId").value(clienteId))
                .andExpect(jsonPath("$.ano").value(2018));
        assertThat(jdbc.queryForObject("SELECT placa FROM veiculo", String.class)).isEqualTo("ABC1D23");
    }

    @Test
    void deveCadastrarPlacaNoFormatoAntigo() throws Exception {
        postVeiculo(clienteId, "ABC1234").andExpect(status().isCreated());
    }

    @Test
    void deveRecusarPlacaInvalidaCom400NoCampoPlaca() throws Exception {
        postVeiculo(clienteId, "AB-1234")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.placa").exists());
        postVeiculo(clienteId, "ABCD123")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.placa").exists());
        assertThat(contarVeiculos()).isZero();
    }

    @Test
    void deveRecusarPlacaDuplicadaCom409() throws Exception {
        postVeiculo(clienteId, "ABC1D23").andExpect(status().isCreated());

        postVeiculo(clienteId, "abc-1d23")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensagem").value(VeiculoService.MENSAGEM_PLACA_DUPLICADA));
        assertThat(contarVeiculos()).isEqualTo(1);
    }

    @Test
    void deveBuscarVeiculoPorPlacaComDadosDoDono() throws Exception {
        postVeiculo(clienteId, "ABC1D23").andExpect(status().isCreated());

        mvc.perform(get("/api/veiculos?placa=ABC1D23").header(HttpHeaders.AUTHORIZATION, bearer(Perfil.MECANICO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.placa").value("ABC1D23"))
                .andExpect(jsonPath("$.modelo").value("Gol"))
                .andExpect(jsonPath("$.dono.id").value(clienteId))
                .andExpect(jsonPath("$.dono.nome").value("Joana Fictícia"))
                .andExpect(jsonPath("$.dono.telefone").value("83988887777"));

        mvc.perform(get("/api/veiculos?placa=abc-1d23").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isOk());
    }

    @Test
    void deveDevolver404ParaPlacaInexistente() throws Exception {
        mvc.perform(get("/api/veiculos?placa=XYZ9Z99").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void deveDevolver404AoCadastrarVeiculoParaClienteInexistenteSemGravarNada() throws Exception {
        postVeiculo(999999, "ABC1D23")
                .andExpect(status().isNotFound());
        assertThat(contarVeiculos()).isZero();
    }

    @Test
    void deveRecusarVeiculoParaClienteInativoCom422() throws Exception {
        mvc.perform(patch("/api/clientes/" + clienteId + "/inativar").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isOk());

        postVeiculo(clienteId, "ABC1D23").andExpect(status().isUnprocessableEntity());
    }

    @Test
    void deveEditarVeiculoERecusarPlacaDeOutro() throws Exception {
        postVeiculo(clienteId, "ABC1D23").andExpect(status().isCreated());
        String resposta = postVeiculo(clienteId, "XYZ1234").andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long id = json.readTree(resposta).get("id").asLong();

        mvc.perform(put("/api/veiculos/" + id).header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"placa\":\"xyz-1234\",\"marca\":\"VW\",\"modelo\":\"Polo\",\"cor\":\"Azul\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.modelo").value("Polo"));

        mvc.perform(put("/api/veiculos/" + id).header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"placa\":\"ABC1D23\",\"marca\":\"VW\",\"modelo\":\"Polo\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void deveRecusarMecanicoNoCadastroEEdicaoDeVeiculoCom403() throws Exception {
        mvc.perform(post("/api/clientes/" + clienteId + "/veiculos")
                        .header(HttpHeaders.AUTHORIZATION, bearer(Perfil.MECANICO))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"placa\":\"ABC1D23\",\"marca\":\"VW\",\"modelo\":\"Gol\"}"))
                .andExpect(status().isForbidden());
    }
}
