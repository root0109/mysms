package com.mysms.messaging;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The source areas whose events the broker adapters can route, and the module that serves and publishes for each
 * (JAVA-CODING-RULES §2.2). An event names its source area; a module deployed as its own service publishes every one
 * of its areas' events as one broker user. Conformance G9 keeps this table equal to the modules' areas.
 */
public final class MessagingModules {

    private static final Map<String, String> MODULE_OF_AREA = moduleOfArea();

    /** Every source area. */
    public static final List<String> AREAS = List.copyOf(MODULE_OF_AREA.keySet());

    private MessagingModules() {
    }

    /**
     * @param area an event's source area ({@code payroll})
     * @return the module that serves it ({@code hrms}), or {@code null} for an area no module serves
     */
    public static String moduleOf(String area) {
        return MODULE_OF_AREA.get(area);
    }

    private static Map<String, String> moduleOfArea() {
        Map<String, String> areas = new LinkedHashMap<>();
        serve(areas, "platform", "platform", "master", "org");
        serve(areas, "identity", "iam", "people");
        serve(areas, "privacy", "privacy");
        serve(areas, "comms", "comms");
        serve(areas, "hrms", "hr", "payroll", "time_attendance");
        serve(areas, "students", "sis", "admission", "careers");
        serve(areas, "academics", "academics", "timetable", "attendance", "assessment");
        serve(areas, "finance", "finance");
        serve(areas, "campus", "library", "transport", "frontoffice", "facility");
        serve(areas, "learning", "lms");
        return Collections.unmodifiableMap(areas);
    }

    private static void serve(Map<String, String> areas, String module, String... served) {
        for (String area : served) {
            if (areas.putIfAbsent(area, module) != null) {
                throw new IllegalStateException("area " + area + " is served by two modules");
            }
        }
    }
}
