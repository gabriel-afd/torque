package br.com.torque.ordens;

import br.com.torque.auth.Perfil;
import br.com.torque.auth.Usuario;
import br.com.torque.clientes.Veiculo;
import br.com.torque.ordens.StatusOS.Transicao;
import br.com.torque.shared.excecao.ConflitoException;
import br.com.torque.shared.excecao.OperacaoNaoPermitidaException;
import br.com.torque.shared.excecao.RecursoNaoEncontradoException;
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
import jakarta.persistence.Version;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

/**
 * Ordem de serviço — raiz do agregado (orçamentos e histórico). As regras de status
 * (RN06, RN07, RN08, RN09) ficam aqui; a tabela de transições fica em {@link StatusOS}.
 */
@Entity
@Table(name = "ordem_servico")
public class OrdemServico {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Número sequencial para o cliente, gerado pela sequence do banco. */
    @Generated(event = EventType.INSERT)
    @Column(insertable = false, updatable = false, nullable = false)
    private Long numero;

    /** RN01: toda OS tem um veículo. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "veiculo_id", nullable = false)
    private Veiculo veiculo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mecanico_id")
    private Usuario mecanico;

    @Column(name = "problema_relatado", nullable = false, columnDefinition = "text")
    private String problemaRelatado;

    private Integer quilometragem;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private StatusOS status;

    @Column(name = "data_abertura", nullable = false)
    private LocalDateTime dataAbertura;

    @Column(name = "data_entrega")
    private LocalDateTime dataEntrega;

    /** Bloqueio otimista. */
    @Version
    @Column(nullable = false)
    private Integer version;

    @OneToMany(mappedBy = "ordemServico", cascade = CascadeType.ALL)
    @OrderBy("versao")
    private List<Orcamento> orcamentos = new ArrayList<>();

    @OneToMany(mappedBy = "ordemServico", cascade = CascadeType.ALL)
    @OrderBy("id")
    private List<HistoricoStatus> historico = new ArrayList<>();

    protected OrdemServico() {
    }

    /** Abre a OS em ABERTA, com o orçamento versão 1 PENDENTE vazio e o primeiro registro de histórico. */
    public static OrdemServico abrir(Veiculo veiculo, String problemaRelatado, Integer quilometragem, Usuario usuario) {
        Permissoes.exigirBalcao(usuario, "abrir ordem de serviço");
        if (problemaRelatado == null || problemaRelatado.isBlank()) {
            throw new RegraDeNegocioException("Informe o problema relatado pelo cliente");
        }
        if (quilometragem != null && quilometragem < 0) {
            throw new RegraDeNegocioException("A quilometragem não pode ser negativa");
        }
        OrdemServico os = new OrdemServico();
        os.veiculo = Objects.requireNonNull(veiculo, "Toda OS precisa de um veículo");
        os.problemaRelatado = problemaRelatado.trim();
        os.quilometragem = quilometragem;
        os.dataAbertura = LocalDateTime.now();
        os.orcamentos.add(new Orcamento(os, 1));
        os.registrar(null, StatusOS.ABERTA, usuario);
        return os;
    }

    // Status --------------------------------------------------------------------

    /**
     * Mudança de status direta (PATCH /status). Valida RN08, a tabela de transições, o perfil (RN09)
     * e RN06. Transições do fluxo do orçamento (aprovar, recusar, nova versão) são recusadas aqui.
     */
    public void alterarStatus(StatusOS novo, Usuario usuario) {
        Transicao transicao = transicaoPermitida(novo);
        if (transicao.viaOrcamento()) {
            throw new RegraDeNegocioException(novo == StatusOS.AGUARDANDO_APROVACAO
                    ? "Para voltar à aprovação, altere o orçamento (uma nova versão será criada)"
                    : "Use o registro de aprovação ou recusa do orçamento");
        }
        exigirExecutor(transicao, usuario);
        if (novo == StatusOS.EM_EXECUCAO && !getOrcamentoVigente().isAprovado()) {
            throw new RegraDeNegocioException("A OS só pode entrar em execução com orçamento aprovado");
        }
        mudarPara(novo, usuario);
    }

