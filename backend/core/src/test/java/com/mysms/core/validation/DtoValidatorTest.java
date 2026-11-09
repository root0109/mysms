package com.mysms.core.validation;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.mysms.core.error.ValidationFailedException;
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.junit.jupiter.api.Test;

class DtoValidatorTest {

    record Dto(@NotNull String name, @Size(max = 2) String code) {
    }

    @Test
    void reportsEveryInvalidField() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            DtoValidator validator = new DtoValidator(factory.getValidator());
            assertThatCode(() -> validator.validate(new Dto("India", "IN"))).doesNotThrowAnyException();
            assertThatThrownBy(() -> validator.validate(new Dto(null, "IND")))
                    .isInstanceOf(ValidationFailedException.class)
                    .satisfies(e -> {
                        var errors = ((ValidationFailedException) e).errors();
                        org.assertj.core.api.Assertions.assertThat(errors).extracting("field").containsExactly("code", "name");
                        org.assertj.core.api.Assertions.assertThat(errors).extracting("code").containsExactly("Size", "NotNull");
                    });
        }
    }
}
