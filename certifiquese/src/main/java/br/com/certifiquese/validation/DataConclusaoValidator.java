package br.com.certifiquese.validation;

import java.time.LocalDate;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class DataConclusaoValidator implements ConstraintValidator<DataConclusaoValida, LocalDate> {

    private int anoMinimo = 1960;

    public DataConclusaoValidator() {
    }

    public DataConclusaoValidator(int anoMinimo) {
        this.anoMinimo = anoMinimo;
    }

    @Override
    public void initialize(DataConclusaoValida constraintAnnotation) {
        if (constraintAnnotation != null) {
            this.anoMinimo = constraintAnnotation.anoMinimo();
        }
    }

    @Override
    public boolean isValid(LocalDate data, ConstraintValidatorContext context) {
        if (data == null) {
            return true;
        }

        LocalDate limiteMinimo = LocalDate.of(anoMinimo, 1, 1);
        LocalDate limiteMaximo = LocalDate.now();

        return !data.isBefore(limiteMinimo) && !data.isAfter(limiteMaximo);
    }
}
