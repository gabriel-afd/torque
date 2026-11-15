package br.com.torque.relatorios;

import br.com.torque.ordens.SituacaoOrcamento;
import br.com.torque.ordens.StatusOS;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.springframework.stereotype.Repository;

/** Consultas agregadas (somente leitura) do painel do mês. Período sempre [inicio, fim). */
@Repository
public class PainelRepository {

    private final EntityManager em;

    public PainelRepository(EntityManager em) {
        this.em = em;
    }

    public long abertas(LocalDateTime inicio, LocalDateTime fim) {
        return em.createQuery("""
                        select count(o) from OrdemServico o
                        where o.dataAbertura >= :inicio and o.dataAbertura < :fim
                        """, Long.class)
                .setParameter("inicio", inicio).setParameter("fim", fim)
                .getSingleResult();
    }

    /** OS que passaram para CONCLUIDA no período (cada OS conta uma vez). */
    public long concluidas(LocalDateTime inicio, LocalDateTime fim) {
        return em.createQuery("""
                        select count(distinct h.ordemServico.id) from HistoricoStatus h
                        where h.statusNovo = :concluida and h.dataHora >= :inicio and h.dataHora < :fim
                        """, Long.class)
                .setParameter("concluida", StatusOS.CONCLUIDA)
                .setParameter("inicio", inicio).setParameter("fim", fim)
                .getSingleResult();
    }

    public long entregues(LocalDateTime inicio, LocalDateTime fim) {
        return em.createQuery("""
                        select count(o) from OrdemServico o
                        where o.status = :entregue and o.dataEntrega >= :inicio and o.dataEntrega < :fim
                        """, Long.class)
                .setParameter("entregue", StatusOS.ENTREGUE)
                .setParameter("inicio", inicio).setParameter("fim", fim)
                .getSingleResult();
    }

    /** Soma dos totais dos orçamentos aprovados das OS entregues no período. */
    public BigDecimal faturamento(LocalDateTime inicio, LocalDateTime fim) {
        return em.createQuery("""
                        select coalesce(sum(orc.total), 0) from Orcamento orc
                        where orc.situacao = :aprovado
                          and orc.ordemServico.status = :entregue
                          and orc.ordemServico.dataEntrega >= :inicio and orc.ordemServico.dataEntrega < :fim
                        """, BigDecimal.class)
                .setParameter("aprovado", SituacaoOrcamento.APROVADO)
                .setParameter("entregue", StatusOS.ENTREGUE)
                .setParameter("inicio", inicio).setParameter("fim", fim)
                .getSingleResult();
    }
}
