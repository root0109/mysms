package com.mysms.messaging;

import java.util.List;

/** The source modules whose events the broker adapters can route. Keep this list aligned with the reactor POM. */
public final class MessagingModules {

    public static final List<String> ALL = List.of("master", "platform", "iam", "org", "people", "academics", "sis",
            "hr", "payroll", "admission", "attendance", "facility", "timetable", "assessment", "lms", "finance",
            "library", "transport", "comms", "frontoffice", "careers", "privacy");

    private MessagingModules() {
    }
}
