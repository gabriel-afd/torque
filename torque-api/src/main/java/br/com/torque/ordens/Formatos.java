package br.com.torque.ordens;

import br.com.torque.shared.Dinheiro;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Formatação em pt-BR para o PDF e a mensagem do WhatsApp. */
final class Formatos {

    static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    static final DateTimeFormatter DATA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private static final Locale PT_BR = Locale.of("pt", "BR");

    private Formatos() {
    }

    /**
     * {@code R$ 1.234,50}, com espaço comum (o {@code NumberFormat} de moeda do JDK usa espaço não separável,
     * que atrapalha a busca de texto no PDF e o encoding da URL).
     */
    static String moeda(BigDecimal valor) {
        DecimalFormat formato = new DecimalFormat("#,##0.00", DecimalFormatSymbols.getInstance(PT_BR));
        return "R$ " + formato.format(Dinheiro.de(valor));
    }
}
