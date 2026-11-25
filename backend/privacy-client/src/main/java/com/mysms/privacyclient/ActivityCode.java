package com.mysms.privacyclient;

import java.util.regex.Pattern;

/**
 * A module's processing activity, as privacy's register lists it (privacy.processing_activity.code), e.g.
 * {@code comms.marketing_broadcast}: the module that processes, then what it does. The module part is also who asks:
 * the check is made as that module's service principal.
 *
 * @param value {@code <module>.<activity>}, lower case
 */
public record ActivityCode(String value) {

    private static final Pattern FORM = Pattern.compile("[a-z]+\\.[a-z_]+");

    public ActivityCode {
        if (value == null || !FORM.matcher(value).matches()) {
            throw new IllegalArgumentException("an activity code is <module>.<activity> in lower case: " + value);
        }
    }

    /** The module that processes, and asks. */
    public String module() {
        return value.substring(0, value.indexOf('.'));
    }

    @Override
    public String toString() {
        return value;
    }
}
