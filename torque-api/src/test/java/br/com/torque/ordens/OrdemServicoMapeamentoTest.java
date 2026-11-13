package br.com.torque.ordens;

import static org.assertj.core.api.Assertions.assertThat;

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
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

/** Confere o mapeamento JPA do agregado da OS contra o schema real (V1). */
class OrdemServicoMapeamentoTest extends IntegracaoTestBase {

    @Autowired
    private OrdemServicoRepository ordens;
    @Autowired
    private ClienteRepository clientes;
    @Autowired
    private VeiculoRepository veiculos;
    @Autowired
    private ServicoRepository servicos;
    @Autowired
    private PecaRepository pecas;
    @Autowired
    private TransactionTemplate transacao;

    @Test
    void devePersistirOsComOrcamentosItensEHistorico() {
        Usuario atendente = criarUsuario(Perfil.ATENDENTE);
        Usuario admin = criarUsuario(Perfil.ADMINISTRADOR);
        Usuario mecanico = criarUsuario(Perfil.MECANICO);
        Cliente cliente = clientes.save(new Cliente("Cliente Fictício", "52998224725", "83999990001", null));
        Veiculo veiculo = veiculos.save(cliente.novoVeiculo("ABC1D23", "Fiat", "Uno", 2015, null));
        Servico servico = servicos.save(new Servico("Troca de óleo", new BigDecimal("120.00")));
        Peca peca = pecas.save(new Peca("FLT-1", "Filtro", new BigDecimal("35.00")));

        Long id = transacao.execute(s -> {
            OrdemServico os = OrdemServico.abrir(veiculo, "Barulho", 50_000, atendente);
            Orcamento orcamento = os.orcamentoParaEdicao(atendente);
            orcamento.adicionarServico(servico);
            orcamento.adicionarPeca(peca, 2);
            orcamento.aplicarDesconto(new BigDecimal("19.00"), atendente);
            os.alterarStatus(StatusOS.AGUARDANDO_APROVACAO, atendente);
            os.aprovarOrcamento(1, atendente);
            os.atribuirMecanico(mecanico, admin);
            os.orcamentoParaEdicao(admin); // RN07: versão 2
            return ordens.saveAndFlush(os).getId();
        });

        transacao.executeWithoutResult(s -> {
            OrdemServico os = ordens.findById(id).orElseThrow();
            assertThat(os.getNumero()).isGreaterThanOrEqualTo(1000L);
            assertThat(os.getVersion()).isNotNull();
            assertThat(os.getStatus()).isEqualTo(StatusOS.AGUARDANDO_APROVACAO);
            assertThat(os.getMecanico().getId()).isEqualTo(mecanico.getId());
            assertThat(os.getOrcamentos()).extracting(Orcamento::getVersao).containsExactly(1, 2);
            assertThat(os.getOrcamento(1).getSituacao()).isEqualTo(SituacaoOrcamento.SUBSTITUIDO);
            assertThat(os.getOrcamento(1).getDecididoPor().getId()).isEqualTo(atendente.getId());
            Orcamento vigente = os.getOrcamentoVigente();
            assertThat(vigente.getItensServico()).hasSize(1);
            assertThat(vigente.getItensPeca()).singleElement()
                    .satisfies(p -> assertThat(p.getQuantidade()).isEqualTo(2));
            assertThat(vigente.getTotal()).isEqualByComparingTo("171.00");
            assertThat(os.getHistorico()).extracting(HistoricoStatus::getStatusNovo).containsExactly(
                    StatusOS.ABERTA, StatusOS.AGUARDANDO_APROVACAO, StatusOS.APROVADA, StatusOS.AGUARDANDO_APROVACAO);
        });

        assertThat(jdbc.queryForObject("SELECT count(*) FROM orcamento_item_servico", Integer.class)).isEqualTo(2);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM historico_status", Integer.class)).isEqualTo(4);
    }
}
