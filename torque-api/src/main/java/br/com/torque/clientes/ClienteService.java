package br.com.torque.clientes;

import br.com.torque.clientes.dto.ClienteDetalheResponse;
import br.com.torque.clientes.dto.ClienteRequest;
import br.com.torque.clientes.dto.ClienteResponse;
import br.com.torque.clientes.dto.VeiculoRequest;
import br.com.torque.clientes.dto.VeiculoResponse;
import br.com.torque.shared.excecao.ConflitoException;
import br.com.torque.shared.excecao.RecursoNaoEncontradoException;
import br.com.torque.shared.validacao.CpfCnpjUtil;
import java.util.Locale;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ClienteService {

    static final String MENSAGEM_DOCUMENTO_DUPLICADO = "Já existe um cliente com este CPF/CNPJ";

    private final ClienteRepository clientes;
    private final VeiculoRepository veiculos;
    private final VeiculoService veiculoService;

    public ClienteService(ClienteRepository clientes, VeiculoRepository veiculos, VeiculoService veiculoService) {
        this.clientes = clientes;
        this.veiculos = veiculos;
        this.veiculoService = veiculoService;
    }

    /** Busca por parte do nome (sem diferenciar maiúsculas) ou do documento (com ou sem máscara). */
    @Transactional(readOnly = true)
    public Page<ClienteResponse> listar(String busca, Pageable pageable) {
        if (busca == null || busca.isBlank()) {
            return clientes.findAll(pageable).map(ClienteResponse::de);
        }
        String termo = busca.trim().toLowerCase(Locale.ROOT);
        String padraoNome = "%" + escaparLike(termo) + "%";
        String digitos = CpfCnpjUtil.normalizar(termo);
        // Sem dígitos no termo, o padrão vazio não casa com nenhum documento (todos têm 11 ou 14 dígitos).
        String padraoDocumento = digitos.isEmpty() ? "" : "%" + digitos + "%";
        return clientes.buscar(padraoNome, padraoDocumento, pageable).map(ClienteResponse::de);
    }

    @Transactional(readOnly = true)
    public ClienteDetalheResponse buscar(Long id) {
        Cliente cliente = carregar(id);
        return ClienteDetalheResponse.de(cliente, veiculos.findByClienteIdOrderByPlaca(id));
    }

    @Transactional
    public ClienteResponse cadastrar(ClienteRequest request) {
        Cliente cliente = new Cliente(request.nome(), request.documento(), request.telefone(), request.email());
        if (clientes.existsByDocumento(cliente.getDocumento())) {
            throw new ConflitoException(MENSAGEM_DOCUMENTO_DUPLICADO);
        }
        return ClienteResponse.de(salvar(cliente));
    }

    @Transactional
    public ClienteResponse atualizar(Long id, ClienteRequest request) {
        Cliente cliente = carregar(id);
        String documento = CpfCnpjUtil.normalizar(request.documento());
        if (clientes.existsByDocumentoAndIdNot(documento, id)) {
            throw new ConflitoException(MENSAGEM_DOCUMENTO_DUPLICADO);
        }
        cliente.atualizar(request.nome(), request.documento(), request.telefone(), request.email());
        return ClienteResponse.de(salvar(cliente));
    }

    @Transactional
    public ClienteResponse inativar(Long id) {
        Cliente cliente = carregar(id);
        cliente.inativar();
        return ClienteResponse.de(cliente);
    }

    @Transactional
    public VeiculoResponse cadastrarVeiculo(Long clienteId, VeiculoRequest request) {
        Cliente cliente = carregar(clienteId);
        Veiculo veiculo = cliente.novoVeiculo(request.placa(), request.marca(), request.modelo(),
                request.ano(), request.cor());
        return VeiculoResponse.de(veiculoService.salvarNovo(veiculo));
    }

    /** A checagem prévia não cobre cadastros simultâneos: a constraint UNIQUE do banco é a garantia final. */
    private Cliente salvar(Cliente cliente) {
        try {
            return clientes.saveAndFlush(cliente);
        } catch (DataIntegrityViolationException e) {
            throw new ConflitoException(MENSAGEM_DOCUMENTO_DUPLICADO);
        }
    }

    private Cliente carregar(Long id) {
        return clientes.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Cliente não encontrado: " + id));
    }

    private static String escaparLike(String termo) {
        return termo.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
