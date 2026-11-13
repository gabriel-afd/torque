package br.com.torque.catalogo;

import br.com.torque.auth.UsuarioAutenticado;
import br.com.torque.catalogo.dto.ServicoRequest;
import br.com.torque.catalogo.dto.ServicoResponse;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
@RequestMapping("/api/servicos")
@Tag(name = "Catálogo — serviços")
public class ServicoController {

    private final CatalogoService service;

    public ServicoController(CatalogoService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public Page<ServicoResponse> listar(
            @RequestParam(required = false) String busca,
            @Parameter(description = "Só tem efeito para ADMINISTRADOR")
            @RequestParam(defaultValue = "false") boolean incluirInativos,
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @PageableDefault(sort = "descricao", direction = Sort.Direction.ASC) Pageable pageable) {
        return service.listarServicos(busca, incluirInativos, usuario, pageable);
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ServicoResponse buscar(@PathVariable Long id) {
        return service.buscarServico(id);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<ServicoResponse> cadastrar(@Valid @RequestBody ServicoRequest request) {
        ServicoResponse criado = service.cadastrarServico(request);
        return ResponseEntity.created(URI.create("/api/servicos/" + criado.id())).body(criado);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ServicoResponse atualizar(@PathVariable Long id, @Valid @RequestBody ServicoRequest request) {
        return service.atualizarServico(id, request);
    }

    @PatchMapping("/{id}/inativar")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ServicoResponse inativar(@PathVariable Long id) {
        return service.inativarServico(id);
    }
}
