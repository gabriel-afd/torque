package br.com.torque.ordens;

import br.com.torque.clientes.Cliente;
import br.com.torque.clientes.Veiculo;
import br.com.torque.shared.excecao.RegraDeNegocioException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Link {@code https://wa.me/55DDDNUMERO?text=...} com o resumo do orçamento para o cliente (HU06).
 * Sem Spring e sem banco: recebe a OS já carregada.
 */
final class CompartilhamentoWhatsApp {

    static final String BASE = "https://wa.me/";
    private static final String DDI_BRASIL = "55";

    private CompartilhamentoWhatsApp() {
    }

    static String link(OrdemServico os, Orcamento orcamento, String oficina) {
        Cliente cliente = os.getVeiculo().getCliente();
        return BASE + telefone(cliente) + "?text=" + codificar(mensagem(os, orcamento, oficina));
    }

    /** Telefone gravado só com dígitos, DDD + número (10 ou 11 dígitos). */
    static String telefone(Cliente cliente) {
        String telefone = cliente.getTelefone();
        if (telefone == null || !telefone.matches("\\d{10,11}")) {
            throw new RegraDeNegocioException("Cliente sem telefone válido (DDD + número) para enviar pelo WhatsApp");
        }
        return DDI_BRASIL + telefone;
    }

    static String mensagem(OrdemServico os, Orcamento orcamento, String oficina) {
        Veiculo veiculo = os.getVeiculo();
        List<String> linhas = new ArrayList<>();
        linhas.add("Olá, " + primeiroNome(os.getVeiculo().getCliente().getNome()) + "!");
        linhas.add("Orçamento da OS nº " + os.getNumero() + " – " + veiculo.getMarca() + " " + veiculo.getModelo()
                + " (" + veiculo.getPlaca() + "):");
        orcamento.getItensServico().forEach(item ->
                linhas.add("• " + item.getDescricao() + ": " + Formatos.moeda(item.getPreco())));
        orcamento.getItensPeca().forEach(item ->
                linhas.add("• " + item.getDescricao() + " (" + item.getQuantidade() + "x): "
                        + Formatos.moeda(item.getSubtotal())));
        if (orcamento.getDesconto().signum() > 0) {
            linhas.add("Desconto: " + Formatos.moeda(orcamento.getDesconto()));
        }
        linhas.add("Total: " + Formatos.moeda(orcamento.getTotal()));
        linhas.add("Situação: " + orcamento.getSituacao().getRotulo());
        linhas.add(oficina);
        return String.join("\n", linhas);
    }

    /** URL-encoding em UTF-8 com espaço como {@code %20} (o {@code +} do form-encoding não é decodificado pelo wa.me). */
    static String codificar(String texto) {
        return URLEncoder.encode(texto, StandardCharsets.UTF_8).replace("+", "%20");
    }

    private static String primeiroNome(String nome) {
        return nome.trim().split("\\s+")[0];
    }
}
