package br.com.torque.ordens;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.torque.auth.Perfil;
import com.fasterxml.jackson.databind.JsonNode;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

class OrdemServicoIntegracaoTest extends OrdemServicoApiTestBase {

    // CT27 / CT28 -------------------------------------------------------------

    @Test
    void deveAbrirOsComNumeroSequencialStatusAbertaEHistorico() throws Exception {
        JsonNode primeira = corpo(abrirOs(veiculo.getId(), tokenAtendente)
                .andExpect(status().isCreated())
                .andExpect(header().string(HttpHeaders.LOCATION, containsString("/api/ordens/")))
                .andExpect(jsonPath("$.status").value("ABERTA"))
                .andExpect(jsonPath("$.veiculo.placa").value("ABC1D23"))
                .andExpect(jsonPath("$.cliente.nome").value("Joana Fictícia"))
                .andExpect(jsonPath("$.quilometragem").value(85000))
                .andExpect(jsonPath("$.orcamento.versao").value(1))
                .andExpect(jsonPath("$.orcamento.situacao").value("PENDENTE"))
                .andExpect(jsonPath("$.orcamento.total").value(0.0))
                .andExpect(jsonPath("$.historico.length()").value(1))
                .andExpect(jsonPath("$.historico[0].statusNovo").value("ABERTA"))
                .andExpect(jsonPath("$.historico[0].statusAnterior").doesNotExist()));
        JsonNode segunda = corpo(abrirOs(veiculo.getId(), tokenAdmin).andExpect(status().isCreated()));

        assertThat(primeira.get("numero").asLong()).isGreaterThanOrEqualTo(1000);
        assertThat(segunda.get("numero").asLong()).isEqualTo(primeira.get("numero").asLong() + 1);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM historico_status", Integer.class)).isEqualTo(2);
    }

    @Test
    void deveDevolver404AoAbrirOsParaVeiculoInexistente() throws Exception {
        abrirOs(999_999L, tokenAtendente).andExpect(status().isNotFound());
        assertThat(jdbc.queryForObject("SELECT count(*) FROM ordem_servico", Integer.class)).isZero();
    }

    @Test
    void deveImpedirMecanicoDeAbrirOs() throws Exception {
        abrirOs(veiculo.getId(), tokenMecanico).andExpect(status().isForbidden());
    }

