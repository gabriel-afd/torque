package br.com.torque.ordens;

public enum SituacaoOrcamento {
    PENDENTE,
    APROVADO,
    RECUSADO,
    /** Orçamento aprovado que foi trocado por uma nova versão (RN07). */
    SUBSTITUIDO
}
