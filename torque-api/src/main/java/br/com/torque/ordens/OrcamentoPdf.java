package br.com.torque.ordens;

import br.com.torque.clientes.Cliente;
import br.com.torque.clientes.Veiculo;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * PDF do orçamento (HU06) com OpenPDF: oficina, número da OS, data, cliente, veículo/placa, itens,
 * desconto, total e situação. Sem Spring e sem banco: recebe a OS já carregada.
 */
final class OrcamentoPdf {

    private static final Color AZUL = new Color(0x1F, 0x3A, 0x5F);
    private static final Color CINZA_CLARO = new Color(0xEE, 0xF1, 0xF5);

    private static final Font TITULO = new Font(Font.HELVETICA, 16, Font.BOLD, AZUL);
    private static final Font SUBTITULO = new Font(Font.HELVETICA, 12, Font.BOLD);
    private static final Font NORMAL = new Font(Font.HELVETICA, 10);
    private static final Font NEGRITO = new Font(Font.HELVETICA, 10, Font.BOLD);
    private static final Font CABECALHO = new Font(Font.HELVETICA, 10, Font.BOLD, Color.WHITE);

    private OrcamentoPdf() {
    }

    static byte[] gerar(OrdemServico os, Orcamento orcamento, String oficina, LocalDateTime emitidoEm) {
        ByteArrayOutputStream saida = new ByteArrayOutputStream();
        Document documento = new Document(PageSize.A4, 40, 40, 40, 40);
        try {
            PdfWriter.getInstance(documento, saida);
            documento.addTitle("Orçamento OS " + os.getNumero());
            documento.open();
            cabecalho(documento, os, orcamento, oficina, emitidoEm);
            dadosDoCliente(documento, os);
            documento.add(itens(orcamento));
            documento.add(totais(orcamento));
            documento.add(new Paragraph("Situação do orçamento: " + orcamento.getSituacao().getRotulo(), NEGRITO));
        } catch (DocumentException e) {
            throw new IllegalStateException("Falha ao gerar o PDF do orçamento", e);
        } finally {
            documento.close();
        }
        return saida.toByteArray();
    }

    private static void cabecalho(Document documento, OrdemServico os, Orcamento orcamento, String oficina,
                                  LocalDateTime emitidoEm) {
        documento.add(new Paragraph(oficina, TITULO));
        Paragraph titulo = new Paragraph("Orçamento – OS nº " + os.getNumero() + " (versão " + orcamento.getVersao() + ")",
                SUBTITULO);
        titulo.setSpacingBefore(6);
        documento.add(titulo);
        documento.add(new Paragraph("Data de abertura: " + os.getDataAbertura().format(Formatos.DATA)
                + "    Emitido em: " + emitidoEm.format(Formatos.DATA_HORA), NORMAL));
    }

    private static void dadosDoCliente(Document documento, OrdemServico os) {
        Veiculo veiculo = os.getVeiculo();
        Cliente cliente = veiculo.getCliente();
        Paragraph dados = new Paragraph();
        dados.setSpacingBefore(10);
        dados.setSpacingAfter(10);
        dados.add(new Phrase("Cliente: ", NEGRITO));
        dados.add(new Phrase(cliente.getNome() + "\n", NORMAL));
        dados.add(new Phrase("Veículo: ", NEGRITO));
        dados.add(new Phrase(veiculo.getMarca() + " " + veiculo.getModelo()
                + (veiculo.getAno() == null ? "" : " " + veiculo.getAno()) + "    ", NORMAL));
        dados.add(new Phrase("Placa: ", NEGRITO));
        dados.add(new Phrase(veiculo.getPlaca(), NORMAL));
        documento.add(dados);
    }

    private static PdfPTable itens(Orcamento orcamento) {
        PdfPTable tabela = new PdfPTable(new float[] {6, 1.2f, 2.2f, 2.2f});
        tabela.setWidthPercentage(100);
        tabela.setHeaderRows(1);
        for (String titulo : new String[] {"Descrição", "Qtd.", "Valor unit.", "Subtotal"}) {
            PdfPCell celula = new PdfPCell(new Phrase(titulo, CABECALHO));
            celula.setBackgroundColor(AZUL);
            celula.setPadding(5);
            tabela.addCell(celula);
        }
        orcamento.getItensServico().forEach(item ->
                linha(tabela, item.getDescricao() + " (serviço)", 1, item.getPreco(), item.getPreco()));
        orcamento.getItensPeca().forEach(item ->
                linha(tabela, item.getDescricao(), item.getQuantidade(), item.getPrecoUnitario(), item.getSubtotal()));
        if (!orcamento.temItens()) {
            PdfPCell vazio = new PdfPCell(new Phrase("Nenhum item no orçamento", NORMAL));
            vazio.setColspan(4);
            vazio.setPadding(5);
            tabela.addCell(vazio);
        }
        return tabela;
    }

    private static void linha(PdfPTable tabela, String descricao, int quantidade, BigDecimal unitario,
                              BigDecimal subtotal) {
        tabela.addCell(celula(descricao, Element.ALIGN_LEFT));
        tabela.addCell(celula(String.valueOf(quantidade), Element.ALIGN_CENTER));
        tabela.addCell(celula(Formatos.moeda(unitario), Element.ALIGN_RIGHT));
        tabela.addCell(celula(Formatos.moeda(subtotal), Element.ALIGN_RIGHT));
    }

    private static PdfPCell celula(String texto, int alinhamento) {
        PdfPCell celula = new PdfPCell(new Phrase(texto, NORMAL));
        celula.setHorizontalAlignment(alinhamento);
        celula.setPadding(4);
        return celula;
    }

    private static PdfPTable totais(Orcamento orcamento) {
        PdfPTable tabela = new PdfPTable(new float[] {9.2f, 2.2f});
        tabela.setWidthPercentage(100);
        tabela.setSpacingBefore(6);
        tabela.setSpacingAfter(12);
        total(tabela, "Subtotal", orcamento.getSubtotal(), NORMAL);
        total(tabela, "Desconto", orcamento.getDesconto(), NORMAL);
        total(tabela, "Total", orcamento.getTotal(), NEGRITO);
        return tabela;
    }

    private static void total(PdfPTable tabela, String rotulo, BigDecimal valor, Font fonte) {
        PdfPCell nome = new PdfPCell(new Phrase(rotulo, fonte));
        nome.setHorizontalAlignment(Element.ALIGN_RIGHT);
        nome.setBorder(PdfPCell.NO_BORDER);
        nome.setPadding(3);
        PdfPCell numero = new PdfPCell(new Phrase(Formatos.moeda(valor), fonte));
        numero.setHorizontalAlignment(Element.ALIGN_RIGHT);
        numero.setBorder(PdfPCell.NO_BORDER);
        numero.setBackgroundColor(CINZA_CLARO);
        numero.setPadding(3);
        tabela.addCell(nome);
        tabela.addCell(numero);
    }
}
