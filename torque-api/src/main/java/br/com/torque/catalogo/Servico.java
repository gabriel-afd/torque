package br.com.torque.catalogo;

import br.com.torque.shared.Dinheiro;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;

/** Serviço (mão de obra) do catálogo. */
@Entity
@Table(name = "servico")
public class Servico {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String descricao;

    @Column(name = "preco_mao_de_obra", nullable = false, precision = 10, scale = 2)
    private BigDecimal precoMaoDeObra;

    @Column(nullable = false)
    private boolean ativo = true;

    protected Servico() {
    }

    public Servico(String descricao, BigDecimal precoMaoDeObra) {
        atualizar(descricao, precoMaoDeObra);
    }

    public void atualizar(String descricao, BigDecimal precoMaoDeObra) {
        this.descricao = descricao.trim();
        this.precoMaoDeObra = Dinheiro.naoNegativo(precoMaoDeObra, "Preço da mão de obra");
    }

    public void inativar() {
        this.ativo = false;
    }

    public Long getId() {
        return id;
    }

    public String getDescricao() {
        return descricao;
    }

    public BigDecimal getPrecoMaoDeObra() {
        return precoMaoDeObra;
    }

    public boolean isAtivo() {
        return ativo;
    }
}
