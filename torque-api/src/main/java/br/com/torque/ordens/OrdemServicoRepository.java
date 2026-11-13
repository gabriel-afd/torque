package br.com.torque.ordens;

import br.com.torque.ordens.dto.ConsultasOrdem.TotalVigente;
import br.com.torque.ordens.dto.ConsultasOrdem.UltimaMudanca;
import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface OrdemServicoRepository extends JpaRepository<OrdemServico, Long>,
        JpaSpecificationExecutor<OrdemServico> {

    /** Quadro: veículo, dono e mecânico na mesma consulta (o count não recebe o fetch). */
    @Override
    @EntityGraph(attributePaths = {"veiculo", "veiculo.cliente", "mecanico"})
    Page<OrdemServico> findAll(Specification<OrdemServico> spec, Pageable pageable);

    @EntityGraph(attributePaths = {"veiculo", "veiculo.cliente", "mecanico"})
    @Query("select o from OrdemServico o where o.id = :id")
    Optional<OrdemServico> buscarDetalhe(Long id);

    /**
     * Carrega a OS para alteração incrementando a versão no commit mesmo que só o orçamento mude:
     * duas edições concorrentes da mesma OS nunca passam juntas (a segunda recebe 409).
     */
    @Lock(LockModeType.OPTIMISTIC_FORCE_INCREMENT)
    @Query("select o from OrdemServico o where o.id = :id")
    Optional<OrdemServico> buscarParaAlteracao(Long id);

    @EntityGraph(attributePaths = {"mecanico"})
    Page<OrdemServico> findByVeiculoId(Long veiculoId, Pageable pageable);

    @Query("""
            select new br.com.torque.ordens.dto.ConsultasOrdem$TotalVigente(o.ordemServico.id, o.total)
            from Orcamento o
            where o.ordemServico.id in :ids
              and o.versao = (select max(o2.versao) from Orcamento o2 where o2.ordemServico = o.ordemServico)
            """)
    List<TotalVigente> totaisVigentes(Collection<Long> ids);

    @Query("""
            select new br.com.torque.ordens.dto.ConsultasOrdem$UltimaMudanca(h.ordemServico.id, max(h.dataHora))
            from HistoricoStatus h
            where h.ordemServico.id in :ids
            group by h.ordemServico.id
            """)
    List<UltimaMudanca> ultimasMudancas(Collection<Long> ids);

    /** Orçamentos vigentes com os itens de serviço, para o histórico do veículo. */
    @Query("""
            select distinct o from Orcamento o
            left join fetch o.itensServico
            where o.ordemServico.id in :ids
              and o.versao = (select max(o2.versao) from Orcamento o2 where o2.ordemServico = o.ordemServico)
            """)
    List<Orcamento> vigentesComServicos(Collection<Long> ids);

    @Query("select h from HistoricoStatus h join fetch h.usuario where h.ordemServico.id = :id order by h.id")
    List<HistoricoStatus> historicoComUsuarios(Long id);
}
