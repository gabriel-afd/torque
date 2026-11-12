package br.com.torque.shared.validacao;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.Set;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

/** CT12, CT13, CT14, CT15 — RN02. */
class PlacaValidatorTest {

    record Veiculo(@Placa String placa) {
    }

    private static Validator validator;

    @BeforeAll
    static void criarValidator() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    private Set<ConstraintViolation<Veiculo>> validar(String placa) {
        return validator.validate(new Veiculo(placa));
    }

    @ParameterizedTest(name = "Mercosul: {0}")
    @ValueSource(strings = {"ABC1D23", "XYZ9Z99", "AAA0A00", "abc1d23", "ABC-1D23"})
    void deveAceitarPlacaMercosul(String placa) {
        assertThat(validar(placa)).isEmpty();
    }

    @ParameterizedTest(name = "antiga: {0}")
    @ValueSource(strings = {"ABC1234", "ZZZ0000", "abc-1234", "ABC 1234"})
    void deveAceitarPlacaNoFormatoAntigo(String placa) {
        assertThat(validar(placa)).isEmpty();
    }

    @ParameterizedTest(name = "inválida: \"{0}\"")
    @ValueSource(strings = {
            "AB-1234",    // CT14: só duas letras
            "ABCD123",    // CT14: quatro letras
            "ABC123",     // curta
            "ABC12345",   // longa
            "1BC1234",    // começa com número
            "ABCD234",
            "ABC1DD3",    // duas letras no meio
            "ABC1D2E",    // termina com letra
            "ABÇ1234",
            "ABC_1234",
            "",
            "   "
    })
    void deveRecusarPlacaInvalida(String placa) {
        assertThat(validar(placa))
                .singleElement()
                .extracting(ConstraintViolation::getMessage)
                .isEqualTo("Placa inválida (use ABC1234 ou ABC1D23)");
    }

    @Test
    void deveAceitarNuloDeixandoObrigatoriedadeParaNotBlank() {
        assertThat(validar(null)).isEmpty();
    }

    @ParameterizedTest(name = "{0} -> {1}")
    @CsvSource({
            "abc-1d23, ABC1D23",   // CT15
            "abc1234, ABC1234",
            "' AbC 1d23 ', ABC1D23",
            "ABC-1234, ABC1234",
            "ABC1D23, ABC1D23"
    })
    void deveNormalizarParaMaiusculasSemHifenEEspacos(String entrada, String esperado) {
        assertThat(PlacaUtil.normalizar(entrada)).isEqualTo(esperado);
    }

    @Test
    void deveManterNuloAoNormalizar() {
        assertThat(PlacaUtil.normalizar(null)).isNull();
        assertThat(PlacaUtil.isValida(null)).isFalse();
    }
}
