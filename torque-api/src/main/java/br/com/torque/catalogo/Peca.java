package br.com.torque.catalogo;

import br.com.torque.shared.Dinheiro;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.Locale;

/** Peça do catálogo. */
@Entity
@Table(name = "peca")
public class Peca {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 40, unique = true)
    private String codigo;

    @Column(nullable = false, length = 150)
    private String descricao;

    @Column(name = "preco_venda", nullable = false, precision = 10, scale = 2)
    private BigDecimal precoVenda;

    @Column(nullable = false)
    private boolean ativo = true;

    protected Peca() {
    }

    public Peca(String codigo, String descricao, BigDecimal precoVenda) {
        atualizar(codigo, descricao, precoVenda);
    }

    public static String normalizarCodigo(String codigo) {
        return codigo == null ? null : codigo.trim().toUpperCase(Locale.ROOT);
    }

    public void atualizar(String codigo, String descricao, BigDecimal precoVenda) {
        this.codigo = normalizarCodigo(codigo);
        this.descricao = descricao.trim();
        this.precoVenda = Dinheiro.naoNegativo(precoVenda, "Preço de venda");
    }

    public void inativar() {
        this.ativo = false;
    }

    public Long getId() {
        return id;
    }

    public String getCodigo() {
        return codigo;
    }

    public String getDescricao() {
        return descricao;
    }

    public BigDecimal getPrecoVenda() {
        return precoVenda;
    }

    public boolean isAtivo() {
        return ativo;
    }
}
