package br.com.torque.clientes;

import br.com.torque.clientes.dto.ClienteDetalheResponse;
import br.com.torque.clientes.dto.ClienteRequest;
import br.com.torque.clientes.dto.ClienteResponse;
import br.com.torque.clientes.dto.VeiculoRequest;
import br.com.torque.clientes.dto.VeiculoResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/clientes")
@Tag(name = "Clientes")
public class ClienteController {

    private final ClienteService service;

    public ClienteController(ClienteService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ATENDENTE', 'ADMINISTRADOR')")
    public Page<ClienteResponse> listar(@RequestParam(required = false) String busca,
                                        @PageableDefault(sort = "nome", direction = Sort.Direction.ASC) Pageable pageable) {
        return service.listar(busca, pageable);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ATENDENTE', 'ADMINISTRADOR')")
    public ClienteDetalheResponse buscar(@PathVariable Long id) {
        return service.buscar(id);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ATENDENTE', 'ADMINISTRADOR')")
    public ResponseEntity<ClienteResponse> cadastrar(@Valid @RequestBody ClienteRequest request) {
        ClienteResponse criado = service.cadastrar(request);
        return ResponseEntity.created(URI.create("/api/clientes/" + criado.id())).body(criado);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ATENDENTE', 'ADMINISTRADOR')")
    public ClienteResponse atualizar(@PathVariable Long id, @Valid @RequestBody ClienteRequest request) {
        return service.atualizar(id, request);
    }

    @PatchMapping("/{id}/inativar")
    @PreAuthorize("hasAnyRole('ATENDENTE', 'ADMINISTRADOR')")
    public ClienteResponse inativar(@PathVariable Long id) {
        return service.inativar(id);
    }

    @PostMapping("/{id}/veiculos")
    @PreAuthorize("hasAnyRole('ATENDENTE', 'ADMINISTRADOR')")
    public ResponseEntity<VeiculoResponse> cadastrarVeiculo(@PathVariable Long id,
                                                            @Valid @RequestBody VeiculoRequest request) {
        VeiculoResponse criado = service.cadastrarVeiculo(id, request);
        return ResponseEntity.created(URI.create("/api/veiculos/" + criado.id())).body(criado);
    }
}
