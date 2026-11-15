package br.com.torque.ordens;

import br.com.torque.catalogo.Peca;
import br.com.torque.shared.Dinheiro;
import br.com.torque.shared.excecao.RegraDeNegocioException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import java.math.BigDecimal;
import java.util.Objects;

/** Peça incluída no orçamento. Descrição e preço unitário são cópias do catálogo na inclusão (RN10). */
@Entity
@Table(name = "orcamento_item_peca")
public class OrcamentoItemPeca {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Id do item da versão anterior quando este é uma cópia ainda não gravada (RN07). */
    @Transient
    private Long idOrigem;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "orcamento_id", nullable = false)
    private Orcamento orcamento;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "peca_id", nullable = false)
    private Peca peca;

    @Column(nullable = false, length = 150)
    private String descricao;

    @Column(nullable = false)
    private int quantidade;

    @Column(name = "preco_unitario", nullable = false, precision = 10, scale = 2)
    private BigDecimal precoUnitario;

    protected OrcamentoItemPeca() {
    }

    OrcamentoItemPeca(Orcamento orcamento, Peca peca, String descricao, int quantidade, BigDecimal precoUnitario) {
        this.orcamento = Objects.requireNonNull(orcamento);
        this.peca = Objects.requireNonNull(peca);
        this.descricao = Objects.requireNonNull(descricao);
        this.precoUnitario = Dinheiro.naoNegativo(precoUnitario, "Preço da peça");
        alterarQuantidade(quantidade);
    }

    void alterarQuantidade(int quantidade) {
        if (quantidade <= 0) {
            throw new RegraDeNegocioException("A quantidade da peça deve ser maior que zero");
        }
        this.quantidade = quantidade;
    }

    OrcamentoItemPeca copiarPara(Orcamento novo) {
        OrcamentoItemPeca copia = new OrcamentoItemPeca(novo, peca, descricao, quantidade, precoUnitario);
        copia.idOrigem = chave();
        return copia;
    }

    /** Identifica o item numa alteração: o próprio id ou, numa cópia ainda não gravada, o id de origem. */
    Long chave() {
        return id != null ? id : idOrigem;
    }

    /** quantidade × preço unitário (RN04). */
    public BigDecimal getSubtotal() {
        return Dinheiro.de(precoUnitario.multiply(BigDecimal.valueOf(quantidade)));
    }

    public Long getId() {
        return id;
    }

    public Peca getPeca() {
        return peca;
    }

    public String getDescricao() {
        return descricao;
    }

    public int getQuantidade() {
        return quantidade;
    }

    public BigDecimal getPrecoUnitario() {
        return precoUnitario;
    }
}
