package br.com.torque.ordens.dto;

import br.com.torque.auth.Usuario;
import br.com.torque.clientes.Cliente;
import br.com.torque.clientes.Veiculo;
import br.com.torque.ordens.HistoricoStatus;
import br.com.torque.ordens.Orcamento;
import br.com.torque.ordens.OrcamentoItemPeca;
import br.com.torque.ordens.OrcamentoItemServico;
import br.com.torque.ordens.OrdemServico;
import br.com.torque.ordens.SituacaoOrcamento;
import br.com.torque.ordens.StatusOS;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * OS completa: dados, orçamento vigente, versões anteriores e histórico de status.
 *
 * @param version versão da OS (bloqueio otimista); envie em {@code versaoOs} nas alterações
 */
public record OrdemServicoResponse(Long id, Long numero, StatusOS status, Integer version,
                                   String problemaRelatado, Integer quilometragem,
                                   LocalDateTime dataAbertura, LocalDateTime dataEntrega,
                                   VeiculoResumo veiculo, ClienteResumo cliente, MecanicoResumo mecanico,
                                   OrcamentoResponse orcamento, List<VersaoOrcamento> versoes,
                                   List<HistoricoResponse> historico) {

    public record VeiculoResumo(Long id, String placa, String marca, String modelo, Integer ano, String cor) {
    }

    public record ClienteResumo(Long id, String nome, String documento, String telefone, String email) {
    }

    public record MecanicoResumo(Long id, String nome) {
    }

    public record ItemServicoResponse(Long id, Long servicoId, String descricao, BigDecimal preco) {
    }

    public record ItemPecaResponse(Long id, Long pecaId, String descricao, int quantidade,
                                   BigDecimal precoUnitario, BigDecimal subtotal) {
    }

    public record OrcamentoResponse(Long id, Integer versao, SituacaoOrcamento situacao,
                                    List<ItemServicoResponse> servicos, List<ItemPecaResponse> pecas,
                                    BigDecimal subtotalServicos, BigDecimal subtotalPecas,
                                    BigDecimal desconto, BigDecimal total,
                                    String decididoPor, LocalDateTime dataDecisao) {
    }

    public record VersaoOrcamento(Integer versao, SituacaoOrcamento situacao, BigDecimal total) {
    }

    public record HistoricoResponse(StatusOS statusAnterior, StatusOS statusNovo, Long usuarioId, String usuario,
                                    LocalDateTime dataHora) {
    }

    public static OrdemServicoResponse de(OrdemServico os, List<HistoricoStatus> historico) {
        Veiculo v = os.getVeiculo();
        Cliente c = v.getCliente();
        Usuario m = os.getMecanico();
        return new OrdemServicoResponse(os.getId(), os.getNumero(), os.getStatus(), os.getVersion(),
                os.getProblemaRelatado(), os.getQuilometragem(), os.getDataAbertura(), os.getDataEntrega(),
                new VeiculoResumo(v.getId(), v.getPlaca(), v.getMarca(), v.getModelo(), v.getAno(), v.getCor()),
                new ClienteResumo(c.getId(), c.getNome(), c.getDocumento(), c.getTelefone(), c.getEmail()),
                m == null ? null : new MecanicoResumo(m.getId(), m.getNome()),
                orcamento(os.getOrcamentoVigente()),
                os.getOrcamentos().stream()
                        .map(o -> new VersaoOrcamento(o.getVersao(), o.getSituacao(), o.getTotal())).toList(),
                historico.stream().map(h -> new HistoricoResponse(h.getStatusAnterior(), h.getStatusNovo(),
                        h.getUsuario().getId(), h.getUsuario().getNome(), h.getDataHora())).toList());
    }

    private static OrcamentoResponse orcamento(Orcamento o) {
        return new OrcamentoResponse(o.getId(), o.getVersao(), o.getSituacao(),
                o.getItensServico().stream().map(OrdemServicoResponse::item).toList(),
                o.getItensPeca().stream().map(OrdemServicoResponse::item).toList(),
                o.getSubtotalServicos(), o.getSubtotalPecas(), o.getDesconto(), o.getTotal(),
                o.getDecididoPor() == null ? null : o.getDecididoPor().getNome(), o.getDataDecisao());
    }

    private static ItemServicoResponse item(OrcamentoItemServico i) {
        return new ItemServicoResponse(i.getId(), i.getServico().getId(), i.getDescricao(), i.getPreco());
    }

    private static ItemPecaResponse item(OrcamentoItemPeca i) {
        return new ItemPecaResponse(i.getId(), i.getPeca().getId(), i.getDescricao(), i.getQuantidade(),
                i.getPrecoUnitario(), i.getSubtotal());
    }
}
