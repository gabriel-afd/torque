package br.com.torque.ordens;

import br.com.torque.auth.Perfil;
import br.com.torque.auth.Usuario;
import br.com.torque.auth.UsuarioAutenticado;
import br.com.torque.auth.UsuarioRepository;
import br.com.torque.catalogo.Peca;
import br.com.torque.catalogo.PecaRepository;
import br.com.torque.catalogo.Servico;
import br.com.torque.catalogo.ServicoRepository;
import br.com.torque.clientes.Veiculo;
import br.com.torque.clientes.VeiculoRepository;
import br.com.torque.ordens.dto.AberturaOsRequest;
import br.com.torque.ordens.dto.ConsultasOrdem.TotalVigente;
import br.com.torque.ordens.dto.ConsultasOrdem.UltimaMudanca;
import br.com.torque.ordens.dto.HistoricoVeiculoResponse;
import br.com.torque.ordens.dto.MecanicoRequest;
import br.com.torque.ordens.dto.OrcamentoRequest;
import br.com.torque.ordens.dto.OrdemResumoResponse;
import br.com.torque.ordens.dto.OrdemServicoResponse;
import br.com.torque.ordens.dto.StatusRequest;
import br.com.torque.shared.excecao.ConflitoException;
import br.com.torque.shared.excecao.RecursoNaoEncontradoException;
import br.com.torque.shared.excecao.RegraDeNegocioException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Casos de uso da OS. Só carrega dados, chama o domínio e monta respostas: nenhuma regra de negócio aqui.
 *
 * <p>As alterações rodam numa transação e a resposta é montada numa segunda (somente leitura), depois do
 * commit, para devolver a {@code version} já incrementada.
 */
@Service
public class OrdemServicoService {

    static final String MENSAGEM_CONFLITO_VERSAO =
            "Esta OS foi alterada por outro usuário. Recarregue a página e tente novamente.";

    private final OrdemServicoRepository ordens;
    private final VeiculoRepository veiculos;
    private final UsuarioRepository usuarios;
    private final ServicoRepository servicos;
    private final PecaRepository pecas;
    private final TransactionTemplate escrita;
    private final TransactionTemplate leitura;

    public OrdemServicoService(OrdemServicoRepository ordens, VeiculoRepository veiculos, UsuarioRepository usuarios,
                               ServicoRepository servicos, PecaRepository pecas,
                               PlatformTransactionManager transacoes) {
        this.ordens = ordens;
        this.veiculos = veiculos;
        this.usuarios = usuarios;
        this.servicos = servicos;
        this.pecas = pecas;
        this.escrita = new TransactionTemplate(transacoes);
        this.leitura = new TransactionTemplate(transacoes);
        this.leitura.setReadOnly(true);
    }

    // Consultas -------------------------------------------------------------------

    /** Quadro de OS. MECANICO vê só as próprias (o filtro de mecânico enviado é ignorado). */
    public Page<OrdemResumoResponse> listar(Collection<StatusOS> status, Long mecanicoId, String busca,
                                            UsuarioAutenticado usuario, Pageable pageable) {
        Long mecanico = usuario.perfil() == Perfil.MECANICO ? usuario.id() : mecanicoId;
        return leitura.execute(tx -> {
            Page<OrdemServico> pagina = ordens.findAll(FiltrosOrdem.de(status, mecanico, busca), pageable);
            List<Long> ids = pagina.map(OrdemServico::getId).toList();
            if (ids.isEmpty()) {
                return pagina.map(os -> OrdemResumoResponse.de(os, null, null));
            }
            Map<Long, BigDecimal> totais = ordens.totaisVigentes(ids).stream()
                    .collect(Collectors.toMap(TotalVigente::ordemId, TotalVigente::total));
            Map<Long, LocalDateTime> mudancas = ordens.ultimasMudancas(ids).stream()
                    .collect(Collectors.toMap(UltimaMudanca::ordemId, UltimaMudanca::dataHora));
            return pagina.map(os -> OrdemResumoResponse.de(os, totais.get(os.getId()), mudancas.get(os.getId())));
        });
    }

    public OrdemServicoResponse detalhar(Long id) {
        return leitura.execute(tx -> {
            OrdemServico os = ordens.buscarDetalhe(id).orElseThrow(() -> naoEncontrada(id));
            return OrdemServicoResponse.de(os, ordens.historicoComUsuarios(id));
        });
    }

    /** HU09: OS do veículo, da mais recente para a mais antiga. */
    public Page<HistoricoVeiculoResponse> historicoDoVeiculo(Long veiculoId, Pageable pageable) {
        return leitura.execute(tx -> {
            if (!veiculos.existsById(veiculoId)) {
                throw new RecursoNaoEncontradoException("Veículo não encontrado: " + veiculoId);
            }
            Page<OrdemServico> pagina = ordens.findByVeiculoId(veiculoId, pageable);
            List<Long> ids = pagina.map(OrdemServico::getId).toList();
            Map<Long, Orcamento> vigentes = ids.isEmpty() ? Map.of()
                    : ordens.vigentesComServicos(ids).stream()
                    .collect(Collectors.toMap(o -> o.getOrdemServico().getId(), Function.identity()));
            return pagina.map(os -> HistoricoVeiculoResponse.de(os, vigentes.get(os.getId())));
        });
    }

    // Alterações ------------------------------------------------------------------

