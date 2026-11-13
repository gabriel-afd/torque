package br.com.torque.ordens;

import br.com.torque.auth.Usuario;
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
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.Objects;

/** Registro imutável de cada mudança de status da OS: quem, quando, de/para. */
@Entity
@Table(name = "historico_status")
public class HistoricoStatus {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ordem_servico_id", nullable = false)
    private OrdemServico ordemServico;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    /** Nulo na abertura da OS. */
    @Enumerated(EnumType.STRING)
    @Column(name = "status_anterior", length = 30)
    private StatusOS statusAnterior;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_novo", nullable = false, length = 30)
    private StatusOS statusNovo;

    @Column(name = "data_hora", nullable = false)
    private LocalDateTime dataHora;

    protected HistoricoStatus() {
    }

    HistoricoStatus(OrdemServico ordemServico, Usuario usuario, StatusOS statusAnterior, StatusOS statusNovo,
                    LocalDateTime dataHora) {
        this.ordemServico = Objects.requireNonNull(ordemServico);
        this.usuario = Objects.requireNonNull(usuario);
        this.statusAnterior = statusAnterior;
        this.statusNovo = Objects.requireNonNull(statusNovo);
        this.dataHora = Objects.requireNonNull(dataHora);
    }

    public Long getId() {
        return id;
    }

    public OrdemServico getOrdemServico() {
        return ordemServico;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public StatusOS getStatusAnterior() {
        return statusAnterior;
    }

    public StatusOS getStatusNovo() {
        return statusNovo;
    }

    public LocalDateTime getDataHora() {
        return dataHora;
    }
}
