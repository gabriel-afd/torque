package br.com.torque.relatorios;

import br.com.torque.relatorios.dto.PainelMesResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/relatorios")
@Tag(name = "Relatórios")
public class RelatorioController {

    private final RelatorioService service;

    public RelatorioController(RelatorioService service) {
        this.service = service;
    }

    @GetMapping("/mes")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Operation(summary = "Painel do mês: OS abertas, concluídas, entregues e faturamento")
    public PainelMesResponse mes(@RequestParam @Min(2000) @Max(2100) int ano,
                                 @RequestParam @Min(1) @Max(12) int mes) {
        return service.painelDoMes(ano, mes);
    }
}