    private Transicao transicaoPermitida(StatusOS novo) {
        Objects.requireNonNull(novo, "Status obrigatório");
        if (status.isFinal()) {
            throw new RegraDeNegocioException("OS " + status + " está finalizada e não pode mudar de status");
        }
        return status.transicaoPara(novo).orElseThrow(() ->
                new RegraDeNegocioException("Transição de " + status + " para " + novo + " não permitida"));
    }

    private void exigirExecutor(Transicao transicao, Usuario usuario) {
        Objects.requireNonNull(usuario, "Usuário obrigatório");
        switch (transicao.executor()) {
            case BALCAO -> Permissoes.exigirBalcao(usuario, "mudar a OS para " + transicao.para());
            case MECANICO_ATRIBUIDO -> {
                if (usuario.isAdministrador() || isMecanicoAtribuido(usuario)) {
                    return;
                }
                throw new OperacaoNaoPermitidaException(usuario.getPerfil() == Perfil.MECANICO
                        ? "Esta OS não está atribuída a você"
                        : "Somente o mecânico atribuído ou o administrador pode mudar a OS para " + transicao.para());
            }
        }
    }

    private void mudarPara(StatusOS novo, Usuario usuario) {
        StatusOS anterior = this.status;
        this.status = novo;
        if (novo == StatusOS.ENTREGUE) {
            this.dataEntrega = LocalDateTime.now();
        }
        registrar(anterior, novo, usuario);
    }

    private void registrar(StatusOS anterior, StatusOS novo, Usuario usuario) {
        this.status = novo;
        historico.add(new HistoricoStatus(this, usuario, anterior, novo, LocalDateTime.now()));
    }

    // Orçamento -------------------------------------------------------------------

    public Orcamento getOrcamentoVigente() {
        return orcamentos.get(orcamentos.size() - 1);
    }

    public Orcamento getOrcamento(int versao) {
        return orcamentos.stream()
                .filter(o -> o.getVersao() == versao)
                .findFirst()
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Orçamento versão " + versao + " não encontrado na OS " + numero));
    }

    /**
     * Devolve o orçamento a ser alterado. Pendente (OS ABERTA ou AGUARDANDO_APROVACAO): o próprio vigente.
     * Aprovado com a OS APROVADA ou EM_EXECUCAO (RN07): cria a versão seguinte, o anterior vira
     * SUBSTITUIDO e a OS volta a AGUARDANDO_APROVACAO.
     */
    public Orcamento orcamentoParaEdicao(Usuario usuario) {
        Orcamento vigente = exigirOrcamentoAlteravel(usuario);
        if (dispensaNovaVersao(vigente)) {
            return vigente;
        }
        Orcamento nova = Orcamento.novaVersaoDe(vigente);
        adotarNovaVersao(vigente, nova, usuario);
        return nova;
    }

    /**
     * Aplica a alteração do PUT do orçamento. {@code versao} é a versão que o usuário estava editando:
     * se não for mais a vigente, 409. Com orçamento aprovado (RN07) a alteração é aplicada na nova versão
     * antes de ela substituir a anterior — se a alteração for inválida, nada muda.
     */
    public Orcamento alterarOrcamento(int versao, Orcamento.Alteracao alteracao, Usuario usuario) {
        Orcamento vigente = exigirOrcamentoAlteravel(usuario);
        if (vigente.getVersao() != versao) {
            throw new ConflitoException("O orçamento já está na versão " + vigente.getVersao()
                    + ". Recarregue a OS antes de alterar.");
        }
        if (dispensaNovaVersao(vigente)) {
            vigente.alterar(alteracao, usuario);
            return vigente;
        }
        Orcamento nova = Orcamento.novaVersaoDe(vigente);
        nova.alterar(alteracao, usuario);
        adotarNovaVersao(vigente, nova, usuario);
        return nova;
    }

    private Orcamento exigirOrcamentoAlteravel(Usuario usuario) {
        Permissoes.exigirBalcao(usuario, "alterar o orçamento");
        if (status.isFinal()) {
            throw new RegraDeNegocioException("OS " + status + " está finalizada e não pode ser alterada");
        }
        Orcamento vigente = getOrcamentoVigente();
        if (!dispensaNovaVersao(vigente)) {
            Transicao voltaParaAprovacao = status.transicaoPara(StatusOS.AGUARDANDO_APROVACAO)
                    .filter(Transicao::viaOrcamento)
                    .orElseThrow(() -> new RegraDeNegocioException(
                            "O orçamento não pode ser alterado com a OS em " + status));
            exigirExecutor(voltaParaAprovacao, usuario);
        }
        return vigente;
    }

