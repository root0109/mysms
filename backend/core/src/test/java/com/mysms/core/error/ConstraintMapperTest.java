package com.mysms.core.error;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Locale;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.i18n.LocaleContextHolder;

/** Field errors read from the message catalogue by code, in the language of the call (a test-only second language). */
class ConstraintMapperTest {

    private final ConstraintMapper mapper = new ConstraintMapper(MessageUtil.ofCatalogue());

    @AfterEach
    void forgetTheLanguage() {
        LocaleContextHolder.resetLocaleContext();
    }

    @Test
    void englishKeepsTheSentenceTheErrorWasRaisedWith() {
        LocaleContextHolder.setLocale(Locale.ENGLISH);
        FieldError raised = new FieldError("name", "must not be null", "NotNull");
        assertThat(mapper.localized(raised)).isSameAs(raised);
    }

    @Test
    void aCodeTheCallersLanguageKnowsIsTranslatedWhoeverRaisedIt() {
        LocaleContextHolder.setLocale(Locale.forLanguageTag("eo"));
        assertThat(mapper.localized(new FieldError("name", "must not be null", "NotNull")))
                .isEqualTo(new FieldError("name", "ne devas esti nula", "NotNull"));
    }

    @Test
    void aCodeOnlyTheBaseBundleKnowsAnswersInEnglishAndAnUnknownCodeKeepsItsSentence() {
        LocaleContextHolder.setLocale(Locale.forLanguageTag("eo"));
        assertThat(mapper.localized(new FieldError("name", "must not be blank", "NotBlank")).message())
                .isEqualTo("must not be blank");
        FieldError domain = new FieldError("courseId", "course does not exist", "NOT_FOUND");
        assertThat(mapper.localized(domain)).isSameAs(domain);
        FieldError uncoded = new FieldError("x", "something", null);
        assertThat(mapper.localized(uncoded)).isSameAs(uncoded);
        // a service that says more than the standard sentence of the code keeps its own words
        FieldError specific = new FieldError("scopeId", "is required for this scope", "NotNull");
        assertThat(mapper.localized(specific)).isSameAs(specific);
        FieldError silent = new FieldError("x", null, "NotNull");
        assertThat(mapper.localized(silent)).isSameAs(silent);
    }

    @Test
    void aLanguageWithoutABundleAnswersInEnglishNotInTheServersLanguage() {
        Locale server = Locale.getDefault();
        try {
            Locale.setDefault(Locale.forLanguageTag("eo"));
            LocaleContextHolder.setLocale(Locale.JAPANESE);
            assertThat(mapper.localized(new FieldError("name", "must not be null", "NotNull")).message())
                    .isEqualTo("must not be null");
        } finally {
            Locale.setDefault(server);
        }
    }
}
