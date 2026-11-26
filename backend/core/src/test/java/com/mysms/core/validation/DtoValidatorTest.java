package com.mysms.core.validation;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.mysms.core.error.ValidationFailedException;
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.Optional;
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

    /** A PATCH body (§7.2): the constraints sit on the type argument; null = absent, empty = cleared. */
    record Changes(Optional<@NotNull @Size(max = 2) String> code, Optional<@Size(max = 2) String> note) {
    }

    @Test
    void aPatchMayChangeOrClearAnOptionalFieldButNeverClearARequiredOne() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            DtoValidator validator = new DtoValidator(factory.getValidator());
            assertThatCode(() -> validator.validate(new Changes(null, null))).as("absent: nothing to check")
                    .doesNotThrowAnyException();
            assertThatCode(() -> validator.validate(new Changes(Optional.of("IN"), Optional.empty())))
                    .as("a value is checked, an optional field may be cleared").doesNotThrowAnyException();
            assertThatThrownBy(() -> validator.validate(new Changes(Optional.empty(), Optional.of("IND"))))
                    .isInstanceOf(ValidationFailedException.class)
                    .satisfies(e -> {
                        var errors = ((ValidationFailedException) e).errors();
                        org.assertj.core.api.Assertions.assertThat(errors).extracting("field")
                                .as("the property itself is named").containsExactly("code", "note");
                        org.assertj.core.api.Assertions.assertThat(errors).extracting("code")
                                .containsExactly("NotNull", "Size");
                    });
        }
    }
}
