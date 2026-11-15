package br.com.torque.ordens;

import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Status da ordem de serviço. A tabela de transições do CLAUDE.md está representada
 * <b>somente aqui</b>, no mapa {@link #TRANSICOES}.
 */
public enum StatusOS {
    ABERTA,
    AGUARDANDO_APROVACAO,
    APROVADA,
    EM_EXECUCAO,
    AGUARDANDO_PECA,
    CONCLUIDA,
    ENTREGUE,
    RECUSADA,
    CANCELADA;

    /** Quem pode executar cada transição. */
    public enum Executor {
        /** ATENDENTE ou ADMINISTRADOR. */
        BALCAO,
        /** Mecânico atribuído à OS ou ADMINISTRADOR (RN09). */
        MECANICO_ATRIBUIDO
    }

    /**
     * @param viaOrcamento a transição só acontece pelo fluxo do orçamento (aprovação, recusa ou
     *                     nova versão — RN07), nunca pela mudança de status direta
     */
    public record Transicao(StatusOS de, StatusOS para, Executor executor, boolean viaOrcamento) {
    }

    private static final Map<StatusOS, Map<StatusOS, Transicao>> TRANSICOES = new EnumMap<>(StatusOS.class);

    static {
        permitir(ABERTA, AGUARDANDO_APROVACAO, Executor.BALCAO, false);
        permitir(ABERTA, CANCELADA, Executor.BALCAO, false);

        permitir(AGUARDANDO_APROVACAO, APROVADA, Executor.BALCAO, true);
        permitir(AGUARDANDO_APROVACAO, RECUSADA, Executor.BALCAO, true);
        permitir(AGUARDANDO_APROVACAO, CANCELADA, Executor.BALCAO, false);

        permitir(APROVADA, EM_EXECUCAO, Executor.MECANICO_ATRIBUIDO, false);
        permitir(APROVADA, AGUARDANDO_APROVACAO, Executor.BALCAO, true);
        permitir(APROVADA, CANCELADA, Executor.BALCAO, false);

        permitir(EM_EXECUCAO, AGUARDANDO_PECA, Executor.MECANICO_ATRIBUIDO, false);
        permitir(EM_EXECUCAO, CONCLUIDA, Executor.MECANICO_ATRIBUIDO, false);
        permitir(EM_EXECUCAO, AGUARDANDO_APROVACAO, Executor.BALCAO, true);
        permitir(EM_EXECUCAO, CANCELADA, Executor.BALCAO, false);

        permitir(AGUARDANDO_PECA, EM_EXECUCAO, Executor.MECANICO_ATRIBUIDO, false);
        permitir(AGUARDANDO_PECA, CANCELADA, Executor.BALCAO, false);

        permitir(CONCLUIDA, ENTREGUE, Executor.BALCAO, false);
        permitir(CONCLUIDA, CANCELADA, Executor.BALCAO, false);
    }

    private static void permitir(StatusOS de, StatusOS para, Executor executor, boolean viaOrcamento) {
        TRANSICOES.computeIfAbsent(de, s -> new EnumMap<>(StatusOS.class))
                .put(para, new Transicao(de, para, executor, viaOrcamento));
    }

    public Optional<Transicao> transicaoPara(StatusOS destino) {
        return Optional.ofNullable(TRANSICOES.getOrDefault(this, Map.of()).get(destino));
    }

    public Set<StatusOS> destinosPermitidos() {
        return Collections.unmodifiableSet(TRANSICOES.getOrDefault(this, Map.of()).keySet());
    }

    /** RN08: status finais não mudam mais. */
    public boolean isFinal() {
        return this == ENTREGUE || this == RECUSADA || this == CANCELADA;
    }

    /** Todas as transições da tabela (usado nos testes parametrizados e na documentação). */
    public static List<Transicao> todasAsTransicoes() {
        return TRANSICOES.values().stream().flatMap(m -> m.values().stream()).toList();
    }
}
