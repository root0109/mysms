package com.mysms.identity.iam.business;

import com.mysms.core.error.FieldError;
import com.mysms.identity.iam.domain.SettingDefinitionEntity;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * What a setting value may be (pure rules, no I/O): the definition's type, allowed values and range, the scopes it
 * may be set at, and the few keys with a shape of their own (time zones, screen ids).
 */
public final class SettingRules {

    public static final String TENANT = "tenant";
    public static final String INSTITUTION = "institution";
    public static final String DEPARTMENT = "department";
    public static final String CLASS_SECTION = "class_section";
    public static final String USER = "user";
    /** Broadest first: the order in which scopes override each other. */
    public static final List<String> CHAIN = List.of(TENANT, INSTITUTION, DEPARTMENT, CLASS_SECTION, USER);
    public static final int MAX_LENGTH = 500;

    private static final String TIMEZONE_KEY = "ui.timezone";
    private static final String LANDING_KEY = "ui.landingScreen";
    private static final Pattern SCREEN_ID = Pattern.compile("[A-Za-z0-9][A-Za-z0-9-]{0,39}");
    private static final Set<String> BOOLEANS = Set.of("true", "false");

    private SettingRules() {
    }

    /** Field errors of {@code value} for the definition (empty = valid). */
    public static List<FieldError> valueErrors(SettingDefinitionEntity definition, String value, String field) {
        List<FieldError> errors = new ArrayList<>();
        if (value == null) {
            errors.add(new FieldError(field, "must not be null", "NotNull"));
            return errors;
        }
        if (value.length() > MAX_LENGTH) {
            errors.add(new FieldError(field, "must be at most " + MAX_LENGTH + " characters", "Size"));
            return errors;
        }
        if (value.chars().anyMatch(Character::isISOControl)) {
            errors.add(new FieldError(field, "must not contain control characters", "Pattern"));
            return errors;
        }
        switch (definition.getValueType()) {
            case "boolean" -> {
                if (!BOOLEANS.contains(value)) {
                    errors.add(new FieldError(field, "must be true or false", "Pattern"));
                }
            }
            case "integer" -> integerErrors(definition, value, field, errors);
            case "choice" -> {
                List<String> allowed = definition.getAllowedValues() == null ? List.of() : definition.getAllowedValues();
                if (!allowed.contains(value)) {
                    errors.add(new FieldError(field, "must be one of " + String.join(", ", allowed), "Choice"));
                }
            }
            case "string" -> stringErrors(definition.getSettingKey(), value, field, errors);
            default -> errors.add(new FieldError(field, "has an unsupported setting type", "Type"));
        }
        return errors;
    }

    /** Whether a value may be stored at this scope of the definition. */
    public static boolean scopeAllowed(SettingDefinitionEntity definition, String scopeType) {
        return definition.getAllowedScopes().contains(scopeType);
    }

    /** The permission that makes administrator-only settings visible (setting administrators). */
    public static final String ADMINISTER = "iam.setting_value.read";

    /**
     * Whether the caller may see this setting at all: user-editable settings are everyone's; an administrator-only
     * setting (session timeout, anything users cannot set) exists only for callers who administer settings at one of
     * the scopes it can be set at — a department head does not see a setting only the tenant administrator sets.
     *
     * @param administeredScopes scope types the caller may administer setting values at
     */
    public static boolean visibleTo(SettingDefinitionEntity definition, java.util.Set<String> administeredScopes) {
        return userEditable(definition)
                || definition.getAllowedScopes().stream().anyMatch(administeredScopes::contains);
    }

    /** Users may set it for themselves (when no broader scope locks it). */
    public static boolean userEditable(SettingDefinitionEntity definition) {
        return definition.isUserEditable() && scopeAllowed(definition, USER);
    }

    private static void integerErrors(SettingDefinitionEntity definition, String value, String field,
                                      List<FieldError> errors) {
        int number;
        try {
            number = Integer.parseInt(value);
        } catch (NumberFormatException e) {
            errors.add(new FieldError(field, "must be a whole number", "Type"));
            return;
        }
        if (!value.equals(Integer.toString(number))) {
            errors.add(new FieldError(field, "must be written without signs or leading zeros", "Pattern"));
        } else if (definition.getMinValue() != null && number < definition.getMinValue()
                || definition.getMaxValue() != null && number > definition.getMaxValue()) {
            errors.add(new FieldError(field, "must be between " + definition.getMinValue() + " and "
                    + definition.getMaxValue(), "Range"));
        }
    }

    private static void stringErrors(String key, String value, String field, List<FieldError> errors) {
        if (TIMEZONE_KEY.equals(key) && !ZoneId.getAvailableZoneIds().contains(value)) {
            errors.add(new FieldError(field, "must be an IANA time zone such as Asia/Kolkata", "Pattern"));
        } else if (LANDING_KEY.equals(key) && !value.isEmpty() && !SCREEN_ID.matcher(value).matches()) {
            errors.add(new FieldError(field, "must be a screen id such as ACC-07", "Pattern"));
        }
    }
}
