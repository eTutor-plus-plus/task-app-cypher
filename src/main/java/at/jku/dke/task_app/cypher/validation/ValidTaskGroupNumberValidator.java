package at.jku.dke.task_app.cypher.validation;

import at.jku.dke.task_app.cypher.dto.ModifyCypherTaskGroupDto;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Custom validator for numbers in {@link ModifyCypherTaskGroupDto}.
 */
public class ValidTaskGroupNumberValidator implements ConstraintValidator<ValidTaskGroupNumber, ModifyCypherTaskGroupDto> {
    /**
     * Creates a new instance of class Valid task group number validator.
     */
    public ValidTaskGroupNumberValidator() {
    }

    @Override
    public boolean isValid(ModifyCypherTaskGroupDto value, ConstraintValidatorContext context) {
        return value.minNumber() < value.maxNumber();
    }
}
