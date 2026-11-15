package br.com.torque.ordens;

import br.com.torque.ordens.dto.HistoricoVeiculoResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.SortDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/** Fica no módulo de ordens: {@code clientes} não depende de {@code ordens}. */
@RestController
@Tag(name = "Veículos")
public class HistoricoVeiculoController {

    private final OrdemServicoService service;

    public HistoricoVeiculoController(OrdemServicoService service) {
        this.service = service;
    }

    @GetMapping("/api/veiculos/{id}/ordens")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Histórico de manutenção do veículo, da OS mais recente para a mais antiga")
    public Page<HistoricoVeiculoResponse> historico(
            @PathVariable Long id,
            @SortDefault.SortDefaults({
                    @SortDefault(sort = "dataAbertura", direction = Sort.Direction.DESC),
                    @SortDefault(sort = "id", direction = Sort.Direction.DESC)}) Pageable pageable) {
        return service.historicoDoVeiculo(id, pageable);
    }
}
