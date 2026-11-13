package br.com.torque.ordens;

import static br.com.torque.ordens.Fixtures.administrador;
import static br.com.torque.ordens.Fixtures.atendente;
import static br.com.torque.ordens.Fixtures.mecanico;
import static br.com.torque.ordens.Fixtures.osAberta;
import static br.com.torque.ordens.Fixtures.servico;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.torque.auth.Usuario;
import br.com.torque.ordens.StatusOS.Executor;
import br.com.torque.ordens.StatusOS.Transicao;
import br.com.torque.shared.excecao.RegraDeNegocioException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * CT34 e CT35: testes gerados a partir da tabela de transições ({@link StatusOS#todasAsTransicoes()}).
 * Se uma linha for incluída ou removida da tabela, os casos mudam junto.
 */
class TransicoesStatusTest {

    private final Usuario atendente = atendente();
    private final Usuario admin = administrador();
    private final Usuario mecanico = mecanico();

    /** Leva uma OS nova até o status pedido pelos caminhos válidos do domínio. */
    private OrdemServico osEm(StatusOS status) {
        OrdemServico os = osAberta();
        os.atribuirMecanico(mecanico, admin);
        if (status == StatusOS.ABERTA) {
            return os;
        }
        if (status == StatusOS.CANCELADA) {
            os.alterarStatus(StatusOS.CANCELADA, atendente);
            return os;
        }
        os.orcamentoParaEdicao(atendente).adicionarServico(servico("Revisão", "200.00"));
        os.alterarStatus(StatusOS.AGUARDANDO_APROVACAO, atendente);
        if (status == StatusOS.AGUARDANDO_APROVACAO) {
            return os;
        }
        if (status == StatusOS.RECUSADA) {
            os.recusarOrcamento(1, atendente);
            return os;
        }
        os.aprovarOrcamento(1, atendente);
        if (status == StatusOS.APROVADA) {
            return os;
        }
        os.alterarStatus(StatusOS.EM_EXECUCAO, mecanico);
        switch (status) {
            case EM_EXECUCAO -> { }
            case AGUARDANDO_PECA -> os.alterarStatus(StatusOS.AGUARDANDO_PECA, mecanico);
            case CONCLUIDA -> os.alterarStatus(StatusOS.CONCLUIDA, mecanico);
            case ENTREGUE -> {
                os.alterarStatus(StatusOS.CONCLUIDA, mecanico);
                os.alterarStatus(StatusOS.ENTREGUE, atendente);
            }
            default -> throw new IllegalArgumentException(status.name());
        }
        return os;
    }

    /** Executa a transição pelo caminho correto (direto ou fluxo do orçamento) com o perfil indicado na tabela. */
    private void executar(OrdemServico os, Transicao t) {
        Usuario usuario = t.executor() == Executor.MECANICO_ATRIBUIDO ? mecanico : atendente;
        if (!t.viaOrcamento()) {
            os.alterarStatus(t.para(), usuario);
            return;
        }
        int versao = os.getOrcamentoVigente().getVersao();
        switch (t.para()) {
            case APROVADA -> os.aprovarOrcamento(versao, usuario);
            case RECUSADA -> os.recusarOrcamento(versao, usuario);
            case AGUARDANDO_APROVACAO -> os.orcamentoParaEdicao(usuario);
            default -> throw new IllegalStateException("Transição via orçamento inesperada: " + t);
        }
    }

    static Stream<Transicao> transicoesPermitidas() {
        return StatusOS.todasAsTransicoes().stream();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("transicoesPermitidas")
    void deveAceitarTodaTransicaoDaTabela(Transicao transicao) {
        OrdemServico os = osEm(transicao.de());
        int historicoAntes = os.getHistorico().size();

        executar(os, transicao);

        assertThat(os.getStatus()).isEqualTo(transicao.para());
        assertThat(os.getHistorico()).hasSize(historicoAntes + 1);
        HistoricoStatus ultimo = os.getHistorico().get(historicoAntes);
        assertThat(ultimo.getStatusAnterior()).isEqualTo(transicao.de());
        assertThat(ultimo.getStatusNovo()).isEqualTo(transicao.para());
    }

    static Stream<Arguments> transicoesNaoPermitidas() {
        List<Arguments> pares = new ArrayList<>();
        for (StatusOS de : StatusOS.values()) {
            for (StatusOS para : StatusOS.values()) {
                if (de.transicaoPara(para).isEmpty()) {
                    pares.add(Arguments.of(de, para));
                }
            }
        }
        return pares.stream();
    }

    @ParameterizedTest(name = "{0} -> {1}")
    @MethodSource("transicoesNaoPermitidas")
    void deveRecusarTransicaoForaDaTabelaComRegraDeNegocio(StatusOS de, StatusOS para) {
        OrdemServico os = osEm(de);
        int historicoAntes = os.getHistorico().size();

        // Administrador: a recusa vem da tabela (422), não de falta de permissão.
        assertThatThrownBy(() -> os.alterarStatus(para, admin)).isInstanceOf(RegraDeNegocioException.class);

        assertThat(os.getStatus()).isEqualTo(de);
        assertThat(os.getHistorico()).hasSize(historicoAntes);
    }

    @Test
    void deveTerAsDezesseisTransicoesDoClaudeMd() {
        assertThat(StatusOS.todasAsTransicoes()).hasSize(16);
        assertThat(transicoesNaoPermitidas()).hasSize(StatusOS.values().length * StatusOS.values().length - 16);
    }

    @Test
    void statusFinaisNaoTemSaida() {
        for (StatusOS status : StatusOS.values()) {
            assertThat(status.destinosPermitidos().isEmpty()).isEqualTo(status.isFinal());
        }
    }
}
