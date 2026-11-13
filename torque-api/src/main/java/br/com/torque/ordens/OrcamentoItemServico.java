package br.com.torque.ordens;

import br.com.torque.catalogo.Servico;
import br.com.torque.shared.Dinheiro;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.Objects;

/** Serviço incluído no orçamento. Descrição e preço são cópias do catálogo no momento da inclusão (RN10). */
@Entity
@Table(name = "orcamento_item_servico")
public class OrcamentoItemServico {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "orcamento_id", nullable = false)
    private Orcamento orcamento;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "servico_id", nullable = false)
    private Servico servico;

    @Column(nullable = false, length = 150)
    private String descricao;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal preco;

    protected OrcamentoItemServico() {
    }

    OrcamentoItemServico(Orcamento orcamento, Servico servico, String descricao, BigDecimal preco) {
        this.orcamento = Objects.requireNonNull(orcamento);
        this.servico = Objects.requireNonNull(servico);
        this.descricao = Objects.requireNonNull(descricao);
        this.preco = Dinheiro.naoNegativo(preco, "Preço do serviço");
    }

    /** Cópia congelada para uma nova versão do orçamento (mantém descrição e preço originais). */
    OrcamentoItemServico copiarPara(Orcamento novo) {
        return new OrcamentoItemServico(novo, servico, descricao, preco);
    }

    public Long getId() {
        return id;
    }

    public Servico getServico() {
        return servico;
    }

    public String getDescricao() {
        return descricao;
    }

    public BigDecimal getPreco() {
        return preco;
    }
}
