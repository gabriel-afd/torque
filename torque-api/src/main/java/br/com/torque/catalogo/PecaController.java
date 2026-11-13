package br.com.torque.catalogo;

import br.com.torque.auth.UsuarioAutenticado;
import br.com.torque.catalogo.dto.PecaRequest;
import br.com.torque.catalogo.dto.PecaResponse;
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
@RequestMapping("/api/pecas")
@Tag(name = "Catálogo — peças")
public class PecaController {

    private final CatalogoService service;

    public PecaController(CatalogoService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public Page<PecaResponse> listar(
            @Parameter(description = "Parte da descrição ou do código")
            @RequestParam(required = false) String busca,
            @Parameter(description = "Só tem efeito para ADMINISTRADOR")
            @RequestParam(defaultValue = "false") boolean incluirInativos,
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @PageableDefault(sort = "descricao", direction = Sort.Direction.ASC) Pageable pageable) {
        return service.listarPecas(busca, incluirInativos, usuario, pageable);
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public PecaResponse buscar(@PathVariable Long id) {
        return service.buscarPeca(id);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<PecaResponse> cadastrar(@Valid @RequestBody PecaRequest request) {
        PecaResponse criada = service.cadastrarPeca(request);
        return ResponseEntity.created(URI.create("/api/pecas/" + criada.id())).body(criada);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public PecaResponse atualizar(@PathVariable Long id, @Valid @RequestBody PecaRequest request) {
        return service.atualizarPeca(id, request);
    }

    @PatchMapping("/{id}/inativar")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public PecaResponse inativar(@PathVariable Long id) {
        return service.inativarPeca(id);
    }
}
