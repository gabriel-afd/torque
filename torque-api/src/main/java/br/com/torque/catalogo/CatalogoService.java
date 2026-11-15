package br.com.torque.catalogo;

import br.com.torque.auth.UsuarioAutenticado;
import br.com.torque.catalogo.dto.PecaRequest;
import br.com.torque.catalogo.dto.PecaResponse;
import br.com.torque.catalogo.dto.ServicoRequest;
import br.com.torque.catalogo.dto.ServicoResponse;
import br.com.torque.shared.Busca;
import br.com.torque.shared.excecao.ConflitoException;
import br.com.torque.shared.excecao.RecursoNaoEncontradoException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Casos de uso do catálogo de serviços e peças. */
@Service
public class CatalogoService {

    static final String MENSAGEM_CODIGO_DUPLICADO = "Já existe uma peça com este código";

    private final ServicoRepository servicos;
    private final PecaRepository pecas;

    public CatalogoService(ServicoRepository servicos, PecaRepository pecas) {
        this.servicos = servicos;
        this.pecas = pecas;
    }

    /** Itens inativos só aparecem para o administrador que pedir explicitamente. */
    static boolean deveIncluirInativos(boolean pedido, UsuarioAutenticado usuario) {
        return pedido && usuario != null && usuario.isAdministrador();
    }

    // Serviços --------------------------------------------------------------

    @Transactional(readOnly = true)
    public Page<ServicoResponse> listarServicos(String busca, boolean incluirInativos, UsuarioAutenticado usuario,
                                                Pageable pageable) {
        return servicos.buscar(Busca.contem(busca), deveIncluirInativos(incluirInativos, usuario), pageable)
                .map(ServicoResponse::de);
    }

    @Transactional(readOnly = true)
    public ServicoResponse buscarServico(Long id) {
        return ServicoResponse.de(carregarServico(id));
    }

    @Transactional
    public ServicoResponse cadastrarServico(ServicoRequest request) {
        return ServicoResponse.de(servicos.save(new Servico(request.descricao(), request.precoMaoDeObra())));
    }

    @Transactional
    public ServicoResponse atualizarServico(Long id, ServicoRequest request) {
        Servico servico = carregarServico(id);
        servico.atualizar(request.descricao(), request.precoMaoDeObra());
        return ServicoResponse.de(servico);
    }

    @Transactional
    public ServicoResponse inativarServico(Long id) {
        Servico servico = carregarServico(id);
        servico.inativar();
        return ServicoResponse.de(servico);
    }

    private Servico carregarServico(Long id) {
        return servicos.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Serviço não encontrado: " + id));
    }

    // Peças -----------------------------------------------------------------

    @Transactional(readOnly = true)
    public Page<PecaResponse> listarPecas(String busca, boolean incluirInativos, UsuarioAutenticado usuario,
                                          Pageable pageable) {
        return pecas.buscar(Busca.contem(busca), deveIncluirInativos(incluirInativos, usuario), pageable)
                .map(PecaResponse::de);
    }

    @Transactional(readOnly = true)
    public PecaResponse buscarPeca(Long id) {
        return PecaResponse.de(carregarPeca(id));
    }

    @Transactional
    public PecaResponse cadastrarPeca(PecaRequest request) {
        Peca peca = new Peca(request.codigo(), request.descricao(), request.precoVenda());
        if (pecas.existsByCodigo(peca.getCodigo())) {
            throw new ConflitoException(MENSAGEM_CODIGO_DUPLICADO);
        }
        return PecaResponse.de(salvar(peca));
    }

    @Transactional
    public PecaResponse atualizarPeca(Long id, PecaRequest request) {
        Peca peca = carregarPeca(id);
        if (pecas.existsByCodigoAndIdNot(Peca.normalizarCodigo(request.codigo()), id)) {
            throw new ConflitoException(MENSAGEM_CODIGO_DUPLICADO);
        }
        peca.atualizar(request.codigo(), request.descricao(), request.precoVenda());
        return PecaResponse.de(salvar(peca));
    }

    @Transactional
    public PecaResponse inativarPeca(Long id) {
        Peca peca = carregarPeca(id);
        peca.inativar();
        return PecaResponse.de(peca);
    }

    private Peca salvar(Peca peca) {
        try {
            return pecas.saveAndFlush(peca);
        } catch (DataIntegrityViolationException e) {
            throw new ConflitoException(MENSAGEM_CODIGO_DUPLICADO);
        }
    }

    private Peca carregarPeca(Long id) {
        return pecas.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Peça não encontrada: " + id));
    }
}
