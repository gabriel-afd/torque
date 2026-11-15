package br.com.torque.ordens;

import static br.com.torque.ordens.Fixtures.administrador;
import static br.com.torque.ordens.Fixtures.comNumero;
import static br.com.torque.ordens.Fixtures.osAberta;
import static br.com.torque.ordens.Fixtures.peca;
import static br.com.torque.ordens.Fixtures.servico;
import static org.assertj.core.api.Assertions.assertThat;

import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** CT45 (unidade): conteúdo do PDF do orçamento, conferido pela extração do texto. */
class OrcamentoPdfTest {

    private static final LocalDateTime EMISSAO = LocalDateTime.of(2026, 11, 20, 14, 30);

    private OrdemServico os;
    private Orcamento orcamento;

    @BeforeEach
    void montarOs() {
        os = comNumero(osAberta(), 37);
        orcamento = os.getOrcamentoVigente();
        orcamento.adicionarServico(servico("Troca de óleo", "120.00"));
        orcamento.adicionarPeca(peca("Filtro de óleo", "35.00"), 2);
        orcamento.aplicarDesconto(new BigDecimal("10.00"), administrador());
    }

    /** Texto do PDF com espaços normalizados (a extração quebra linhas entre células da tabela). */
    static String texto(byte[] pdf) throws IOException {
        PdfReader leitor = new PdfReader(pdf);
        try {
            PdfTextExtractor extrator = new PdfTextExtractor(leitor);
            StringBuilder texto = new StringBuilder();
            for (int pagina = 1; pagina <= leitor.getNumberOfPages(); pagina++) {
                texto.append(extrator.getTextFromPage(pagina)).append(' ');
            }
            return texto.toString().replaceAll("\\s+", " ");
        } finally {
            leitor.close();
        }
    }

    @Test
    void deveGerarArquivoPdfValido() {
        byte[] pdf = OrcamentoPdf.gerar(os, orcamento, "Oficina Fictícia", EMISSAO);

        assertThat(new String(pdf, 0, 5, StandardCharsets.ISO_8859_1)).isEqualTo("%PDF-");
    }

    @Test
    void deveConterOficinaNumeroDaOsDatasClienteEVeiculo() throws IOException {
        String texto = texto(OrcamentoPdf.gerar(os, orcamento, "Oficina Fictícia", EMISSAO));

        assertThat(texto)
                .contains("Oficina Fictícia")
                .contains("OS nº 37 (versão 1)")
                .contains("Data de abertura: " + os.getDataAbertura().format(Formatos.DATA))
                .contains("Emitido em: 20/11/2026 14:30")
                .contains("Cliente: Cliente Fictício")
                .contains("Fiat Uno 2015")
                .contains("Placa: ABC1D23");
    }

    @Test
    void deveConterItensDescontoTotalESituacao() throws IOException {
        String texto = texto(OrcamentoPdf.gerar(os, orcamento, "Oficina Fictícia", EMISSAO));

        assertThat(texto)
                .contains("Troca de óleo (serviço)")
                // a extração junta células vizinhas da tabela sem espaço
                .containsPattern("Filtro de óleo ?2 ?R\\$ 35,00 ?R\\$ 70,00")
                .contains("Subtotal R$ 190,00")
                .contains("Desconto R$ 10,00")
                .contains("Total R$ 180,00")
                .contains("Situação do orçamento: Pendente");
    }

    @Test
    void deveIndicarOrcamentoSemItens() throws IOException {
        OrdemServico vazia = comNumero(osAberta(), 38);

        String texto = texto(OrcamentoPdf.gerar(vazia, vazia.getOrcamentoVigente(), "Oficina Fictícia", EMISSAO));

        assertThat(texto).contains("Nenhum item no orçamento").contains("Total R$ 0,00");
    }
}
