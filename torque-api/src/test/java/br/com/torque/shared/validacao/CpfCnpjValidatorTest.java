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

/** CT09, CT11 — RN03. Documentos fictícios gerados apenas para teste. */
class CpfCnpjValidatorTest {

    record Documento(@CpfCnpj String valor) {
    }

    private static Validator validator;

    @BeforeAll
    static void criarValidator() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    private Set<ConstraintViolation<Documento>> validar(String valor) {
        return validator.validate(new Documento(valor));
    }

    @ParameterizedTest(name = "CPF válido: {0}")
    @ValueSource(strings = {"52998224725", "529.982.247-25", "111.444.777-35", "123.456.789-09", " 111.444.777-35 "})
    void deveAceitarCpfValidoComOuSemMascara(String cpf) {
        assertThat(validar(cpf)).isEmpty();
    }

    @ParameterizedTest(name = "CNPJ válido: {0}")
    @ValueSource(strings = {"11222333000181", "11.222.333/0001-81", "12.345.678/0001-95"})
    void deveAceitarCnpjValidoComOuSemMascara(String cnpj) {
        assertThat(validar(cnpj)).isEmpty();
    }

    @ParameterizedTest(name = "inválido: \"{0}\"")
    @ValueSource(strings = {
            "111.111.111-12",       // CT09: dígito verificador errado
            "529.982.247-26",       // segundo dígito errado
            "529.982.247-15",       // primeiro dígito errado
            "11.222.333/0001-82",   // CNPJ com dígito errado
            "11.222.333/0001-91",
            "1234567890",           // 10 dígitos
            "123456789012",         // 12 dígitos
            "123456789012345",      // 15 dígitos
            "",
            "   ",
            "abc.def.ghi-jk",
            "529982247A5",          // letra no meio
            "52998224725x"
    })
    void deveRecusarDocumentoInvalido(String documento) {
        assertThat(validar(documento))
                .singleElement()
                .extracting(ConstraintViolation::getMessage)
                .isEqualTo("CPF ou CNPJ inválido");
    }

    @ParameterizedTest(name = "sequência repetida: {0}")
    @ValueSource(strings = {"111.111.111-11", "00000000000", "99999999999", "00.000.000/0000-00", "11111111111111"})
    void deveRecusarSequenciasRepetidas(String documento) {
        assertThat(validar(documento)).hasSize(1);
    }

    @Test
    void deveAceitarNuloDeixandoObrigatoriedadeParaNotBlank() {
        assertThat(validar(null)).isEmpty();
    }

    @ParameterizedTest(name = "{0} -> {1}")
    @CsvSource({
            "529.982.247-25, 52998224725",
            "11.222.333/0001-81, 11222333000181",
            "' 529 982 247 25 ', 52998224725",
            "52998224725, 52998224725"
    })
    void deveNormalizarParaSomenteDigitos(String entrada, String esperado) {
        assertThat(CpfCnpjUtil.normalizar(entrada)).isEqualTo(esperado);
    }

    @Test
    void deveManterNuloAoNormalizar() {
        assertThat(CpfCnpjUtil.normalizar(null)).isNull();
    }
}
