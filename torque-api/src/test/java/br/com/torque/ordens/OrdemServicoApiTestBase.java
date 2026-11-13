package br.com.torque.ordens;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.torque.auth.Perfil;
import br.com.torque.auth.Usuario;
import br.com.torque.catalogo.Peca;
import br.com.torque.catalogo.PecaRepository;
import br.com.torque.catalogo.Servico;
import br.com.torque.catalogo.ServicoRepository;
import br.com.torque.clientes.Cliente;
import br.com.torque.clientes.ClienteRepository;
import br.com.torque.clientes.Veiculo;
import br.com.torque.clientes.VeiculoRepository;
import br.com.torque.shared.IntegracaoTestBase;
import com.fasterxml.jackson.databind.JsonNode;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

/** Cenário comum dos testes de API da OS: usuários, cliente, veículo e catálogo fictícios. */
abstract class OrdemServicoApiTestBase extends IntegracaoTestBase {

    @Autowired
    protected ClienteRepository clientes;
    @Autowired
    protected VeiculoRepository veiculos;
    @Autowired
    protected ServicoRepository servicos;
    @Autowired
    protected PecaRepository pecas;

    protected Usuario mecanico;
    protected String tokenAtendente;
    protected String tokenAdmin;
    protected String tokenMecanico;
    protected Veiculo veiculo;
    protected Servico trocaDeOleo;
    protected Peca filtro;

    @BeforeEach
    void prepararCenario() {
        tokenAtendente = bearer(Perfil.ATENDENTE);
        tokenAdmin = bearer(Perfil.ADMINISTRADOR);
        mecanico = criarUsuario("Mário Mecânico", "mario@oficina.teste", Perfil.MECANICO);
        tokenMecanico = bearer(mecanico);
        Cliente cliente = clientes.save(new Cliente("Joana Fictícia", "52998224725", "83999990001", null));
        veiculo = veiculos.save(cliente.novoVeiculo("ABC1D23", "Fiat", "Uno", 2015, "Branco"));
        trocaDeOleo = servicos.save(new Servico("Troca de óleo", new BigDecimal("120.00")));
        filtro = pecas.save(new Peca("FLT-1", "Filtro de óleo", new BigDecimal("35.00")));
    }

    protected JsonNode corpo(ResultActions resultado) throws Exception {
        return json.readTree(resultado.andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8));
    }

    protected ResultActions abrirOs(Long veiculoId, String token) throws Exception {
        return mvc.perform(post("/api/ordens").header(HttpHeaders.AUTHORIZATION, token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(paraJson(Map.of("veiculoId", veiculoId, "problemaRelatado", "Barulho na suspensão",
                        "quilometragem", 85000))));
    }

    protected long abrirOs() throws Exception {
        return corpo(abrirOs(veiculo.getId(), tokenAtendente).andExpect(status().isCreated())).get("id").asLong();
    }

    protected ResultActions putOrcamento(long os, int versao, Object corpo, String token) throws Exception {
        return mvc.perform(put("/api/ordens/" + os + "/orcamentos/" + versao).header(HttpHeaders.AUTHORIZATION, token)
                .contentType(MediaType.APPLICATION_JSON).content(paraJson(corpo)));
    }

    /** Orçamento com 1 troca de óleo (120) + 2 filtros (35): R$ 190,00. */
    protected Map<String, Object> orcamentoPadrao(String desconto) {
        return Map.of(
                "servicos", List.of(Map.of("servicoId", trocaDeOleo.getId())),
                "pecas", List.of(Map.of("pecaId", filtro.getId(), "quantidade", 2)),
                "desconto", new BigDecimal(desconto));
    }

    protected ResultActions decidir(long os, int versao, String decisao, String token) throws Exception {
        return mvc.perform(post("/api/ordens/" + os + "/orcamentos/" + versao + "/" + decisao)
                .header(HttpHeaders.AUTHORIZATION, token));
    }

    protected ResultActions mudarStatus(long os, String status, String token) throws Exception {
        return mvc.perform(patch("/api/ordens/" + os + "/status").header(HttpHeaders.AUTHORIZATION, token)
                .contentType(MediaType.APPLICATION_JSON).content(paraJson(Map.of("status", status))));
    }

    protected ResultActions atribuir(long os, Usuario mecanico, String token) throws Exception {
        return mvc.perform(patch("/api/ordens/" + os + "/mecanico").header(HttpHeaders.AUTHORIZATION, token)
                .contentType(MediaType.APPLICATION_JSON).content(paraJson(Map.of("mecanicoId", mecanico.getId()))));
    }

    protected ResultActions detalhar(long os, String token) throws Exception {
        return mvc.perform(get("/api/ordens/" + os).header(HttpHeaders.AUTHORIZATION, token));
    }

    /** OS com orçamento padrão aprovado e mecânico atribuído. */
    protected long osAprovadaComMecanico() throws Exception {
        long os = abrirOs();
        putOrcamento(os, 1, orcamentoPadrao("0"), tokenAtendente).andExpect(status().isOk());
        mudarStatus(os, "AGUARDANDO_APROVACAO", tokenAtendente).andExpect(status().isOk());
        decidir(os, 1, "aprovacao", tokenAtendente).andExpect(status().isOk());
        atribuir(os, mecanico, tokenAdmin).andExpect(status().isOk());
        return os;
    }
}
