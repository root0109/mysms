package com.mysms.demoseed;

import java.net.URI;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;

/**
 * What one run does and against which installation. Plain settings come from system properties
 * ({@code -Dmysms.demo.*}); credentials come only from environment variables, so they never appear on a command line
 * or in a log.
 *
 * @param tenantCode        the demo tenant; must start with {@code demo-}
 * @param studentsPerClass  students in each of the 20 class sections (the size of the school)
 * @param pastWeeks         whole weeks before this one with a timetable and daily attendance
 * @param platformToken     a platform operator's access token, needed by {@code remove} only (blank = none)
 * @param today             the day the school's calendar is built around (its academic year, attendance weeks)
 */
public record DemoSettings(URI baseUrl, Action action, String tenantCode, String adminUsername, String adminPassword,
                           String personaPassword, String platformToken, int studentsPerClass, int pastWeeks,
                           long randomSeed, Path manifestDirectory, LocalDate today) {

    /** Every demo tenant's code starts with this, and the seeder touches no other tenant. */
    public static final String DEMO_PREFIX = "demo-";

    /** What a run does. */
    public enum Action { SEED, REMOVE }

    public DemoSettings {
        Objects.requireNonNull(baseUrl, "mysms.demo.baseUrl");
        Objects.requireNonNull(action, "mysms.demo.action");
        if (tenantCode == null || !tenantCode.startsWith(DEMO_PREFIX) || tenantCode.length() <= DEMO_PREFIX.length()) {
            throw new IllegalArgumentException("the demo tenant's code must start with '" + DEMO_PREFIX + "'");
        }
        if (studentsPerClass < 1) {
            throw new IllegalArgumentException("mysms.demo.studentsPerClass must be at least 1");
        }
        if (pastWeeks < 0) {
            throw new IllegalArgumentException("mysms.demo.pastWeeks must not be negative");
        }
    }

    /** The settings of a run started from the command line. */
    public static DemoSettings from(Properties properties, Map<String, String> environment, LocalDate today) {
        String code = properties.getProperty("mysms.demo.tenantCode",
                DEMO_PREFIX + today.format(DateTimeFormatter.BASIC_ISO_DATE));
        return new DemoSettings(URI.create(required(properties.getProperty("mysms.demo.baseUrl"), "mysms.demo.baseUrl")),
                Action.valueOf(required(properties.getProperty("mysms.demo.action"), "mysms.demo.action")
                        .toUpperCase(java.util.Locale.ROOT)),
                code,
                environment.getOrDefault("MYSMS_DEMO_ADMIN_USERNAME", ""),
                environment.getOrDefault("MYSMS_DEMO_ADMIN_PASSWORD", ""),
                environment.getOrDefault("MYSMS_DEMO_PERSONA_PASSWORD", ""),
                environment.getOrDefault("MYSMS_DEMO_PLATFORM_TOKEN", ""),
                Integer.parseInt(properties.getProperty("mysms.demo.studentsPerClass", "15")),
                Integer.parseInt(properties.getProperty("mysms.demo.pastWeeks", "3")),
                Long.parseLong(properties.getProperty("mysms.demo.seed", "20261118")),
                Path.of(properties.getProperty("mysms.demo.manifestDir", "demo-manifests")),
                LocalDate.parse(properties.getProperty("mysms.demo.today", today.toString())));
    }

    private static String required(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " is required");
        }
        return value;
    }

    /** Leaves the credentials out, so the settings can be printed. */
    @Override
    public String toString() {
        return "DemoSettings[baseUrl=" + baseUrl + ", action=" + action + ", tenantCode=" + tenantCode
                + ", studentsPerClass=" + studentsPerClass + ", pastWeeks=" + pastWeeks + ", randomSeed=" + randomSeed
                + ", manifestDirectory=" + manifestDirectory + ", today=" + today + "]";
    }
}
