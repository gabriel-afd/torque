package br.com.torque.ordens.dto;

import br.com.torque.ordens.Orcamento;
import br.com.torque.ordens.OrcamentoItemServico;
import br.com.torque.ordens.OrdemServico;
import br.com.torque.ordens.StatusOS;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** Uma OS no histórico do veículo (HU09). {@code total} e {@code servicos} vêm do orçamento vigente. */
public record HistoricoVeiculoResponse(Long id, Long numero, StatusOS status, LocalDateTime dataAbertura,
                                       LocalDateTime dataEntrega, Integer quilometragem, String problemaRelatado,
                                       List<String> servicos, BigDecimal total) {

    public static HistoricoVeiculoResponse de(OrdemServico os, Orcamento vigente) {
        return new HistoricoVeiculoResponse(os.getId(), os.getNumero(), os.getStatus(), os.getDataAbertura(),
                os.getDataEntrega(), os.getQuilometragem(), os.getProblemaRelatado(),
                vigente == null ? List.of()
                        : vigente.getItensServico().stream().map(OrcamentoItemServico::getDescricao).toList(),
                vigente == null ? null : vigente.getTotal());
    }
}
