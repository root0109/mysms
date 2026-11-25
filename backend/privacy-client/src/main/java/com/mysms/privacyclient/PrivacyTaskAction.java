package com.mysms.privacyclient;

import java.util.Locale;

/** What privacy asks a module to do with one subject's data in a rights-request task (design §9.1). */
public enum PrivacyTaskAction {
    EXPORT, ERASE, ANONYMISE, RESTRICT;

    /** The action as the task_assigned event names it: lower case. */
    public String value() {
        return name().toLowerCase(Locale.ROOT);
    }

    /** The action an event names; {@link IllegalArgumentException} for any other word. */
    public static PrivacyTaskAction of(String value) {
        for (PrivacyTaskAction action : values()) {
            if (action.value().equals(value)) {
                return action;
            }
        }
        throw new IllegalArgumentException("not a privacy task action: " + value);
    }
}
