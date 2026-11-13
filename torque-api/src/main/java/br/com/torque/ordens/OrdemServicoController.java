package br.com.torque.ordens;

import br.com.torque.auth.UsuarioAutenticado;
import br.com.torque.ordens.dto.AberturaOsRequest;
import br.com.torque.ordens.dto.MecanicoRequest;
import br.com.torque.ordens.dto.OrcamentoRequest;
import br.com.torque.ordens.dto.OrdemResumoResponse;
import br.com.torque.ordens.dto.OrdemServicoResponse;
import br.com.torque.ordens.dto.StatusRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
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
@RequestMapping("/api/ordens")
@Tag(name = "Ordens de serviço")
public class OrdemServicoController {

    private final OrdemServicoService service;

    public OrdemServicoController(OrdemServicoService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Quadro de OS (MECANICO vê só as suas)")
    public Page<OrdemResumoResponse> listar(
            @Parameter(description = "Um ou mais status") @RequestParam(required = false) List<StatusOS> status,
            @Parameter(description = "Id do mecânico") @RequestParam(required = false) Long mecanico,
            @Parameter(description = "Parte da placa ou do nome do cliente") @RequestParam(required = false) String busca,
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @PageableDefault(size = 50, sort = "dataAbertura", direction = Sort.Direction.DESC) Pageable pageable) {
        return service.listar(status, mecanico, busca, usuario, pageable);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ATENDENTE', 'ADMINISTRADOR')")
    @Operation(summary = "Abre a OS com orçamento versão 1 vazio")
    public ResponseEntity<OrdemServicoResponse> abrir(@Valid @RequestBody AberturaOsRequest request,
                                                      @AuthenticationPrincipal UsuarioAutenticado usuario) {
        OrdemServicoResponse criada = service.abrir(request, usuario);
        return ResponseEntity.created(URI.create("/api/ordens/" + criada.id())).body(criada);
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "OS com orçamento vigente e histórico")
    public OrdemServicoResponse detalhar(@PathVariable Long id) {
        return service.detalhar(id);
    }

    @PutMapping("/{id}/orcamentos/{versao}")
    @PreAuthorize("hasAnyRole('ATENDENTE', 'ADMINISTRADOR')")
    @Operation(summary = "Altera itens e desconto; orçamento aprovado gera nova versão (RN07)")
    public OrdemServicoResponse alterarOrcamento(@PathVariable Long id, @PathVariable int versao,
                                                 @Valid @RequestBody OrcamentoRequest request,
                                                 @AuthenticationPrincipal UsuarioAutenticado usuario) {
        return service.alterarOrcamento(id, versao, request, usuario);
    }

    @PostMapping("/{id}/orcamentos/{versao}/aprovacao")
    @PreAuthorize("hasAnyRole('ATENDENTE', 'ADMINISTRADOR')")
    @Operation(summary = "Registra a aprovação do cliente")
    public OrdemServicoResponse aprovar(@PathVariable Long id, @PathVariable int versao,
                                        @AuthenticationPrincipal UsuarioAutenticado usuario) {
        return service.aprovarOrcamento(id, versao, usuario);
    }

    @PostMapping("/{id}/orcamentos/{versao}/recusa")
    @PreAuthorize("hasAnyRole('ATENDENTE', 'ADMINISTRADOR')")
    @Operation(summary = "Registra a recusa do cliente (OS RECUSADA)")
    public OrdemServicoResponse recusar(@PathVariable Long id, @PathVariable int versao,
                                        @AuthenticationPrincipal UsuarioAutenticado usuario) {
        return service.recusarOrcamento(id, versao, usuario);
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ATENDENTE', 'ADMINISTRADOR', 'MECANICO')")
    @Operation(summary = "Muda o status conforme a tabela de transições (perfil validado no domínio)")
    public OrdemServicoResponse alterarStatus(@PathVariable Long id, @Valid @RequestBody StatusRequest request,
                                              @AuthenticationPrincipal UsuarioAutenticado usuario) {
        return service.alterarStatus(id, request, usuario);
    }

    @PatchMapping("/{id}/mecanico")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Operation(summary = "Atribui o mecânico responsável")
    public OrdemServicoResponse atribuirMecanico(@PathVariable Long id, @Valid @RequestBody MecanicoRequest request,
                                                 @AuthenticationPrincipal UsuarioAutenticado usuario) {
        return service.atribuirMecanico(id, request, usuario);
    }
}
