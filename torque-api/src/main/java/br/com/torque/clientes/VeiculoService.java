package br.com.torque.clientes;

import br.com.torque.clientes.dto.VeiculoComDonoResponse;
import br.com.torque.clientes.dto.VeiculoRequest;
import br.com.torque.clientes.dto.VeiculoResponse;
import br.com.torque.shared.excecao.ConflitoException;
import br.com.torque.shared.excecao.RecursoNaoEncontradoException;
import br.com.torque.shared.validacao.PlacaUtil;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VeiculoService {

    static final String MENSAGEM_PLACA_DUPLICADA = "Já existe um veículo com esta placa";

    private final VeiculoRepository veiculos;

    public VeiculoService(VeiculoRepository veiculos) {
        this.veiculos = veiculos;
    }

    @Transactional(readOnly = true)
    public VeiculoComDonoResponse buscarPorPlaca(String placa) {
        String normalizada = PlacaUtil.normalizar(placa);
        return veiculos.buscarPorPlacaComDono(normalizada)
                .map(VeiculoComDonoResponse::de)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Veículo não encontrado: " + normalizada));
    }

    @Transactional
    public VeiculoResponse atualizar(Long id, VeiculoRequest request) {
        Veiculo veiculo = veiculos.buscarComDono(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Veículo não encontrado: " + id));
        String placa = PlacaUtil.normalizar(request.placa());
        if (veiculos.existsByPlacaAndIdNot(placa, id)) {
            throw new ConflitoException(MENSAGEM_PLACA_DUPLICADA);
        }
        veiculo.atualizar(request.placa(), request.marca(), request.modelo(), request.ano(), request.cor());
        return VeiculoResponse.de(salvar(veiculo));
    }

    @Transactional
    Veiculo salvarNovo(Veiculo veiculo) {
        if (veiculos.existsByPlaca(veiculo.getPlaca())) {
            throw new ConflitoException(MENSAGEM_PLACA_DUPLICADA);
        }
        return salvar(veiculo);
    }

    /** A constraint UNIQUE do banco é a garantia final contra cadastros simultâneos. */
    private Veiculo salvar(Veiculo veiculo) {
        try {
            return veiculos.saveAndFlush(veiculo);
        } catch (DataIntegrityViolationException e) {
            throw new ConflitoException(MENSAGEM_PLACA_DUPLICADA);
        }
    }
}
