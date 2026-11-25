package com.mysms.comms.business;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Fills a message template's {@code {{name}}} placeholders from the send's payload. A placeholder the payload does not
 * fill stays as written, so a missing value shows instead of disappearing silently.
 */
final class TemplateText {

    private static final Pattern PLACEHOLDER = Pattern.compile("\\{\\{\\s*([A-Za-z0-9_.-]+)\\s*}}");

    private TemplateText() {
    }

    /** The text with every placeholder the values name replaced; null stays null. */
    static String render(String text, Map<?, ?> values) {
        if (text == null) {
            return null;
        }
        Matcher placeholder = PLACEHOLDER.matcher(text);
        StringBuilder out = new StringBuilder();
        while (placeholder.find()) {
            Object value = values.get(placeholder.group(1));
            placeholder.appendReplacement(out, Matcher.quoteReplacement(value == null ? placeholder.group()
                    : String.valueOf(value)));
        }
        placeholder.appendTail(out);
        return out.toString();
    }
}
