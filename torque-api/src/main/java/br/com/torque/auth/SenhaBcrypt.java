package br.com.torque.auth;

import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.nio.charset.StandardCharsets;

/**
 * A senha precisa caber nos 72 <b>bytes</b> que o BCrypt usa. {@code @Size(max = 72)} conta caracteres, e um
 * caractere acentuado ocupa 2 bytes em UTF-8. Nulo é válido (combine com {@code @NotBlank} quando obrigatória).
 */
@Documented
@Constraint(validatedBy = SenhaBcrypt.Validador.class)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
public @interface SenhaBcrypt {

    int MAXIMO_BYTES = 72;

    String message() default "A senha deve ter no máximo 72 bytes (caracteres acentuados contam como 2)";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class Validador implements ConstraintValidator<SenhaBcrypt, String> {

        public static boolean cabe(String senha) {
            return senha == null || senha.getBytes(StandardCharsets.UTF_8).length <= MAXIMO_BYTES;
        }

        @Override
        public boolean isValid(String senha, ConstraintValidatorContext contexto) {
            return cabe(senha);
        }
    }
}
