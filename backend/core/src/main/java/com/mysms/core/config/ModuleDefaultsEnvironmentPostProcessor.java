package com.mysms.core.config;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

/**
 * Builds the configuration of a MySMS process from the modules it runs, so no module ships a configuration file or a
 * descriptor of itself. Which modules run is read from the application class: a module is a package
 * {@code com.mysms.<name>}, and the application says which packages it scans — a module's own
 * {@code @SpringBootApplication} scans its own package (that process is the module's microservice), the monolith
 * ({@code app}) lists every module's. The name is also the module's schema and its configuration namespace
 * ({@code mysms.<name>.*}). Registered in core's {@code META-INF/spring.factories}.
 *
 * <p>Every value is a placeholder over the environment variables of DEPLOYMENT.md §3 — a module's own variable first
 * ({@code MYSMS_FINANCE_DB_URL}), then the shared one ({@code MYSMS_DB_URL}) — and the whole map is added as the
 * lowest-precedence property source. Every standard Spring Boot source therefore overrides any single value: an
 * operator's {@code application.yml} beside the jar or in {@code config/}, environment variables, system properties
 * and the command line. No MySMS jar contains an {@code application.yml}, so nothing on the classpath competes. A
 * module's settings beyond its datasource are read where they are used, with the same fallback to the environment
 * (for example iam's {@code IamSecurityConfig}).
 */
