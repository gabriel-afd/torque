package br.com.torque.ordens;

import static br.com.torque.ordens.Fixtures.administrador;
import static br.com.torque.ordens.Fixtures.atendente;
import static br.com.torque.ordens.Fixtures.mecanico;
import static br.com.torque.ordens.Fixtures.osAberta;
import static br.com.torque.ordens.Fixtures.peca;
import static br.com.torque.ordens.Fixtures.servico;
import static br.com.torque.ordens.Fixtures.veiculo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.torque.auth.Usuario;
import br.com.torque.shared.excecao.OperacaoNaoPermitidaException;
import br.com.torque.shared.excecao.RecursoNaoEncontradoException;
import br.com.torque.shared.excecao.RegraDeNegocioException;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class OrdemServicoTest {

    private final Usuario atendente = atendente();
    private final Usuario admin = administrador();
    private final Usuario mecanico = mecanico();

    /** OS com 1 serviço, enviada para aprovação. */
    private OrdemServico osAguardandoAprovacao() {
        OrdemServico os = osAberta();
        os.orcamentoParaEdicao(atendente).adicionarServico(servico("Troca de óleo", "120.00"));
        os.alterarStatus(StatusOS.AGUARDANDO_APROVACAO, atendente);
        return os;
    }

    private OrdemServico osAprovadaComMecanico() {
        OrdemServico os = osAguardandoAprovacao();
        os.aprovarOrcamento(1, atendente);
        os.atribuirMecanico(mecanico, admin);
        return os;
    }

    private OrdemServico osEmExecucao() {
        OrdemServico os = osAprovadaComMecanico();
        os.alterarStatus(StatusOS.EM_EXECUCAO, mecanico);
        return os;
    }

    // Abertura ----------------------------------------------------------------

    @Test
    void deveAbrirOsComOrcamentoVersaoUmPendenteEHistorico() {
        OrdemServico os = OrdemServico.abrir(veiculo(), "  Barulho no freio  ", 120_000, atendente);

        assertThat(os.getStatus()).isEqualTo(StatusOS.ABERTA);
        assertThat(os.getProblemaRelatado()).isEqualTo("Barulho no freio");
        assertThat(os.getDataAbertura()).isNotNull();
        assertThat(os.getOrcamentos()).singleElement().satisfies(o -> {
            assertThat(o.getVersao()).isEqualTo(1);
            assertThat(o.getSituacao()).isEqualTo(SituacaoOrcamento.PENDENTE);
            assertThat(o.getTotal()).isEqualByComparingTo("0.00");
        });
        assertThat(os.getHistorico()).singleElement().satisfies(h -> {
            assertThat(h.getStatusAnterior()).isNull();
            assertThat(h.getStatusNovo()).isEqualTo(StatusOS.ABERTA);
            assertThat(h.getUsuario()).isSameAs(atendente);
        });
    }

    @Test
    void deveExigirVeiculoNaAbertura() {
        assertThatThrownBy(() -> OrdemServico.abrir(null, "Barulho", 10, atendente))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void deveRecusarAberturaSemProblemaOuComKmNegativa() {
        assertThatThrownBy(() -> OrdemServico.abrir(veiculo(), " ", 10, atendente))
                .isInstanceOf(RegraDeNegocioException.class);
        assertThatThrownBy(() -> OrdemServico.abrir(veiculo(), "Barulho", -1, atendente))
                .isInstanceOf(RegraDeNegocioException.class);
    }

    @Test
    void deveImpedirMecanicoDeAbrirOs() {
        assertThatThrownBy(() -> OrdemServico.abrir(veiculo(), "Barulho", 10, mecanico))
                .isInstanceOf(OperacaoNaoPermitidaException.class);
    }

    // Aprovação e recusa (HU06) ---------------------------------------------------

    @Test
    void deveAprovarOrcamentoPendenteRegistrandoDataEUsuario() {
        OrdemServico os = osAguardandoAprovacao();

        os.aprovarOrcamento(1, atendente);

        assertThat(os.getStatus()).isEqualTo(StatusOS.APROVADA);
        Orcamento orcamento = os.getOrcamento(1);
        assertThat(orcamento.getSituacao()).isEqualTo(SituacaoOrcamento.APROVADO);
        assertThat(orcamento.getDecididoPor()).isSameAs(atendente);
        assertThat(orcamento.getDataDecisao()).isNotNull();
    }

    @Test
    void deveRecusarSegundaAprovacaoDoMesmoOrcamento() {
        OrdemServico os = osAguardandoAprovacao();
        os.aprovarOrcamento(1, atendente);

        assertThatThrownBy(() -> os.aprovarOrcamento(1, atendente))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("APROVADO");
        assertThat(os.getHistorico()).hasSize(3);
    }

    @Test
    void deveRecusarOrcamentoEFinalizarOsComoRecusada() {
        OrdemServico os = osAguardandoAprovacao();

        os.recusarOrcamento(1, atendente);

        assertThat(os.getStatus()).isEqualTo(StatusOS.RECUSADA);
        assertThat(os.getOrcamento(1).getSituacao()).isEqualTo(SituacaoOrcamento.RECUSADO);
        assertThat(os.getOrcamento(1).getDecididoPor()).isSameAs(atendente);
    }

    @Test
    void deveExigirOsAguardandoAprovacaoParaAprovar() {
        OrdemServico os = osAberta();
        os.orcamentoParaEdicao(atendente).adicionarServico(servico("Troca de óleo", "120.00"));

        assertThatThrownBy(() -> os.aprovarOrcamento(1, atendente)).isInstanceOf(RegraDeNegocioException.class);
        assertThat(os.getOrcamento(1).isPendente()).isTrue();
    }

    @Test
    void deveImpedirMecanicoDeRegistrarAprovacao() {
        OrdemServico os = osAguardandoAprovacao();
        assertThatThrownBy(() -> os.aprovarOrcamento(1, mecanico)).isInstanceOf(OperacaoNaoPermitidaException.class);
        assertThat(os.getStatus()).isEqualTo(StatusOS.AGUARDANDO_APROVACAO);
    }

    @Test
    void deveDevolverNaoEncontradoParaVersaoInexistente() {
        assertThatThrownBy(() -> osAguardandoAprovacao().aprovarOrcamento(9, atendente))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    // RN07 ----------------------------------------------------------------------

    @Test
    void deveCriarNovaVersaoAoAlterarOrcamentoAprovado() {
        OrdemServico os = osAguardandoAprovacao();
        os.orcamentoParaEdicao(atendente);
        os.aprovarOrcamento(1, atendente);
        Orcamento anterior = os.getOrcamentoVigente();

        Orcamento nova = os.orcamentoParaEdicao(atendente);
        nova.adicionarPeca(peca("Filtro de óleo", "35.00"), 1);

        assertThat(nova).isNotSameAs(anterior);
        assertThat(nova.getVersao()).isEqualTo(2);
        assertThat(nova.getSituacao()).isEqualTo(SituacaoOrcamento.PENDENTE);
        assertThat(nova.getItensServico()).extracting(OrcamentoItemServico::getPreco)
                .usingElementComparator(BigDecimal::compareTo).containsExactly(new BigDecimal("120.00"));
        assertThat(nova.getTotal()).isEqualByComparingTo("155.00");
        assertThat(anterior.getSituacao()).isEqualTo(SituacaoOrcamento.SUBSTITUIDO);
        assertThat(anterior.getTotal()).isEqualByComparingTo("120.00");
        assertThat(os.getStatus()).isEqualTo(StatusOS.AGUARDANDO_APROVACAO);
        assertThat(os.getOrcamentoVigente()).isSameAs(nova);
    }

    @Test
    void deveCriarNovaVersaoTambemComOsEmExecucao() {
        OrdemServico os = osEmExecucao();

        Orcamento nova = os.orcamentoParaEdicao(admin);

        assertThat(nova.getVersao()).isEqualTo(2);
        assertThat(os.getStatus()).isEqualTo(StatusOS.AGUARDANDO_APROVACAO);
    }

    @Test
    void deveUsarOProprioOrcamentoPendenteSemCriarVersao() {
        OrdemServico os = osAguardandoAprovacao();
        assertThat(os.orcamentoParaEdicao(atendente).getVersao()).isEqualTo(1);
        assertThat(os.getOrcamentos()).hasSize(1);
    }

    @Test
    void naoDevePermitirAlterarOrcamentoComOsAguardandoPeca() {
        OrdemServico os = osEmExecucao();
        os.alterarStatus(StatusOS.AGUARDANDO_PECA, mecanico);
        assertThatThrownBy(() -> os.orcamentoParaEdicao(atendente)).isInstanceOf(RegraDeNegocioException.class);
    }

    @Test
    void deveImpedirMecanicoDeAlterarOrcamento() {
        assertThatThrownBy(() -> osAberta().orcamentoParaEdicao(mecanico))
                .isInstanceOf(OperacaoNaoPermitidaException.class);
    }

    @Test
    void deveRecusarMudancaDiretaQuePertenceAoFluxoDoOrcamento() {
        assertThatThrownBy(() -> osAguardandoAprovacao().alterarStatus(StatusOS.APROVADA, admin))
                .isInstanceOf(RegraDeNegocioException.class);
        assertThatThrownBy(() -> osAprovadaComMecanico().alterarStatus(StatusOS.AGUARDANDO_APROVACAO, admin))
                .isInstanceOf(RegraDeNegocioException.class);
    }

    // RN06 ----------------------------------------------------------------------

    @Test
    void deveRecusarInicioDeExecucaoSemOrcamentoAprovado() {
        OrdemServico os = osAberta();
        os.atribuirMecanico(mecanico, admin);

        assertThatThrownBy(() -> os.alterarStatus(StatusOS.EM_EXECUCAO, admin))
                .isInstanceOf(RegraDeNegocioException.class);
        assertThat(os.getStatus()).isEqualTo(StatusOS.ABERTA);
    }

    // RN09 ----------------------------------------------------------------------

    @Test
    void deveDeixarMecanicoAtribuidoIniciarServico() {
        OrdemServico os = osAprovadaComMecanico();

        os.alterarStatus(StatusOS.EM_EXECUCAO, mecanico);

        assertThat(os.getStatus()).isEqualTo(StatusOS.EM_EXECUCAO);
    }

    @Test
    void deveImpedirMecanicoNaoAtribuidoDeAlterarStatus() {
        OrdemServico os = osAprovadaComMecanico();

        assertThatThrownBy(() -> os.alterarStatus(StatusOS.EM_EXECUCAO, mecanico()))
                .isInstanceOf(OperacaoNaoPermitidaException.class)
                .hasMessageContaining("não está atribuída");
        assertThat(os.getStatus()).isEqualTo(StatusOS.APROVADA);
    }

    @Test
    void deveImpedirAtendenteDeExecutarTransicaoDoMecanico() {
        OrdemServico os = osAprovadaComMecanico();
        assertThatThrownBy(() -> os.alterarStatus(StatusOS.EM_EXECUCAO, atendente))
                .isInstanceOf(OperacaoNaoPermitidaException.class);
    }

    @Test
    void deveDeixarAdministradorExecutarTransicaoDoMecanico() {
        OrdemServico os = osAprovadaComMecanico();
        os.alterarStatus(StatusOS.EM_EXECUCAO, admin);
        assertThat(os.getStatus()).isEqualTo(StatusOS.EM_EXECUCAO);
    }

    @Test
    void deveImpedirMecanicoDeExecutarTransicaoDoBalcao() {
        OrdemServico os = osEmExecucao();
        assertThatThrownBy(() -> os.alterarStatus(StatusOS.CANCELADA, mecanico))
                .isInstanceOf(OperacaoNaoPermitidaException.class);
    }

    // CT37 ----------------------------------------------------------------------

    @Test
    void deveIrEVoltarDeAguardandoPeca() {
        OrdemServico os = osEmExecucao();

        os.alterarStatus(StatusOS.AGUARDANDO_PECA, mecanico);
        os.alterarStatus(StatusOS.EM_EXECUCAO, mecanico);

        assertThat(os.getStatus()).isEqualTo(StatusOS.EM_EXECUCAO);
        assertThat(os.getHistorico()).extracting(HistoricoStatus::getStatusNovo).endsWith(
                StatusOS.EM_EXECUCAO, StatusOS.AGUARDANDO_PECA, StatusOS.EM_EXECUCAO);
    }

    // RN08 ----------------------------------------------------------------------

    @ParameterizedTest
    @EnumSource(StatusOS.class)
    void deveRecusarQualquerMudancaDeOsEntregue(StatusOS destino) {
        OrdemServico os = osEmExecucao();
        os.alterarStatus(StatusOS.CONCLUIDA, mecanico);
        os.alterarStatus(StatusOS.ENTREGUE, atendente);

        assertThatThrownBy(() -> os.alterarStatus(destino, admin))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("finalizada");
        assertThat(os.getStatus()).isEqualTo(StatusOS.ENTREGUE);
    }

    @ParameterizedTest
    @EnumSource(StatusOS.class)
    void deveRecusarQualquerMudancaDeOsRecusadaOuCancelada(StatusOS destino) {
        OrdemServico recusada = osAguardandoAprovacao();
        recusada.recusarOrcamento(1, atendente);
        OrdemServico cancelada = osAberta();
        cancelada.alterarStatus(StatusOS.CANCELADA, atendente);

        assertThatThrownBy(() -> recusada.alterarStatus(destino, admin)).isInstanceOf(RegraDeNegocioException.class);
        assertThatThrownBy(() -> cancelada.alterarStatus(destino, admin)).isInstanceOf(RegraDeNegocioException.class);
        assertThatThrownBy(() -> cancelada.orcamentoParaEdicao(admin)).isInstanceOf(RegraDeNegocioException.class);
        assertThatThrownBy(() -> cancelada.atribuirMecanico(mecanico, admin)).isInstanceOf(RegraDeNegocioException.class);
    }

    @Test
    void deveRegistrarDataDeEntrega() {
        OrdemServico os = osEmExecucao();
        os.alterarStatus(StatusOS.CONCLUIDA, mecanico);
        assertThat(os.getDataEntrega()).isNull();

        os.alterarStatus(StatusOS.ENTREGUE, atendente);

        assertThat(os.getDataEntrega()).isNotNull();
    }

    // Histórico -------------------------------------------------------------------

    @Test
    void deveGravarHistoricoDeCadaMudancaComUsuarioEDeParaCorretos() {
        OrdemServico os = osEmExecucao();

        assertThat(os.getHistorico()).extracting(HistoricoStatus::getStatusAnterior, HistoricoStatus::getStatusNovo)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple(null, StatusOS.ABERTA),
                        org.assertj.core.groups.Tuple.tuple(StatusOS.ABERTA, StatusOS.AGUARDANDO_APROVACAO),
                        org.assertj.core.groups.Tuple.tuple(StatusOS.AGUARDANDO_APROVACAO, StatusOS.APROVADA),
                        org.assertj.core.groups.Tuple.tuple(StatusOS.APROVADA, StatusOS.EM_EXECUCAO));
        assertThat(os.getHistorico().get(3).getUsuario()).isSameAs(mecanico);
        assertThat(os.getHistorico()).allSatisfy(h -> assertThat(h.getDataHora()).isNotNull());
    }

    // Atribuição de mecânico ----------------------------------------------------

    @Test
    void deveDeixarSoAdministradorAtribuirMecanicoAtivo() {
        OrdemServico os = osAberta();

        assertThatThrownBy(() -> os.atribuirMecanico(mecanico, atendente))
                .isInstanceOf(OperacaoNaoPermitidaException.class);
        assertThatThrownBy(() -> os.atribuirMecanico(atendente(), admin))
                .isInstanceOf(RegraDeNegocioException.class);
        Usuario inativo = mecanico();
        inativo.inativar();
        assertThatThrownBy(() -> os.atribuirMecanico(inativo, admin))
                .isInstanceOf(RegraDeNegocioException.class);

        os.atribuirMecanico(mecanico, admin);
        assertThat(os.getMecanico()).isSameAs(mecanico);
        assertThat(os.isMecanicoAtribuido(mecanico)).isTrue();
        assertThat(os.isMecanicoAtribuido(mecanico())).isFalse();
    }
}