    public OrdemServicoResponse abrir(AberturaOsRequest request, UsuarioAutenticado autenticado) {
        Long id = escrita.execute(tx -> {
            Veiculo veiculo = veiculos.findById(request.veiculoId())
                    .orElseThrow(() -> new RecursoNaoEncontradoException("Veículo não encontrado: " + request.veiculoId()));
            OrdemServico os = OrdemServico.abrir(veiculo, request.problemaRelatado(), request.quilometragem(),
                    usuario(autenticado));
            return ordens.saveAndFlush(os).getId();
        });
        return detalhar(id);
    }

    public OrdemServicoResponse alterarOrcamento(Long id, int versao, OrcamentoRequest request,
                                                 UsuarioAutenticado autenticado) {
        return alterar(id, request.versaoOs(), autenticado,
                (os, usuario) -> os.alterarOrcamento(versao, montarAlteracao(request), usuario));
    }

    public OrdemServicoResponse aprovarOrcamento(Long id, int versao, UsuarioAutenticado autenticado) {
        return alterar(id, null, autenticado, (os, usuario) -> os.aprovarOrcamento(versao, usuario));
    }

    public OrdemServicoResponse recusarOrcamento(Long id, int versao, UsuarioAutenticado autenticado) {
        return alterar(id, null, autenticado, (os, usuario) -> os.recusarOrcamento(versao, usuario));
    }

    public OrdemServicoResponse alterarStatus(Long id, StatusRequest request, UsuarioAutenticado autenticado) {
        return alterar(id, request.versaoOs(), autenticado, (os, usuario) -> os.alterarStatus(request.status(), usuario));
    }

    public OrdemServicoResponse atribuirMecanico(Long id, MecanicoRequest request, UsuarioAutenticado autenticado) {
        return alterar(id, request.versaoOs(), autenticado, (os, usuario) -> {
            Usuario mecanico = usuarios.findById(request.mecanicoId())
                    .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário não encontrado: " + request.mecanicoId()));
            os.atribuirMecanico(mecanico, usuario);
        });
    }

    private OrdemServicoResponse alterar(Long id, Integer versaoOs, UsuarioAutenticado autenticado,
                                         BiConsumer<OrdemServico, Usuario> acao) {
        escrita.executeWithoutResult(tx -> {
            OrdemServico os = ordens.buscarParaAlteracao(id).orElseThrow(() -> naoEncontrada(id));
            if (versaoOs != null && !versaoOs.equals(os.getVersion())) {
                throw new ConflitoException(MENSAGEM_CONFLITO_VERSAO);
            }
            acao.accept(os, usuario(autenticado));
        });
        return detalhar(id);
    }

    /** Converte o request em {@link Orcamento.Alteracao}, buscando no catálogo só os itens novos. */
    private Orcamento.Alteracao montarAlteracao(OrcamentoRequest request) {
        Set<Long> servicosMantidos = new HashSet<>();
        Map<Long, Integer> pecasMantidas = new HashMap<>();
        for (OrcamentoRequest.ItemServico item : request.servicos()) {
            if (item.itemId() != null && !servicosMantidos.add(item.itemId())) {
                throw new RegraDeNegocioException("Item de serviço repetido: " + item.itemId());
            }
        }
        for (OrcamentoRequest.ItemPeca item : request.pecas()) {
            if (item.itemId() != null && pecasMantidas.put(item.itemId(), item.quantidade()) != null) {
                throw new RegraDeNegocioException("Item de peça repetido: " + item.itemId());
            }
        }
        Map<Long, Servico> catalogoServicos = carregar(servicos::findAllById,
                request.servicos().stream().map(OrcamentoRequest.ItemServico::servicoId), Servico::getId, "Serviço");
        Map<Long, Peca> catalogoPecas = carregar(pecas::findAllById,
                request.pecas().stream().map(OrcamentoRequest.ItemPeca::pecaId), Peca::getId, "Peça");

        List<Servico> servicosNovos = request.servicos().stream()
                .filter(i -> i.servicoId() != null)
                .map(i -> catalogoServicos.get(i.servicoId()))
                .toList();
        List<Orcamento.PecaNova> pecasNovas = request.pecas().stream()
                .filter(i -> i.pecaId() != null)
                .map(i -> new Orcamento.PecaNova(catalogoPecas.get(i.pecaId()), i.quantidade()))
                .toList();
        return new Orcamento.Alteracao(servicosMantidos, pecasMantidas, servicosNovos, pecasNovas, request.desconto());
    }

    private static <T> Map<Long, T> carregar(Function<Set<Long>, List<T>> busca, Stream<Long> ids,
                                             Function<T, Long> id, String nome) {
        Set<Long> pedidos = ids.filter(Objects::nonNull).collect(Collectors.toSet());
        if (pedidos.isEmpty()) {
            return Map.of();
        }
        Map<Long, T> encontrados = busca.apply(pedidos).stream().collect(Collectors.toMap(id, Function.identity()));
        pedidos.stream().filter(p -> !encontrados.containsKey(p)).findFirst().ifPresent(p -> {
            throw new RecursoNaoEncontradoException(nome + " não encontrado(a) no catálogo: " + p);
        });
        return encontrados;
    }

    private Usuario usuario(UsuarioAutenticado autenticado) {
        return usuarios.findById(autenticado.id())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário não encontrado: " + autenticado.id()));
    }

    private static RecursoNaoEncontradoException naoEncontrada(Long id) {
        return new RecursoNaoEncontradoException("Ordem de serviço não encontrada: " + id);
    }
}
