package br.com.torque.clientes;

import br.com.torque.clientes.dto.VeiculoComDonoResponse;
import br.com.torque.clientes.dto.VeiculoRequest;
import br.com.torque.clientes.dto.VeiculoResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/veiculos")
@Tag(name = "Veículos")
public class VeiculoController {

    private final VeiculoService service;

    public VeiculoController(VeiculoService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Busca rápida por placa (aceita minúsculas e hífen); 404 se não existir")
    public VeiculoComDonoResponse buscarPorPlaca(@RequestParam @NotBlank String placa) {
        return service.buscarPorPlaca(placa);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ATENDENTE', 'ADMINISTRADOR')")
    public VeiculoResponse atualizar(@PathVariable Long id, @Valid @RequestBody VeiculoRequest request) {
        return service.atualizar(id, request);
    }
}
