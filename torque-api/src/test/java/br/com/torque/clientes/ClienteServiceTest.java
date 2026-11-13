package br.com.torque.clientes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.torque.clientes.dto.ClienteRequest;
import br.com.torque.clientes.dto.VeiculoRequest;
import br.com.torque.shared.excecao.ConflitoException;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/** Normalização e tratamento de duplicidade no serviço, sem banco. */
class ClienteServiceTest {

    private final ClienteRepository clientes = mock(ClienteRepository.class);
    private final VeiculoRepository veiculos = mock(VeiculoRepository.class);
    private final ClienteService service = new ClienteService(clientes, veiculos, new VeiculoService(veiculos));

    @Test
    void deveNormalizarDocumentoTelefoneEEmailAntesDeSalvar() {
        when(clientes.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));

        service.cadastrar(new ClienteRequest("  Ana Fictícia ", "529.982.247-25", "(83) 99999-0001", " Ana@Exemplo.Teste "));

        ArgumentCaptor<Cliente> captor = ArgumentCaptor.forClass(Cliente.class);
        verify(clientes).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getNome()).isEqualTo("Ana Fictícia");
        assertThat(captor.getValue().getDocumento()).isEqualTo("52998224725");
        assertThat(captor.getValue().getTelefone()).isEqualTo("83999990001");
        assertThat(captor.getValue().getEmail()).isEqualTo("ana@exemplo.teste");
        verify(clientes).existsByDocumento("52998224725");
    }

    @Test
    void deveNormalizarPlacaAntesDeSalvar() {
        Cliente cliente = new Cliente("Ana", "52998224725", "83999990001", null);
        when(clientes.findById(1L)).thenReturn(Optional.of(cliente));
        when(veiculos.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));

        service.cadastrarVeiculo(1L, new VeiculoRequest(" abc-1d23 ", "Fiat", "Uno", 2010, ""));

        ArgumentCaptor<Veiculo> captor = ArgumentCaptor.forClass(Veiculo.class);
        verify(veiculos).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getPlaca()).isEqualTo("ABC1D23");
        assertThat(captor.getValue().getCor()).isNull();
        verify(veiculos).existsByPlaca("ABC1D23");
    }

    @Test
    void deveConverterViolacaoDeUniqueDoBancoEmConflito() {
        // Checagem prévia passa (cadastro simultâneo), mas o banco recusa pela constraint UNIQUE.
        when(clientes.existsByDocumento(anyString())).thenReturn(false);
        when(clientes.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException("cliente_documento_key"));

        assertThatThrownBy(() -> service.cadastrar(
                new ClienteRequest("Ana", "52998224725", "83999990001", null)))
                .isInstanceOf(ConflitoException.class)
                .hasMessage(ClienteService.MENSAGEM_DOCUMENTO_DUPLICADO);
    }

    @Test
    void deveConverterViolacaoDeUniqueDaPlacaEmConflito() {
        Cliente cliente = new Cliente("Ana", "52998224725", "83999990001", null);
        when(clientes.findById(1L)).thenReturn(Optional.of(cliente));
        when(veiculos.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException("veiculo_placa_key"));

        assertThatThrownBy(() -> service.cadastrarVeiculo(1L, new VeiculoRequest("ABC1D23", "Fiat", "Uno", null, null)))
                .isInstanceOf(ConflitoException.class)
                .hasMessage(VeiculoService.MENSAGEM_PLACA_DUPLICADA);
    }

    @Test
    void deveBuscarPorNomeEscapandoCuringasEPorDigitosDoDocumento() {
        when(clientes.buscar(anyString(), anyString(), any())).thenReturn(Page.empty());

        service.listar("50%_", Pageable.unpaged());
        verify(clientes).buscar("%50\\%\\_%", "%50%", Pageable.unpaged());

        service.listar("Ana", Pageable.unpaged());
        verify(clientes).buscar("%ana%", "", Pageable.unpaged());
    }
}
