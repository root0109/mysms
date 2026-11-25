package com.mysms.comms.business;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;

class TemplateTextTest {

    @Test
    void fillsEveryPlaceholderThePayloadNames() {
        assertThat(TemplateText.render("Dear {{name}}, fees of {{ amount }} are due on {{due.date}}.",
                Map.of("name", "Asha", "amount", 1500, "due.date", "1 May")))
                .isEqualTo("Dear Asha, fees of 1500 are due on 1 May.");
    }

    @Test
    void leavesAnUnfilledPlaceholderVisible() {
        assertThat(TemplateText.render("Hello {{name}} from {{school}}", Map.of("name", "Ravi")))
                .isEqualTo("Hello Ravi from {{school}}");
    }

    @Test
    void takesAValueLiterallyEvenWhenItLooksLikeAReplacement() {
        assertThat(TemplateText.render("Pay {{amount}}", Map.of("amount", "$1 \\ 2"))).isEqualTo("Pay $1 \\ 2");
    }

    @Test
    void keepsNoTextAsNoText() {
        assertThat(TemplateText.render(null, Map.of())).isNull();
        assertThat(TemplateText.render("No placeholders", Map.of("x", 1))).isEqualTo("No placeholders");
    }
}
