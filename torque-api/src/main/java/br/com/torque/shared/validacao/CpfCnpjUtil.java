package br.com.torque.shared.validacao;

import java.util.regex.Pattern;

/** Normalização e validação de CPF/CNPJ (RN03). */
public final class CpfCnpjUtil {

    private static final Pattern CARACTERES_PERMITIDOS = Pattern.compile("^[0-9./\\-\\s]+$");
    private static final int[] PESOS_CNPJ_1 = {5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
    private static final int[] PESOS_CNPJ_2 = {6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};

    private CpfCnpjUtil() {
    }

    /** Remove a máscara, devolvendo só os dígitos. {@code null} continua {@code null}. */
    public static String normalizar(String documento) {
        return documento == null ? null : documento.replaceAll("\\D", "");
    }

    public static boolean isValido(String documento) {
        if (documento == null || !CARACTERES_PERMITIDOS.matcher(documento).matches()) {
            return false;
        }
        String digitos = normalizar(documento);
        return switch (digitos.length()) {
            case 11 -> isCpfValido(digitos);
            case 14 -> isCnpjValido(digitos);
            default -> false;
        };
    }

    private static boolean isCpfValido(String cpf) {
        if (todosIguais(cpf)) {
            return false;
        }
        for (int posicao = 9; posicao <= 10; posicao++) {
            int soma = 0;
            for (int i = 0; i < posicao; i++) {
                soma += digito(cpf, i) * (posicao + 1 - i);
            }
            int dv = (soma * 10) % 11 % 10;
            if (dv != digito(cpf, posicao)) {
                return false;
            }
        }
        return true;
    }

    private static boolean isCnpjValido(String cnpj) {
        if (todosIguais(cnpj)) {
            return false;
        }
        return digitoCnpj(cnpj, PESOS_CNPJ_1) == digito(cnpj, 12)
                && digitoCnpj(cnpj, PESOS_CNPJ_2) == digito(cnpj, 13);
    }

    private static int digitoCnpj(String cnpj, int[] pesos) {
        int soma = 0;
        for (int i = 0; i < pesos.length; i++) {
            soma += digito(cnpj, i) * pesos[i];
        }
        int resto = soma % 11;
        return resto < 2 ? 0 : 11 - resto;
    }

    private static boolean todosIguais(String digitos) {
        return digitos.chars().allMatch(c -> c == digitos.charAt(0));
    }

    private static int digito(String s, int i) {
        return s.charAt(i) - '0';
    }
}
