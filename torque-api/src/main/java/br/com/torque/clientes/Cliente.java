package br.com.torque.clientes;

import br.com.torque.shared.excecao.RegraDeNegocioException;
import br.com.torque.shared.validacao.CpfCnpjUtil;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "cliente")
public class Cliente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String nome;

    /** CPF (11) ou CNPJ (14), só dígitos (RN03). */
    @Column(nullable = false, length = 14, unique = true)
    private String documento;

    /** Só dígitos, com DDD. */
    @Column(nullable = false, length = 20)
    private String telefone;

    @Column(length = 160)
    private String email;

    @Column(nullable = false)
    private boolean ativo = true;

    protected Cliente() {
    }

    public Cliente(String nome, String documento, String telefone, String email) {
        atualizar(nome, documento, telefone, email);
    }

    public void atualizar(String nome, String documento, String telefone, String email) {
        this.nome = nome.trim();
        this.documento = CpfCnpjUtil.normalizar(documento);
        this.telefone = normalizarTelefone(telefone);
        this.email = email == null || email.isBlank() ? null : email.trim().toLowerCase();
    }

    public static String normalizarTelefone(String telefone) {
        return telefone == null ? null : telefone.replaceAll("\\D", "");
    }

    public void inativar() {
        this.ativo = false;
    }

    /** RN01: veículo sempre pertence a um cliente; cliente inativo não recebe veículos novos. */
    public Veiculo novoVeiculo(String placa, String marca, String modelo, Integer ano, String cor) {
        if (!ativo) {
            throw new RegraDeNegocioException("Cliente inativo não pode receber novos veículos");
        }
        return new Veiculo(this, placa, marca, modelo, ano, cor);
    }

    public Long getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public String getDocumento() {
        return documento;
    }

    public String getTelefone() {
        return telefone;
    }

    public String getEmail() {
        return email;
    }

    public boolean isAtivo() {
        return ativo;
    }
}
