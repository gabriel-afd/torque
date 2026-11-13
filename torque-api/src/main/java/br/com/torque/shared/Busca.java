package br.com.torque.shared;

import java.util.Locale;

/** Apoio às buscas textuais com {@code like}. */
public final class Busca {

    private Busca() {
    }

    public static boolean vazia(String termo) {
        return termo == null || termo.isBlank();
    }

    /**
     * Padrão "contém" em minúsculas, com {@code %}, {@code _} e {@code \} escapados
     * (use {@code escape '\'} na consulta). Termo vazio casa com tudo.
     */
    public static String contem(String termo) {
        if (vazia(termo)) {
            return "%";
        }
        String escapado = termo.trim().toLowerCase(Locale.ROOT)
                .replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
        return "%" + escapado + "%";
    }
}
