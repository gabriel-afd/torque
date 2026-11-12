package br.com.torque.shared.validacao;

import java.util.Locale;
import java.util.regex.Pattern;

/** Normalização e validação de placa: antiga ABC1234 ou Mercosul ABC1D23 (RN02). */
public final class PlacaUtil {

    private static final Pattern FORMATO = Pattern.compile("^[A-Z]{3}[0-9][A-Z0-9][0-9]{2}$");

    private PlacaUtil() {
    }

    /** Maiúsculas, sem hífen e sem espaços. {@code null} continua {@code null}. */
    public static String normalizar(String placa) {
        return placa == null ? null : placa.replaceAll("[\\s-]", "").toUpperCase(Locale.ROOT);
    }

    public static boolean isValida(String placa) {
        return placa != null && FORMATO.matcher(normalizar(placa)).matches();
    }
}
