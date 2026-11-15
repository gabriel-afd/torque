package br.com.torque.relatorios;

import br.com.torque.relatorios.dto.PainelMesResponse;
import br.com.torque.shared.Dinheiro;
import java.time.LocalDateTime;
import java.time.YearMonth;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RelatorioService {

    private final PainelRepository painel;

    public RelatorioService(PainelRepository painel) {
        this.painel = painel;
    }

    @Transactional(readOnly = true)
    public PainelMesResponse painelDoMes(int ano, int mes) {
        YearMonth periodo = YearMonth.of(ano, mes);
        LocalDateTime inicio = periodo.atDay(1).atStartOfDay();
        LocalDateTime fim = periodo.plusMonths(1).atDay(1).atStartOfDay();
        return new PainelMesResponse(ano, mes,
                painel.abertas(inicio, fim),
                painel.concluidas(inicio, fim),
                painel.entregues(inicio, fim),
                Dinheiro.de(painel.faturamento(inicio, fim)));
    }
}
