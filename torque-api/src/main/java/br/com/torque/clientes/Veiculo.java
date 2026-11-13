package br.com.torque.clientes;

import br.com.torque.shared.validacao.PlacaUtil;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.Objects;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "veiculo")
public class Veiculo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** RN01: todo veículo tem um cliente. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    /** RN02: ABC1234 ou ABC1D23, maiúscula e sem hífen. */
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(nullable = false, length = 7, unique = true)
    private String placa;

    @Column(nullable = false, length = 60)
    private String marca;

    @Column(nullable = false, length = 80)
    private String modelo;

    @JdbcTypeCode(SqlTypes.SMALLINT)
    private Integer ano;

    @Column(length = 30)
    private String cor;

    protected Veiculo() {
    }

    Veiculo(Cliente cliente, String placa, String marca, String modelo, Integer ano, String cor) {
        this.cliente = Objects.requireNonNull(cliente, "Veículo precisa de um cliente");
        atualizar(placa, marca, modelo, ano, cor);
    }

    public void atualizar(String placa, String marca, String modelo, Integer ano, String cor) {
        this.placa = PlacaUtil.normalizar(placa);
        this.marca = marca.trim();
        this.modelo = modelo.trim();
        this.ano = ano;
        this.cor = cor == null || cor.isBlank() ? null : cor.trim();
    }

    public Long getId() {
        return id;
    }

    public Cliente getCliente() {
        return cliente;
    }

    public String getPlaca() {
        return placa;
    }

    public String getMarca() {
        return marca;
    }

    public String getModelo() {
        return modelo;
    }

    public Integer getAno() {
        return ano;
    }

    public String getCor() {
        return cor;
    }

    /** "Marca Modelo", para cabeçalhos e listagens. */
    public String getDescricao() {
        return marca + " " + modelo;
    }
}
