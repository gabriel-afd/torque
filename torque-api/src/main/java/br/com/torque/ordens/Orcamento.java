package br.com.torque.ordens;

import br.com.torque.auth.Perfil;
import br.com.torque.auth.Usuario;
import br.com.torque.catalogo.Peca;
import br.com.torque.catalogo.Servico;
import br.com.torque.shared.Dinheiro;
import br.com.torque.shared.excecao.RegraDeNegocioException;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Uma versão do orçamento da OS. Só é alterável enquanto {@link SituacaoOrcamento#PENDENTE};
 * aprovado é imutável (RN07).
 */
@Entity
@Table(name = "orcamento")
public class Orcamento {

    /** RN05: limite de desconto do atendente sobre o valor dos itens. */
    static final BigDecimal LIMITE_DESCONTO_ATENDENTE = new BigDecimal("0.10");

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ordem_servico_id", nullable = false)
    private OrdemServico ordemServico;

    @JdbcTypeCode(SqlTypes.SMALLINT)
    @Column(nullable = false)
    private Integer versao;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal desconto = Dinheiro.ZERO;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal total = Dinheiro.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SituacaoOrcamento situacao = SituacaoOrcamento.PENDENTE;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "decidido_por_id")
    private Usuario decididoPor;

    @Column(name = "data_decisao")
    private LocalDateTime dataDecisao;

    @OneToMany(mappedBy = "orcamento", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id")
    private List<OrcamentoItemServico> itensServico = new ArrayList<>();

    @OneToMany(mappedBy = "orcamento", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id")
    private List<OrcamentoItemPeca> itensPeca = new ArrayList<>();

    protected Orcamento() {
    }

    Orcamento(OrdemServico ordemServico, int versao) {
        this.ordemServico = Objects.requireNonNull(ordemServico);
        this.versao = versao;
    }

    /** RN07: nova versão PENDENTE com cópia dos itens (preços congelados) e do desconto da anterior. */
    static Orcamento novaVersaoDe(Orcamento anterior) {
        Orcamento novo = new Orcamento(anterior.ordemServico, anterior.versao + 1);
        anterior.itensServico.forEach(item -> novo.itensServico.add(item.copiarPara(novo)));
        anterior.itensPeca.forEach(item -> novo.itensPeca.add(item.copiarPara(novo)));
        novo.desconto = anterior.desconto;
        novo.recalcularTotal();
        return novo;
    }

    // Itens -------------------------------------------------------------------

    /** RN10: copia descrição e preço do catálogo. Item inativo não entra (422). */
    public OrcamentoItemServico adicionarServico(Servico servico) {
        exigirPendente();
        if (!servico.isAtivo()) {
            throw new RegraDeNegocioException("Serviço inativo no catálogo: " + servico.getDescricao());
        }
        var item = new OrcamentoItemServico(this, servico, servico.getDescricao(), servico.getPrecoMaoDeObra());
        itensServico.add(item);
        recalcularTotal();
        return item;
    }

    /** RN10: copia descrição e preço do catálogo. Item inativo não entra (422). */
    public OrcamentoItemPeca adicionarPeca(Peca peca, int quantidade) {
        exigirPendente();
        if (!peca.isAtivo()) {
            throw new RegraDeNegocioException("Peça inativa no catálogo: " + peca.getDescricao());
        }
        var item = new OrcamentoItemPeca(this, peca, peca.getDescricao(), quantidade, peca.getPrecoVenda());
        itensPeca.add(item);
        recalcularTotal();
        return item;
    }

    public void removerServico(OrcamentoItemServico item) {
        exigirPendente();
        exigirDescontoCabe(getSubtotal().subtract(item.getPreco()), desconto);
        if (!itensServico.remove(item)) {
            throw new RegraDeNegocioException("Serviço não pertence a este orçamento");
        }
        recalcularTotal();
    }

    public void removerPeca(OrcamentoItemPeca item) {
        exigirPendente();
        exigirDescontoCabe(getSubtotal().subtract(item.getSubtotal()), desconto);
        if (!itensPeca.remove(item)) {
            throw new RegraDeNegocioException("Peça não pertence a este orçamento");
        }
        recalcularTotal();
    }

    public void alterarQuantidade(OrcamentoItemPeca item, int quantidade) {
        exigirPendente();
        if (!itensPeca.contains(item)) {
            throw new RegraDeNegocioException("Peça não pertence a este orçamento");
        }
        BigDecimal novoSubtotalItem = item.getPrecoUnitario().multiply(BigDecimal.valueOf(Math.max(quantidade, 0)));
        exigirDescontoCabe(getSubtotal().subtract(item.getSubtotal()).add(novoSubtotalItem), desconto);
        item.alterarQuantidade(quantidade);
        recalcularTotal();
    }

    /** Peça nova a incluir numa {@link Alteracao}. */
    public record PecaNova(Peca peca, int quantidade) {
    }

    /**
     * Alteração completa do orçamento (itens mantidos, novos e desconto), validada inteira antes de
     * aplicar: ou tudo muda, ou nada muda.
     *
     * @param servicosMantidos ids dos itens de serviço que continuam (os demais saem)
     * @param pecasMantidas    id do item de peça que continua → nova quantidade (os demais saem)
     *                         (numa nova versão ainda não gravada, valem os ids dos itens da versão anterior)
     */
    public record Alteracao(Set<Long> servicosMantidos, Map<Long, Integer> pecasMantidas,
                            List<Servico> servicosNovos, List<PecaNova> pecasNovas, BigDecimal desconto) {
    }

    public void alterar(Alteracao alteracao, Usuario usuario) {
        exigirPendente();
        validarIdsExistentes(alteracao);

        BigDecimal subtotalFinal = Dinheiro.ZERO;
        for (OrcamentoItemServico item : itensServico) {
            if (alteracao.servicosMantidos().contains(item.chave())) {
                subtotalFinal = subtotalFinal.add(item.getPreco());
            }
        }
        for (OrcamentoItemPeca item : itensPeca) {
            Integer quantidade = alteracao.pecasMantidas().get(item.chave());
            if (quantidade != null) {
                exigirQuantidadePositiva(quantidade);
                subtotalFinal = subtotalFinal.add(item.getPrecoUnitario().multiply(BigDecimal.valueOf(quantidade)));
            }
        }
        for (Servico servico : alteracao.servicosNovos()) {
            exigirAtivo(servico.isAtivo(), "Serviço inativo no catálogo: " + servico.getDescricao());
            subtotalFinal = subtotalFinal.add(servico.getPrecoMaoDeObra());
        }
        for (PecaNova nova : alteracao.pecasNovas()) {
            exigirAtivo(nova.peca().isAtivo(), "Peça inativa no catálogo: " + nova.peca().getDescricao());
            exigirQuantidadePositiva(nova.quantidade());
            subtotalFinal = subtotalFinal.add(nova.peca().getPrecoVenda().multiply(BigDecimal.valueOf(nova.quantidade())));
        }
        BigDecimal novoDesconto = validarDesconto(alteracao.desconto(), Dinheiro.de(subtotalFinal), usuario);

        // Validado: aplica.
        itensServico.removeIf(item -> !alteracao.servicosMantidos().contains(item.chave()));
        itensPeca.removeIf(item -> !alteracao.pecasMantidas().containsKey(item.chave()));
        itensPeca.forEach(item -> item.alterarQuantidade(alteracao.pecasMantidas().get(item.chave())));
        alteracao.servicosNovos().forEach(s ->
                itensServico.add(new OrcamentoItemServico(this, s, s.getDescricao(), s.getPrecoMaoDeObra())));
        alteracao.pecasNovas().forEach(n -> itensPeca.add(
                new OrcamentoItemPeca(this, n.peca(), n.peca().getDescricao(), n.quantidade(), n.peca().getPrecoVenda())));
        this.desconto = novoDesconto;
        recalcularTotal();
    }

    private void validarIdsExistentes(Alteracao alteracao) {
        Set<Long> idsServico = itensServico.stream().map(OrcamentoItemServico::chave).collect(Collectors.toSet());
        Set<Long> idsPeca = itensPeca.stream().map(OrcamentoItemPeca::chave).collect(Collectors.toSet());
        if (!idsServico.containsAll(alteracao.servicosMantidos())
                || !idsPeca.containsAll(alteracao.pecasMantidas().keySet())) {
            throw new RegraDeNegocioException("Item informado não pertence a este orçamento");
        }
    }

    // Desconto e total (RN04, RN05) -----------------------------------------

    /** RN05: ATENDENTE até 10% do valor dos itens; ADMINISTRADOR sem limite; nunca acima do valor dos itens. */
    public void aplicarDesconto(BigDecimal valor, Usuario usuario) {
        exigirPendente();
        this.desconto = validarDesconto(valor, getSubtotal(), usuario);
        recalcularTotal();
    }

    private BigDecimal validarDesconto(BigDecimal valor, BigDecimal subtotal, Usuario usuario) {
        Permissoes.exigirBalcao(usuario, "conceder desconto");
        if (valor == null || valor.signum() < 0) {
            throw new RegraDeNegocioException("O desconto não pode ser negativo");
        }
        BigDecimal desconto = Dinheiro.de(valor);
        exigirDescontoCabe(subtotal, desconto);
        if (usuario.getPerfil() == Perfil.ATENDENTE) {
            BigDecimal limite = Dinheiro.de(subtotal.multiply(LIMITE_DESCONTO_ATENDENTE));
            if (desconto.compareTo(limite) > 0) {
                throw new RegraDeNegocioException(
                        "Desconto acima do permitido para atendente: máximo de 10% (R$ " + limite + ")");
            }
        }
        return desconto;
    }

    private static void exigirDescontoCabe(BigDecimal subtotal, BigDecimal desconto) {
        if (desconto.compareTo(subtotal) > 0) {
            throw new RegraDeNegocioException("O desconto não pode ser maior que o valor dos itens");
        }
    }

    public BigDecimal getSubtotalServicos() {
        return Dinheiro.de(itensServico.stream().map(OrcamentoItemServico::getPreco)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    public BigDecimal getSubtotalPecas() {
        return Dinheiro.de(itensPeca.stream().map(OrcamentoItemPeca::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    /** Serviços + peças, antes do desconto. */
    public BigDecimal getSubtotal() {
        return getSubtotalServicos().add(getSubtotalPecas());
    }

    /** RN04: soma dos serviços + soma (quantidade × preço unitário) das peças − desconto. */
    public BigDecimal calcularTotal() {
        return Dinheiro.de(getSubtotal().subtract(desconto));
    }

    private void recalcularTotal() {
        this.total = calcularTotal();
    }

    // Decisão do cliente --------------------------------------------------------

    void aprovar(Usuario usuario, LocalDateTime quando) {
        exigirPendente();
        if (itensServico.isEmpty() && itensPeca.isEmpty()) {
            throw new RegraDeNegocioException("Orçamento sem itens não pode ser aprovado");
        }
        decidir(SituacaoOrcamento.APROVADO, usuario, quando);
    }

    void recusar(Usuario usuario, LocalDateTime quando) {
        exigirPendente();
        decidir(SituacaoOrcamento.RECUSADO, usuario, quando);
    }

    /** RN07: o aprovado anterior vira SUBSTITUIDO quando nasce a nova versão. */
    void substituir() {
        if (situacao != SituacaoOrcamento.APROVADO) {
            throw new RegraDeNegocioException("Só orçamento aprovado pode ser substituído");
        }
        this.situacao = SituacaoOrcamento.SUBSTITUIDO;
    }

    private void decidir(SituacaoOrcamento nova, Usuario usuario, LocalDateTime quando) {
        this.situacao = nova;
        this.decididoPor = Objects.requireNonNull(usuario);
        this.dataDecisao = Objects.requireNonNull(quando);
    }

    private void exigirPendente() {
        if (situacao != SituacaoOrcamento.PENDENTE) {
            throw new RegraDeNegocioException("Orçamento " + situacao + " não pode ser alterado (versão " + versao + ")");
        }
    }

    private static void exigirAtivo(boolean ativo, String mensagem) {
        if (!ativo) {
            throw new RegraDeNegocioException(mensagem);
        }
    }

    private static void exigirQuantidadePositiva(Integer quantidade) {
        if (quantidade == null || quantidade <= 0) {
            throw new RegraDeNegocioException("A quantidade da peça deve ser maior que zero");
        }
    }

    public boolean isPendente() {
        return situacao == SituacaoOrcamento.PENDENTE;
    }

    public boolean isAprovado() {
        return situacao == SituacaoOrcamento.APROVADO;
    }

    public boolean temItens() {
        return !itensServico.isEmpty() || !itensPeca.isEmpty();
    }

    // Leitura -------------------------------------------------------------------

    public Long getId() {
        return id;
    }

    public OrdemServico getOrdemServico() {
        return ordemServico;
    }

    public Integer getVersao() {
        return versao;
    }

    public BigDecimal getDesconto() {
        return desconto;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public SituacaoOrcamento getSituacao() {
        return situacao;
    }

    public Usuario getDecididoPor() {
        return decididoPor;
    }

    public LocalDateTime getDataDecisao() {
        return dataDecisao;
    }

    public List<OrcamentoItemServico> getItensServico() {
        return Collections.unmodifiableList(itensServico);
    }

    public List<OrcamentoItemPeca> getItensPeca() {
        return Collections.unmodifiableList(itensPeca);
    }
}
