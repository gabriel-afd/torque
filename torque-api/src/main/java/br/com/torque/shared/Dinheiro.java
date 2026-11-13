package br.com.torque.shared;

import br.com.torque.shared.excecao.RegraDeNegocioException;
import java.math.BigDecimal;
import java.math.RoundingMode;

/** Regras de dinheiro do projeto: escala 2, {@link RoundingMode#HALF_EVEN}. */
public final class Dinheiro {

    public static final int ESCALA = 2;
    public static final RoundingMode ARREDONDAMENTO = RoundingMode.HALF_EVEN;
    public static final BigDecimal ZERO = BigDecimal.ZERO.setScale(ESCALA, ARREDONDAMENTO);

    private Dinheiro() {
    }

    public static BigDecimal de(BigDecimal valor) {
        return valor == null ? ZERO : valor.setScale(ESCALA, ARREDONDAMENTO);
    }

    public static BigDecimal de(String valor) {
        return de(new BigDecimal(valor));
    }

    /** Valor obrigatório e não negativo, normalizado para escala 2. */
    public static BigDecimal naoNegativo(BigDecimal valor, String campo) {
        if (valor == null || valor.signum() < 0) {
            throw new RegraDeNegocioException(campo + " não pode ser negativo");
        }
        return de(valor);
    }
}