public final class ModuleDefaultsEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    /** Name of the property source this adds; tests and diagnostics can find it by this name. */
    public static final String PROPERTY_SOURCE = "mysmsModuleDefaults";

    /** The application name of the process that runs several modules. */
    static final String MONOLITH = "mysms";

    private static final String MODULE_PACKAGES = "com.mysms.";

    /** Packages under com.mysms that are libraries or the monolith's shell, not modules with a schema. */
    private static final Set<String> NOT_MODULES =
            Set.of("app", "core", "messaging", "authclient", "sdk", "conformance");

    /** The datasource keys of one module and the suffix of the environment variables that set them. */
    private static final Map<String, String> DATASOURCE = datasourceKeys();

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        List<String> modules = modulesOf(application.getAllSources());
        if (modules.isEmpty()) {
            // a library used without any module (core's or messaging's own tests): nothing to configure
            return;
        }
        environment.getPropertySources().addLast(new MapPropertySource(PROPERTY_SOURCE, defaults(modules)));
    }

    /** Last in the chain: the defaults only fill what nothing else has set. */
    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }

    /**
     * The modules an application runs, sorted by name: for each application class, the packages it scans (its own
     * when it names none) that are {@code com.mysms.<module>} packages.
     */
    static List<String> modulesOf(Collection<?> sources) {
        Set<String> names = new TreeSet<>();
        for (Object source : sources) {
            if (!(source instanceof Class<?> application)) {
                continue;
            }
            ComponentScan scan = AnnotatedElementUtils.findMergedAnnotation(application, ComponentScan.class);
            List<String> packages = new ArrayList<>();
            if (scan != null) {
                packages.addAll(List.of(scan.basePackages()));
                for (Class<?> marker : scan.basePackageClasses()) {
                    packages.add(marker.getPackageName());
                }
            }
            if (packages.isEmpty()) {
                packages.add(application.getPackageName());
            }
            for (String scanned : packages) {
                if (scanned.startsWith(MODULE_PACKAGES)) {
                    String name = scanned.substring(MODULE_PACKAGES.length()).split("\\.", 2)[0];
                    if (!NOT_MODULES.contains(name)) {
                        names.add(name);
                    }
                }
            }
        }
        return List.copyOf(names);
    }

    /** The defaults for these modules, in a stable order (module names sorted) so every start sees the same map. */
    static Map<String, Object> defaults(List<String> modules) {
        List<String> sorted = modules.stream().sorted().toList();
        boolean monolith = sorted.size() > 1;
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("spring.application.name", monolith ? MONOLITH : sorted.get(0));
        values.put("spring.threads.virtual.enabled", "true");
        values.put("server.port", "${PORT:8080}");
        values.put("server.shutdown", "graceful");
        values.put("management.endpoints.web.exposure.include", "health,info");
        values.put("management.endpoint.health.probes.enabled", "true");
        // the OIDC issuer (iam, or an external IdP) has no default: a service refuses to start without it
        values.put("mysms.auth.issuer", "${MYSMS_AUTH_ISSUER}");
        values.put("mysms.auth.jwk-set-uri", "${MYSMS_AUTH_JWK_SET_URI:}");
        // the OpenAPI document and Swagger UI stay closed unless the operator opens them (data security first)
        values.put("mysms.auth.api-docs", "${MYSMS_API_DOCS:false}");
        for (int i = 0; i < sorted.size(); i++) {
            values.put("mysms.auth.modules[" + i + "]", sorted.get(i));
        }
        // rabbitmq (default) | google-pubsub | in-process (one JVM) | none (tests only)
        values.put("mysms.events.bus", "${MYSMS_EVENTS_BUS:rabbitmq}");
        values.put("mysms.events.rabbitmq.uri", "${MYSMS_RABBITMQ_URI:}");
        // a microservice publishes as its module's broker user (blank = mysms-<module>); the monolith publishes for
        // every module as one broker user of its own ("self")
        values.put("mysms.events.rabbitmq.publisher-user",
                "${MYSMS_RABBITMQ_PUBLISHER_USER:" + (monolith ? "self" : "") + "}");
        values.put("mysms.events.google-pubsub.project-id", "${MYSMS_EVENTS_GCP_PROJECT_ID:${GOOGLE_CLOUD_PROJECT:}}");
        values.put("mysms.events.google-pubsub.emulator-host", "${PUBSUB_EMULATOR_HOST:}");
        for (String module : sorted) {
            datasource(module, values);
        }
        return values;
    }

    /**
     * {@code mysms.<module>.datasource.*} (core's ModuleDatabaseSettings): the module's own variable wins, then the
     * shared one; the URL and user fall back to a local development database.
     */
    private static void datasource(String module, Map<String, Object> values) {
        String prefix = "MYSMS_" + module.toUpperCase(Locale.ROOT) + "_";
        DATASOURCE.forEach((key, variable) -> values.put("mysms." + module + ".datasource." + key,
                "${" + prefix + variable + ":" + sharedDefault(variable) + "}"));
    }

    private static String sharedDefault(String variable) {
        return switch (variable) {
            case "DB_URL" -> "${MYSMS_DB_URL:jdbc:postgresql://localhost:5432/mysms}";
            case "DB_USER" -> "${MYSMS_DB_USER:mysms}";
            // the pool size has no shared variable: each schema's pool is sized on its own (DEPLOYMENT.md §3)
            case "DB_POOL_SIZE" -> "10";
            // statements slower than this are named in the log; 0 switches the report off
            case "DB_SLOW_STATEMENT" -> "${MYSMS_DB_SLOW_STATEMENT:200ms}";
            default -> "${MYSMS_" + variable + ":}";
        };
    }

    private static Map<String, String> datasourceKeys() {
        Map<String, String> keys = new LinkedHashMap<>();
        keys.put("url", "DB_URL");
        keys.put("username", "DB_USER");
        keys.put("password", "DB_PASSWORD");
        keys.put("maximum-pool-size", "DB_POOL_SIZE");
        keys.put("slow-statement", "DB_SLOW_STATEMENT");
        // a streaming replica read-only transactions use; blank = every read on the primary
        keys.put("replica-url", "DB_REPLICA_URL");
        keys.put("replica-username", "DB_REPLICA_USER");
        keys.put("replica-password", "DB_REPLICA_PASSWORD");
        // the schema owner Flyway runs as; the application role only gets DML grants (database/README "Roles")
        keys.put("migrate-username", "DB_MIGRATE_USER");
        keys.put("migrate-password", "DB_MIGRATE_PASSWORD");
        // the outbox relay's role (mysms_relay), granted the outbox and inbox only
        keys.put("relay-username", "DB_RELAY_USER");
        keys.put("relay-password", "DB_RELAY_PASSWORD");
        return Collections.unmodifiableMap(keys);
    }
}
