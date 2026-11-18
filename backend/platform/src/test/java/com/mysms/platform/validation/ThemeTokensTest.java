package com.mysms.platform.validation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.mysms.core.error.ValidationFailedException;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ThemeTokensTest {

    @Test
    void brandTokensAreKeptSortedAndColoursNormalised() {
        assertThat(ThemeTokens.parse(Map.of("radius.step", "-1", "color.primary", "#0f766e",
                "color.on-primary.dark", "#ffffff", "font.family", "Noto Sans")))
                .containsExactly(Map.entry("color.on-primary.dark", "#FFFFFF"), Map.entry("color.primary", "#0F766E"),
                        Map.entry("font.family", "Noto Sans"), Map.entry("radius.step", "-1"));
    }

    @Test
    void everyBadTokenIsReportedAtOnce() {
        assertThatThrownBy(() -> ThemeTokens.parse(Map.of("intent.success", "#00FF00", "color.secondary", "teal",
                "radius.step", "2", "font.family", 3)))
                .isInstanceOfSatisfying(ValidationFailedException.class, e -> assertThat(e.errors())
                        .extracting(error -> error.field())
                        .containsExactlyInAnyOrder("tokens.intent.success", "tokens.color.secondary",
                                "tokens.radius.step", "tokens.font.family"));
        assertThatThrownBy(() -> ThemeTokens.parse(List.of("color.primary")))
                .isInstanceOf(ValidationFailedException.class);
    }
}
