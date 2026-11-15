package br.com.torque.shared.validacao;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Placa antiga (ABC1234) ou Mercosul (ABC1D23); aceita minúsculas, hífen e espaços. {@code null} é aceito. */
@Documented
@Constraint(validatedBy = PlacaValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
public @interface Placa {

    String message() default "Placa inválida (use ABC1234 ou ABC1D23)";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
