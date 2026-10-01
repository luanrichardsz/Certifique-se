package br.com.certifiquese.validation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

@Target({ElementType.FIELD, ElementType.RECORD_COMPONENT, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = DataConclusaoValidator.class)
@Documented
public @interface DataConclusaoValida {

    String message() default "A data de conclusão deve ser a partir de 01/01/1960 e não pode ultrapassar a data atual";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    int anoMinimo() default 1960;
}
