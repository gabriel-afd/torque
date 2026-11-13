package br.com.torque.ordens.dto;

import br.com.torque.auth.Usuario;
import br.com.torque.clientes.Veiculo;
import br.com.torque.ordens.OrdemServico;
import br.com.torque.ordens.StatusOS;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Cartão do quadro de OS.
 *
 * @param total      total do orçamento vigente
 * @param dataStatus quando a OS entrou no status atual
 */
public record OrdemResumoResponse(Long id, Long numero, StatusOS status, String placa, String veiculo,
                                  Long clienteId, String cliente, Long mecanicoId, String mecanico,
                                  BigDecimal total, LocalDateTime dataAbertura, LocalDateTime dataStatus) {

    public static OrdemResumoResponse de(OrdemServico os, BigDecimal total, LocalDateTime dataStatus) {
        Veiculo v = os.getVeiculo();
        Usuario m = os.getMecanico();
        return new OrdemResumoResponse(os.getId(), os.getNumero(), os.getStatus(), v.getPlaca(), v.getDescricao(),
                v.getCliente().getId(), v.getCliente().getNome(),
                m == null ? null : m.getId(), m == null ? null : m.getNome(),
                total, os.getDataAbertura(), dataStatus);
    }
}
