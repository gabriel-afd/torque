package br.com.torque.ordens;

public enum SituacaoOrcamento {
    PENDENTE("Pendente"),
    APROVADO("Aprovado"),
    RECUSADO("Recusado"),
    /** Orçamento aprovado que foi trocado por uma nova versão (RN07). */
    SUBSTITUIDO("Substituído");

    private final String rotulo;

    SituacaoOrcamento(String rotulo) {
        this.rotulo = rotulo;
    }

    /** Texto para documentos e mensagens ao cliente. */
    public String getRotulo() {
        return rotulo;
    }
}