    @Test
    void deveValidarAberturaCom400() throws Exception {
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/ordens")
                        .header(HttpHeaders.AUTHORIZATION, tokenAtendente)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"problemaRelatado\":\"\",\"quilometragem\":-5}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.veiculoId").exists())
                .andExpect(jsonPath("$.campos.problemaRelatado").exists())
                .andExpect(jsonPath("$.campos.quilometragem").exists());
    }

    // Orçamento (CT21, CT24, CT25) ---------------------------------------------

    @Test
    void deveMontarOrcamentoComTotalCalculado() throws Exception {
        long os = abrirOs();

        putOrcamento(os, 1, orcamentoPadrao("19.00"), tokenAtendente)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orcamento.servicos[0].descricao").value("Troca de óleo"))
                .andExpect(jsonPath("$.orcamento.pecas[0].quantidade").value(2))
                .andExpect(jsonPath("$.orcamento.pecas[0].subtotal").value(70.0))
                .andExpect(jsonPath("$.orcamento.subtotalServicos").value(120.0))
                .andExpect(jsonPath("$.orcamento.subtotalPecas").value(70.0))
                .andExpect(jsonPath("$.orcamento.desconto").value(19.0))
                .andExpect(jsonPath("$.orcamento.total").value(171.0));
    }

    @Test
    void deveRecusarDescontoAcimaDeDezPorCentoParaAtendenteCom422SemAlterarOrcamento() throws Exception {
        long os = abrirOs();
        putOrcamento(os, 1, orcamentoPadrao("0"), tokenAtendente).andExpect(status().isOk());
        JsonNode antes = corpo(detalhar(os, tokenAtendente)).get("orcamento");

        putOrcamento(os, 1, Map.of("servicos", List.of(Map.of("itemId", antes.at("/servicos/0/id").asLong())),
                        "pecas", List.of(), "desconto", new BigDecimal("28.50")), tokenAtendente) // 15% de 190
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.mensagem").value(containsString("10%")));

        JsonNode depois = corpo(detalhar(os, tokenAtendente)).get("orcamento");
        assertThat(depois).isEqualTo(antes);
    }

    @Test
    void deveAceitarDescontoAcimaDeDezPorCentoParaAdministrador() throws Exception {
        long os = abrirOs();
        putOrcamento(os, 1, orcamentoPadrao("28.50"), tokenAdmin)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orcamento.total").value(161.5));
    }

    @Test
    void deveManterPrecoDoItemAoAlterarCatalogoDepois() throws Exception {
        long os = abrirOs();
        JsonNode orcamento = corpo(putOrcamento(os, 1, orcamentoPadrao("0"), tokenAtendente)).get("orcamento");

        mvc.perform(put("/api/servicos/" + trocaDeOleo.getId()).header(HttpHeaders.AUTHORIZATION, tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"descricao\":\"Troca de óleo sintético\",\"precoMaoDeObra\":300}"))
                .andExpect(status().isOk());

        detalhar(os, tokenAtendente)
                .andExpect(jsonPath("$.orcamento.servicos[0].descricao").value("Troca de óleo"))
                .andExpect(jsonPath("$.orcamento.servicos[0].preco").value(120.0))
                .andExpect(jsonPath("$.orcamento.total").value(190.0));

        // Reenviar o item existente (por itemId) também mantém o preço congelado.
        putOrcamento(os, 1, Map.of(
                        "servicos", List.of(Map.of("itemId", orcamento.at("/servicos/0/id").asLong())),
                        "pecas", List.of(Map.of("itemId", orcamento.at("/pecas/0/id").asLong(), "quantidade", 2)),
                        "desconto", 0), tokenAtendente)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orcamento.servicos[0].preco").value(120.0))
                .andExpect(jsonPath("$.orcamento.total").value(190.0));
    }

    @Test
    void deveRecusarQuantidadeDePecaZeroOuNegativaCom400() throws Exception {
        long os = abrirOs();
        for (int quantidade : new int[]{0, -1}) {
            putOrcamento(os, 1, Map.of("servicos", List.of(),
                            "pecas", List.of(Map.of("pecaId", filtro.getId(), "quantidade", quantidade)),
                            "desconto", 0), tokenAtendente)
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.campos['pecas[0].quantidade']").exists());
        }
        assertThat(jdbc.queryForObject("SELECT count(*) FROM orcamento_item_peca", Integer.class)).isZero();
    }

    @Test
    void deveRecusarItemComDuasReferenciasOuSemReferenciaCom400() throws Exception {
        long os = abrirOs();
        putOrcamento(os, 1, Map.of("servicos", List.of(Map.of("itemId", 1, "servicoId", trocaDeOleo.getId())),
                "pecas", List.of(), "desconto", 0), tokenAtendente)
                .andExpect(status().isBadRequest());
        putOrcamento(os, 1, Map.of("servicos", List.of(Map.of()), "pecas", List.of(), "desconto", 0), tokenAtendente)
                .andExpect(status().isBadRequest());
    }

    @Test
    void deveRecusarItemInativoDoCatalogoCom422() throws Exception {
        trocaDeOleo.inativar();
        servicos.save(trocaDeOleo);
        long os = abrirOs();
        putOrcamento(os, 1, orcamentoPadrao("0"), tokenAtendente).andExpect(status().isUnprocessableEntity());
    }

    @Test
    void deveDevolver404ParaItemInexistenteNoCatalogo() throws Exception {
        long os = abrirOs();
        putOrcamento(os, 1, Map.of("servicos", List.of(Map.of("servicoId", 999_999)), "pecas", List.of(),
                "desconto", 0), tokenAtendente)
                .andExpect(status().isNotFound());
    }

    @Test
    void deveImpedirMecanicoDeAlterarOrcamento() throws Exception {
        long os = abrirOs();
        putOrcamento(os, 1, orcamentoPadrao("0"), tokenMecanico).andExpect(status().isForbidden());
    }

    // Aprovação e recusa (CT29, CT31) --------------------------------------------

    @Test
    void deveAprovarOrcamentoRegistrandoDataEUsuario() throws Exception {
        long os = abrirOs();
        putOrcamento(os, 1, orcamentoPadrao("0"), tokenAtendente).andExpect(status().isOk());
        mudarStatus(os, "AGUARDANDO_APROVACAO", tokenAtendente).andExpect(status().isOk());

        decidir(os, 1, "aprovacao", tokenAtendente)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APROVADA"))
                .andExpect(jsonPath("$.orcamento.situacao").value("APROVADO"))
                .andExpect(jsonPath("$.orcamento.decididoPor").exists())
                .andExpect(jsonPath("$.orcamento.dataDecisao").exists());

        decidir(os, 1, "aprovacao", tokenAtendente).andExpect(status().isUnprocessableEntity());
    }

    @Test
    void deveRecusarOrcamentoEFinalizarOs() throws Exception {
        long os = abrirOs();
        putOrcamento(os, 1, orcamentoPadrao("0"), tokenAtendente).andExpect(status().isOk());
        mudarStatus(os, "AGUARDANDO_APROVACAO", tokenAtendente).andExpect(status().isOk());

        decidir(os, 1, "recusa", tokenAtendente)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RECUSADA"))
                .andExpect(jsonPath("$.orcamento.situacao").value("RECUSADO"));

        mudarStatus(os, "CANCELADA", tokenAdmin).andExpect(status().isUnprocessableEntity());
    }

    @Test
    void deveImpedirMecanicoDeRegistrarAprovacaoCom403() throws Exception {
        long os = abrirOs();
        decidir(os, 1, "aprovacao", tokenMecanico).andExpect(status().isForbidden());
    }

    @Test
    void deveRecusarAprovacaoPeloPatchDeStatusCom422() throws Exception {
        long os = abrirOs();
        putOrcamento(os, 1, orcamentoPadrao("0"), tokenAtendente).andExpect(status().isOk());
        mudarStatus(os, "AGUARDANDO_APROVACAO", tokenAtendente).andExpect(status().isOk());
        mudarStatus(os, "APROVADA", tokenAdmin).andExpect(status().isUnprocessableEntity());
    }

    // RN07 (CT32) -----------------------------------------------------------------

    @Test
    void deveCriarNovaVersaoAoAlterarOrcamentoAprovado() throws Exception {
        long os = osAprovadaComMecanico();
        JsonNode v1 = corpo(detalhar(os, tokenAtendente)).get("orcamento");

        putOrcamento(os, 1, Map.of(
                        "servicos", List.of(Map.of("itemId", v1.at("/servicos/0/id").asLong())),
                        "pecas", List.of(Map.of("itemId", v1.at("/pecas/0/id").asLong(), "quantidade", 3)),
                        "desconto", 0), tokenAtendente)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("AGUARDANDO_APROVACAO"))
                .andExpect(jsonPath("$.orcamento.versao").value(2))
                .andExpect(jsonPath("$.orcamento.situacao").value("PENDENTE"))
                .andExpect(jsonPath("$.orcamento.servicos[0].preco").value(120.0))
                .andExpect(jsonPath("$.orcamento.pecas[0].quantidade").value(3))
                .andExpect(jsonPath("$.orcamento.total").value(225.0))
                .andExpect(jsonPath("$.versoes[0].versao").value(1))
                .andExpect(jsonPath("$.versoes[0].situacao").value("SUBSTITUIDO"))
                .andExpect(jsonPath("$.versoes[0].total").value(190.0))
                .andExpect(jsonPath("$.versoes[1].situacao").value("PENDENTE"));

        // A versão 1 não é mais a vigente: alterar por ela agora é conflito.
        putOrcamento(os, 1, orcamentoPadrao("0"), tokenAtendente).andExpect(status().isConflict());
        decidir(os, 2, "aprovacao", tokenAtendente).andExpect(status().isOk());
    }

    @Test
    void naoDeveCriarNovaVersaoQuandoAlteracaoEhInvalida() throws Exception {
        long os = osAprovadaComMecanico();

        putOrcamento(os, 1, orcamentoPadrao("100.00"), tokenAtendente).andExpect(status().isUnprocessableEntity());

        detalhar(os, tokenAtendente)
                .andExpect(jsonPath("$.status").value("APROVADA"))
                .andExpect(jsonPath("$.orcamento.versao").value(1))
                .andExpect(jsonPath("$.orcamento.situacao").value("APROVADO"));
    }

    // RN09 (CT38, CT39) -------------------------------------------------------------

    @Test
    void deveDeixarMecanicoAtribuidoIniciarServico() throws Exception {
        long os = osAprovadaComMecanico();

        mudarStatus(os, "EM_EXECUCAO", tokenMecanico)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("EM_EXECUCAO"));
    }

    @Test
    void deveRecusarMecanicoNaoAtribuidoCom403SemAlterarStatus() throws Exception {
        long os = osAprovadaComMecanico();
        String outroMecanico = bearer(criarUsuario("Pedro Mecânico", "pedro@oficina.teste", Perfil.MECANICO));

        mudarStatus(os, "EM_EXECUCAO", outroMecanico)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));

        detalhar(os, tokenAtendente).andExpect(jsonPath("$.status").value("APROVADA"));
    }

    @Test
    void deveRecusarInicioSemOrcamentoAprovadoCom422() throws Exception {
        long os = abrirOs();
        mudarStatus(os, "EM_EXECUCAO", tokenAdmin).andExpect(status().isUnprocessableEntity());
    }

    @Test
    void deveDeixarSoAdministradorAtribuirMecanico() throws Exception {
        long os = abrirOs();
        atribuir(os, mecanico, tokenAtendente).andExpect(status().isForbidden());
        atribuir(os, criarUsuario(Perfil.ATENDENTE), tokenAdmin).andExpect(status().isUnprocessableEntity());
        atribuir(os, mecanico, tokenAdmin)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mecanico.id").value(mecanico.getId()))
                .andExpect(jsonPath("$.mecanico.nome").value("Mário Mecânico"));
    }

    // CT40 ----------------------------------------------------------------------

    @Test
    void deveGravarHistoricoDeCadaTransicao() throws Exception {
        long os = osAprovadaComMecanico();          // ABERTA → AGUARDANDO_APROVACAO → APROVADA
        mudarStatus(os, "EM_EXECUCAO", tokenMecanico).andExpect(status().isOk());

        detalhar(os, tokenAtendente)
                .andExpect(jsonPath("$.historico.length()").value(4))
                .andExpect(jsonPath("$.historico[3].statusAnterior").value("APROVADA"))
                .andExpect(jsonPath("$.historico[3].statusNovo").value("EM_EXECUCAO"))
                .andExpect(jsonPath("$.historico[3].usuarioId").value(mecanico.getId()))
                .andExpect(jsonPath("$.historico[3].dataHora").exists());

        List<Map<String, Object>> linhas = jdbc.queryForList(
                "SELECT usuario_id, status_anterior, status_novo, data_hora FROM historico_status "
                        + "WHERE ordem_servico_id = ? AND status_anterior IS NOT NULL ORDER BY id", os);
        assertThat(linhas).hasSize(3);
        assertThat(linhas).allSatisfy(l -> {
            assertThat(l.get("usuario_id")).isNotNull();
            assertThat(l.get("data_hora")).isNotNull();
        });
        assertThat(linhas).extracting(l -> l.get("status_novo"))
                .containsExactly("AGUARDANDO_APROVACAO", "APROVADA", "EM_EXECUCAO");
    }

    // CT41 ----------------------------------------------------------------------

    @Test
    void deveRecusarSegundaAtualizacaoComAMesmaVersaoDaOsCom409() throws Exception {
        long os = abrirOs();
        int versaoLida = corpo(detalhar(os, tokenAtendente)).get("version").asInt();
        Map<String, Object> corpoComVersao = new java.util.HashMap<>(orcamentoPadrao("0"));
        corpoComVersao.put("versaoOs", versaoLida);

        JsonNode primeira = corpo(putOrcamento(os, 1, corpoComVersao, tokenAtendente).andExpect(status().isOk()));
        assertThat(primeira.get("version").asInt()).isGreaterThan(versaoLida);

        putOrcamento(os, 1, corpoComVersao, tokenAdmin)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensagem").value(containsString("Recarregue")));

        mvc.perform(patch("/api/ordens/" + os + "/status").header(HttpHeaders.AUTHORIZATION, tokenAtendente)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(paraJson(Map.of("status", "CANCELADA", "versaoOs", versaoLida))))
                .andExpect(status().isConflict());
        detalhar(os, tokenAtendente).andExpect(jsonPath("$.status").value("ABERTA"));
    }

    @Test
    void deveDevolver404ParaOsInexistente() throws Exception {
        detalhar(999_999, tokenAtendente).andExpect(status().isNotFound());
        mudarStatus(999_999, "CANCELADA", tokenAtendente).andExpect(status().isNotFound());
    }

    @Test
    void deveDeixarMecanicoVerDetalheDaOs() throws Exception {
        long os = abrirOs();
        detalhar(os, tokenMecanico).andExpect(status().isOk());
    }

    @Test
    void deveRecusarStatusInvalidoNoCorpoCom400() throws Exception {
        long os = abrirOs();
        mudarStatus(os, "VOANDO", tokenAtendente).andExpect(status().isBadRequest());
    }

    @Test
    void deveExigirAutenticacao() throws Exception {
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/ordens"))
                .andExpect(status().isUnauthorized());
    }
}
