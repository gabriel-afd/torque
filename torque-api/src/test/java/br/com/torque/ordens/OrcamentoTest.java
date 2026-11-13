package br.com.torque.ordens;

import static br.com.torque.ordens.Fixtures.administrador;
import static br.com.torque.ordens.Fixtures.atendente;
import static br.com.torque.ordens.Fixtures.mecanico;
import static br.com.torque.ordens.Fixtures.osAberta;
import static br.com.torque.ordens.Fixtures.peca;
import static br.com.torque.ordens.Fixtures.servico;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.torque.catalogo.Peca;
import br.com.torque.catalogo.Servico;
import br.com.torque.ordens.Orcamento.Alteracao;
import br.com.torque.ordens.Orcamento.PecaNova;
import br.com.torque.shared.excecao.OperacaoNaoPermitidaException;
import br.com.torque.shared.excecao.RegraDeNegocioException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class OrcamentoTest {

    private Orcamento orcamento;

    @BeforeEach
    void criarOrcamento() {
        orcamento = osAberta().getOrcamentoVigente();
    }

    /** Itens que somam R$ 390,00: 1 serviço de 250 + 2 peças de 70. */
    private void itensSomando390() {
        orcamento.adicionarServico(servico("Revisão completa", "250.00"));
        orcamento.adicionarPeca(peca("Pastilha de freio", "70.00"), 2);
    }

    // RN04 ------------------------------------------------------------------

    @Test
    void deveCalcularTotalComoServicosMaisQuantidadeVezesPrecoDasPecas() {
        orcamento.adicionarServico(servico("Troca de óleo", "120.00"));
        orcamento.adicionarPeca(peca("Filtro de óleo", "35.00"), 2);

        assertThat(orcamento.getSubtotalServicos()).isEqualByComparingTo("120.00");
        assertThat(orcamento.getSubtotalPecas()).isEqualByComparingTo("70.00");
        assertThat(orcamento.calcularTotal()).isEqualByComparingTo("190.00");
        assertThat(orcamento.getTotal()).isEqualByComparingTo("190.00");
    }

    @Test
    void deveSubtrairDescontoDoTotal() {
        itensSomando390();

        orcamento.aplicarDesconto(new BigDecimal("19.50"), atendente());

        assertThat(orcamento.getSubtotal()).isEqualByComparingTo("390.00");
        assertThat(orcamento.getTotal()).isEqualByComparingTo("370.50");
    }

    @Test
    void deveManterTotalComEscalaDois() {
        orcamento.adicionarPeca(peca("Parafuso", "0.33"), 3);
        assertThat(orcamento.getTotal()).hasToString("0.99");
    }

    @Test
    void deveRecalcularTotalAoRemoverItemEAlterarQuantidade() {
        orcamento.adicionarServico(servico("Troca de óleo", "120.00"));
        OrcamentoItemPeca filtro = orcamento.adicionarPeca(peca("Filtro", "35.00"), 2);

        orcamento.alterarQuantidade(filtro, 4);
        assertThat(orcamento.getTotal()).isEqualByComparingTo("260.00");

        orcamento.removerPeca(filtro);
        assertThat(orcamento.getTotal()).isEqualByComparingTo("120.00");
        assertThat(orcamento.getItensPeca()).isEmpty();
    }

    // RN05 ------------------------------------------------------------------

    @Test
    void deveRecusarDescontoAcimaDeDezPorCentoParaAtendenteSemAlterarOrcamento() {
        itensSomando390();

        assertThatThrownBy(() -> orcamento.aplicarDesconto(new BigDecimal("58.50"), atendente())) // 15%
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("10%");

        assertThat(orcamento.getDesconto()).isEqualByComparingTo("0.00");
        assertThat(orcamento.getTotal()).isEqualByComparingTo("390.00");
    }

    @Test
    void deveAceitarDescontoDeExatamenteDezPorCentoParaAtendente() {
        itensSomando390();

        orcamento.aplicarDesconto(new BigDecimal("39.00"), atendente());

        assertThat(orcamento.getTotal()).isEqualByComparingTo("351.00");
    }

    @Test
    void deveRecusarDescontoUmCentavoAcimaDosDezPorCentoParaAtendente() {
        itensSomando390();
        assertThatThrownBy(() -> orcamento.aplicarDesconto(new BigDecimal("39.01"), atendente()))
                .isInstanceOf(RegraDeNegocioException.class);
    }

    @Test
    void deveAceitarDescontoAcimaDeDezPorCentoParaAdministrador() {
        itensSomando390();

        orcamento.aplicarDesconto(new BigDecimal("58.50"), administrador());

        assertThat(orcamento.getTotal()).isEqualByComparingTo("331.50");
    }

    @Test
    void deveRecusarDescontoMaiorQueOValorDosItensMesmoParaAdministrador() {
        itensSomando390();
        assertThatThrownBy(() -> orcamento.aplicarDesconto(new BigDecimal("390.01"), administrador()))
                .isInstanceOf(RegraDeNegocioException.class);
    }

    @Test
    void deveRecusarDescontoNegativo() {
        itensSomando390();
        assertThatThrownBy(() -> orcamento.aplicarDesconto(new BigDecimal("-1"), administrador()))
                .isInstanceOf(RegraDeNegocioException.class);
    }

    @Test
    void deveImpedirMecanicoDeConcederDesconto() {
        itensSomando390();
        assertThatThrownBy(() -> orcamento.aplicarDesconto(BigDecimal.ONE, mecanico()))
                .isInstanceOf(OperacaoNaoPermitidaException.class);
    }

    @Test
    void deveImpedirRemocaoQueDeixariaDescontoMaiorQueOsItens() {
        OrcamentoItemServico caro = orcamento.adicionarServico(servico("Motor", "1000.00"));
        orcamento.adicionarServico(servico("Lavagem", "50.00"));
        orcamento.aplicarDesconto(new BigDecimal("500.00"), administrador());

        assertThatThrownBy(() -> orcamento.removerServico(caro)).isInstanceOf(RegraDeNegocioException.class);
        assertThat(orcamento.getItensServico()).hasSize(2);
    }

    // RN10 ------------------------------------------------------------------

    @Test
    void deveCopiarDescricaoEPrecoDoCatalogoNaInclusao() {
        Servico servico = servico("Alinhamento", "60.00");
        Peca peca = peca("Filtro de ar", "45.00");

        OrcamentoItemServico itemServico = orcamento.adicionarServico(servico);
        OrcamentoItemPeca itemPeca = orcamento.adicionarPeca(peca, 1);
        servico.atualizar("Alinhamento 3D", new BigDecimal("90.00"));
        peca.atualizar(peca.getCodigo(), "Filtro de ar esportivo", new BigDecimal("80.00"));

        assertThat(itemServico.getDescricao()).isEqualTo("Alinhamento");
        assertThat(itemServico.getPreco()).isEqualByComparingTo("60.00");
        assertThat(itemPeca.getDescricao()).isEqualTo("Filtro de ar");
        assertThat(itemPeca.getPrecoUnitario()).isEqualByComparingTo("45.00");
        assertThat(orcamento.getTotal()).isEqualByComparingTo("105.00");
    }

    // CT26 e quantidade -----------------------------------------------------

    @Test
    void deveRecusarServicoInativoDoCatalogo() {
        Servico inativo = servico("Polimento", "100.00");
        inativo.inativar();
        assertThatThrownBy(() -> orcamento.adicionarServico(inativo))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("inativo");
        assertThat(orcamento.getItensServico()).isEmpty();
    }

    @Test
    void deveRecusarPecaInativaDoCatalogo() {
        Peca inativa = peca("Lâmpada", "15.00");
        inativa.inativar();
        assertThatThrownBy(() -> orcamento.adicionarPeca(inativa, 1))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("inativa");
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1, -10})
    void deveRecusarQuantidadeDePecaMenorOuIgualAZero(int quantidade) {
        assertThatThrownBy(() -> orcamento.adicionarPeca(peca("Filtro", "35.00"), quantidade))
                .isInstanceOf(RegraDeNegocioException.class);
        assertThat(orcamento.getItensPeca()).isEmpty();
    }

    // Alteração completa (usada pelo PUT do orçamento) --------------------------

    @Test
    void deveAplicarAlteracaoCompletaMantendoPrecoCongeladoDosItensExistentes() {
        Servico oleo = servico("Troca de óleo", "120.00");
        OrcamentoItemServico itemOleo = orcamento.adicionarServico(oleo);
        OrcamentoItemPeca itemFiltro = orcamento.adicionarPeca(peca("Filtro", "35.00"), 2);
        orcamento.adicionarServico(servico("Lavagem", "40.00"));
        Fixtures.gravarItens(orcamento);
        oleo.atualizar("Troca de óleo", new BigDecimal("999.00")); // catálogo mudou depois da inclusão

        orcamento.alterar(new Alteracao(
                Set.of(itemOleo.getId()),
                Map.of(itemFiltro.getId(), 3),
                List.of(servico("Alinhamento", "60.00")),
                List.of(new PecaNova(peca("Palheta", "25.00"), 2)),
                new BigDecimal("10.00")), atendente());

        assertThat(orcamento.getItensServico()).extracting(OrcamentoItemServico::getDescricao)
                .containsExactly("Troca de óleo", "Alinhamento");
        assertThat(itemOleo.getPreco()).isEqualByComparingTo("120.00");
        assertThat(itemFiltro.getQuantidade()).isEqualTo(3);
        // 120 + 60 + 3×35 + 2×25 − 10 = 325
        assertThat(orcamento.getTotal()).isEqualByComparingTo("325.00");
    }

    @Test
    void naoDeveAplicarNadaQuandoAlteracaoTemDescontoInvalido() {
        OrcamentoItemServico item = orcamento.adicionarServico(servico("Troca de óleo", "100.00"));
        Fixtures.gravarItens(orcamento);

        assertThatThrownBy(() -> orcamento.alterar(new Alteracao(Set.of(), Map.of(),
                List.of(servico("Alinhamento", "60.00")), List.of(), new BigDecimal("50.00")), atendente()))
                .isInstanceOf(RegraDeNegocioException.class);

        assertThat(orcamento.getItensServico()).containsExactly(item);
        assertThat(orcamento.getTotal()).isEqualByComparingTo("100.00");
    }

    @Test
    void deveRecusarAlteracaoComItemDeOutroOrcamento() {
        orcamento.adicionarServico(servico("Troca de óleo", "100.00"));
        Fixtures.gravarItens(orcamento);

        assertThatThrownBy(() -> orcamento.alterar(new Alteracao(Set.of(-1L), Map.of(), List.of(), List.of(),
                BigDecimal.ZERO), atendente()))
                .isInstanceOf(RegraDeNegocioException.class);
    }

    @Test
    void deveRecusarAlteracaoComPecaNovaInativaOuQuantidadeInvalida() {
        Peca inativa = peca("Lâmpada", "15.00");
        inativa.inativar();
        assertThatThrownBy(() -> orcamento.alterar(new Alteracao(Set.of(), Map.of(), List.of(),
                List.of(new PecaNova(inativa, 1)), BigDecimal.ZERO), atendente()))
                .isInstanceOf(RegraDeNegocioException.class);
        assertThatThrownBy(() -> orcamento.alterar(new Alteracao(Set.of(), Map.of(), List.of(),
                List.of(new PecaNova(peca("Filtro", "10.00"), 0)), BigDecimal.ZERO), atendente()))
                .isInstanceOf(RegraDeNegocioException.class);
    }

    // Situação ----------------------------------------------------------------

    @Test
    void naoDevePermitirAlterarOrcamentoAprovado() {
        orcamento.adicionarServico(servico("Troca de óleo", "120.00"));
        orcamento.aprovar(atendente(), java.time.LocalDateTime.now());

        assertThatThrownBy(() -> orcamento.adicionarServico(servico("Alinhamento", "60.00")))
                .isInstanceOf(RegraDeNegocioException.class);
        assertThatThrownBy(() -> orcamento.aplicarDesconto(BigDecimal.ONE, administrador()))
                .isInstanceOf(RegraDeNegocioException.class);
    }

    @Test
    void deveRecusarAprovacaoDeOrcamentoSemItens() {
        assertThatThrownBy(() -> orcamento.aprovar(atendente(), java.time.LocalDateTime.now()))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("sem itens");
    }

    @Test
    void deveRecusarRecusaDeOrcamentoQueNaoEstaPendente() {
        orcamento.adicionarServico(servico("Troca de óleo", "120.00"));
        orcamento.recusar(atendente(), java.time.LocalDateTime.now());
        assertThatThrownBy(() -> orcamento.recusar(atendente(), java.time.LocalDateTime.now()))
                .isInstanceOf(RegraDeNegocioException.class);
    }
}
