package br.com.torque.ordens;

import static br.com.torque.ordens.Fixtures.administrador;
import static br.com.torque.ordens.Fixtures.atendente;
import static br.com.torque.ordens.Fixtures.comNumero;
import static br.com.torque.ordens.Fixtures.peca;
import static br.com.torque.ordens.Fixtures.servico;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.torque.clientes.Cliente;
import br.com.torque.shared.excecao.RegraDeNegocioException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** CT46: link wa.me com telefone do cliente, resumo do orçamento e total. */
class CompartilhamentoWhatsAppTest {

    private static final String OFICINA = "Oficina Fictícia do Zé";

    private OrdemServico os;
    private Orcamento orcamento;

    @BeforeEach
    void montarOs() {
        Cliente cliente = new Cliente("Joana Maria Fictícia", "52998224725", "(83) 99999-0001", null);
        os = comNumero(OrdemServico.abrir(cliente.novoVeiculo("ABC1D23", "Fiat", "Uno", 2015, "Branco"),
                "Barulho na suspensão", 85_000, atendente()), 42);
        orcamento = os.getOrcamentoVigente();
        orcamento.adicionarServico(servico("Troca de óleo", "120.00"));
        orcamento.adicionarPeca(peca("Filtro de óleo", "35.00"), 2);
    }

    private static String textoDecodificado(String link) {
        String query = URI.create(link).getRawQuery();
        assertThat(query).startsWith("text=");
        return URLDecoder.decode(query.substring("text=".length()), StandardCharsets.UTF_8);
    }

    @Test
    void deveGerarLinkWaMeComDdiDoBrasilETelefoneDoCliente() {
        String link = CompartilhamentoWhatsApp.link(os, orcamento, OFICINA);

        assertThat(link).startsWith("https://wa.me/5583999990001?text=");
    }

    @Test
    void deveIncluirResumoComNumeroDaOsVeiculoItensETotal() {
        String texto = textoDecodificado(CompartilhamentoWhatsApp.link(os, orcamento, OFICINA));

        assertThat(texto)
                .startsWith("Olá, Joana!")
                .contains("OS nº 42")
                .contains("Fiat Uno (ABC1D23)")
                .contains("Troca de óleo: R$ 120,00")
                .contains("Filtro de óleo (2x): R$ 70,00")
                .contains("Total: R$ 190,00")
                .contains("Situação: Pendente")
                .endsWith(OFICINA)
                .doesNotContain("Desconto");
    }

    @Test
    void deveMostrarDescontoQuandoHouver() {
        orcamento.aplicarDesconto(new BigDecimal("19.00"), administrador());

        String texto = textoDecodificado(CompartilhamentoWhatsApp.link(os, orcamento, OFICINA));

        assertThat(texto).contains("Desconto: R$ 19,00").contains("Total: R$ 171,00");
    }

    @Test
    void deveCodificarTextoParaUrlSemEspacosNemCaracteresEspeciaisCrus() {
        String link = CompartilhamentoWhatsApp.link(os, orcamento, OFICINA);
        String query = URI.create(link).getRawQuery();

        assertThat(query).doesNotContain(" ", "+", "\n", "ó", "nº");
        assertThat(query).contains("%20").contains("%0A").contains("R%24%20190%2C00");
    }

    @Test
    void deveRecusarCompartilhamentoDeClienteSemTelefoneValido() {
        Cliente semTelefone = new Cliente("Carlos Fictício", "52998224725", null, null);
        OrdemServico outra = OrdemServico.abrir(semTelefone.novoVeiculo("XYZ9876", "VW", "Gol", 2010, null),
                "Revisão", 10_000, atendente());

        assertThatThrownBy(() -> CompartilhamentoWhatsApp.link(outra, outra.getOrcamentoVigente(), OFICINA))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("telefone");
    }

    @Test
    void deveFormatarMoedaNoPadraoBrasileiroComSeparadorDeMilhar() {
        assertThat(Formatos.moeda(new BigDecimal("1234.5"))).isEqualTo("R$ 1.234,50");
        assertThat(Formatos.moeda(BigDecimal.ZERO)).isEqualTo("R$ 0,00");
    }
}