    private boolean dispensaNovaVersao(Orcamento vigente) {
        return vigente.isPendente() && (status == StatusOS.ABERTA || status == StatusOS.AGUARDANDO_APROVACAO);
    }

    /** RN07: anterior vira SUBSTITUIDO, a nova passa a vigente e a OS volta a AGUARDANDO_APROVACAO. */
    private void adotarNovaVersao(Orcamento vigente, Orcamento nova, Usuario usuario) {
        vigente.substituir();
        orcamentos.add(nova);
        mudarPara(StatusOS.AGUARDANDO_APROVACAO, usuario);
    }

    /** Cliente aprovou: orçamento APROVADO (com data e usuário) e OS APROVADA. */
    public void aprovarOrcamento(int versao, Usuario usuario) {
        Orcamento orcamento = orcamentoEmDecisao(versao);
        Transicao transicao = transicaoPermitida(StatusOS.APROVADA);
        exigirExecutor(transicao, usuario);
        orcamento.aprovar(usuario, LocalDateTime.now());
        mudarPara(StatusOS.APROVADA, usuario);
    }

    /** Cliente recusou: orçamento RECUSADO e OS RECUSADA (final). */
    public void recusarOrcamento(int versao, Usuario usuario) {
        Orcamento orcamento = orcamentoEmDecisao(versao);
        Transicao transicao = transicaoPermitida(StatusOS.RECUSADA);
        exigirExecutor(transicao, usuario);
        orcamento.recusar(usuario, LocalDateTime.now());
        mudarPara(StatusOS.RECUSADA, usuario);
    }

    private Orcamento orcamentoEmDecisao(int versao) {
        Orcamento orcamento = getOrcamento(versao);
        if (orcamento != getOrcamentoVigente()) {
            throw new RegraDeNegocioException("Só a versão vigente do orçamento pode ser decidida");
        }
        if (!orcamento.isPendente()) {
            throw new RegraDeNegocioException("Orçamento já está " + orcamento.getSituacao());
        }
        return orcamento;
    }

    // Mecânico ----------------------------------------------------------------------

    public void atribuirMecanico(Usuario novoMecanico, Usuario usuario) {
        Objects.requireNonNull(usuario, "Usuário obrigatório");
        if (!usuario.isAdministrador()) {
            throw new OperacaoNaoPermitidaException("Somente o administrador pode atribuir mecânico");
        }
        if (status.isFinal()) {
            throw new RegraDeNegocioException("OS " + status + " está finalizada e não pode ser alterada");
        }
        if (novoMecanico.getPerfil() != Perfil.MECANICO || !novoMecanico.isAtivo()) {
            throw new RegraDeNegocioException("O usuário atribuído deve ser um mecânico ativo");
        }
        this.mecanico = novoMecanico;
    }

    public boolean isMecanicoAtribuido(Usuario usuario) {
        return mecanico != null && usuario != null && usuario.getPerfil() == Perfil.MECANICO
                && (mecanico == usuario || (mecanico.getId() != null && mecanico.getId().equals(usuario.getId())));
    }

    // Leitura -------------------------------------------------------------------

    public Long getId() {
        return id;
    }

    public Long getNumero() {
        return numero;
    }

    public Veiculo getVeiculo() {
        return veiculo;
    }

    public Usuario getMecanico() {
        return mecanico;
    }

    public String getProblemaRelatado() {
        return problemaRelatado;
    }

    public Integer getQuilometragem() {
        return quilometragem;
    }

    public StatusOS getStatus() {
        return status;
    }

    public LocalDateTime getDataAbertura() {
        return dataAbertura;
    }

    public LocalDateTime getDataEntrega() {
        return dataEntrega;
    }

    public Integer getVersion() {
        return version;
    }

    public List<Orcamento> getOrcamentos() {
        return Collections.unmodifiableList(orcamentos);
    }

    public List<HistoricoStatus> getHistorico() {
        return Collections.unmodifiableList(historico);
    }
}
