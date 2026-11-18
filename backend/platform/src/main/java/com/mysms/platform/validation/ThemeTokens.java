package com.mysms.platform.validation;

import com.mysms.core.error.FieldError;
import com.mysms.core.error.ValidationFailedException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * The design tokens a tenant may override: brand colours with optional dark variants, the radius by one notch, the UI
 * font from an allow-list. Semantic intents, spacing, the type scale and the focus ring are never overridable, so their
 * keys are refused. Values are checked by kind: colours are {@code #RRGGBB}.
 */
public final class ThemeTokens {

    private static final Set<String> COLOURS = Stream.of("color.primary", "color.on-primary",
                    "color.primary-container", "color.secondary", "color.on-secondary", "color.secondary-container")
            .flatMap(key -> Stream.of(key, key + ".dark")).collect(Collectors.toUnmodifiableSet());
    private static final Map<String, Set<String>> CHOICES = Map.of(
            "font.family", Set.of("Inter", "Noto Sans", "Roboto", "Open Sans", "Poppins"),
            "radius.step", Set.of("-1", "0", "1"));
    private static final Pattern HEX_COLOUR = Pattern.compile("#[0-9A-Fa-f]{6}");
    private static final String FIELD = "tokens";

    private ThemeTokens() {
    }

    /**
     * The request's tokens as key → value, sorted by key; 422 for anything but a JSON object of strings with known keys
     * and valid values (every failing key reported at once).
     */
    public static Map<String, String> parse(Object tokens) {
        if (!(tokens instanceof Map<?, ?> given)) {
            throw new ValidationFailedException(List.of(new FieldError(FIELD,
                    "an object of token keys to string values", "Type")));
        }
        Map<String, String> parsed = new TreeMap<>();
        List<FieldError> errors = new ArrayList<>();
        for (Map.Entry<?, ?> entry : given.entrySet()) {
            String key = String.valueOf(entry.getKey());
            String field = FIELD + "." + key;
            if (!(entry.getValue() instanceof String value)) {
                errors.add(new FieldError(field, "must be a string", "Type"));
            } else if (COLOURS.contains(key)) {
                if (!HEX_COLOUR.matcher(value).matches()) {
                    errors.add(new FieldError(field, "a colour as #RRGGBB", "Pattern"));
                }
                parsed.put(key, value.toUpperCase(Locale.ROOT));
            } else if (CHOICES.containsKey(key)) {
                if (!CHOICES.get(key).contains(value)) {
                    errors.add(new FieldError(field, "one of " + CHOICES.get(key).stream().sorted().toList(),
                            "AllowedValues"));
                }
                parsed.put(key, value);
            } else {
                errors.add(new FieldError(field, "not a token a tenant may override", "UnknownToken"));
            }
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
        return parsed;
    }
}
