package com.mysms.conformance.rules;

import com.mysms.conformance.ConformanceRule;
import com.mysms.conformance.SourceTree;
import com.mysms.conformance.Violation;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The boundaries of the domain modules:
 * <ul>
 * <li>A1 — a domain module depends only on core, auth-client, privacy-client and messaging. Messaging is
 * runtime-only, except for a module that uses its event API (EventEnvelope, EventHandler, EventSubscriptions,
 * RejectedEventException); no module imports RabbitMQ, Google Cloud Pub/Sub or messaging adapter/runtime types.</li>
 * <li>§3 — an area's packages are the layer roles only (an area named like its module sits in the module's root
 * package beside its sibling areas' packages), and SpotBugs analyses every one of them: the parent POM's
 * {@code mysms.spotbugs.onlyAnalyze} names each layer, and a module that sets its own names its root package and every
 * layer of every area.</li>
 * <li>§14 — an area's migrations live in database/migrations/&lt;area&gt; and touch only its module's schemas.</li>
 * </ul>
 */
public final class ModuleBoundaryRule implements ConformanceRule {

    private static final Set<String> ALLOWED_DEPENDENCIES = Set.of("core", "auth-client", "privacy-client", "mysms");
    private static final Set<String> LAYERS = Set.of("api", "controller", "grpc", "integration", "business",
            "repository", "domain", "model", "mapper", "validation", "errors", "events", "config", "constants");
    private static final String EVENT_API = "(EventEnvelope|EventHandler|EventSubscriptions|RejectedEventException)";
    private static final Pattern USES_EVENT_API =
            Pattern.compile("(?m)^import com\\.mysms\\.messaging\\.events\\." + EVENT_API + ";");
    private static final Pattern BROKER_IMPORT = Pattern.compile("(?m)^import (?:static )?(?:com\\.rabbitmq\\.|"
            + "com\\.google\\.cloud\\.pubsub\\.|com\\.google\\.pubsub\\.|com\\.google\\.api\\.gax\\.)");
    private static final Pattern RUNTIME_IMPORT = Pattern.compile(
            "(?m)^import (?:static )?com\\.mysms\\.messaging\\.(?!events\\." + EVENT_API + ";)");
    private static final Pattern DEPENDENCY = Pattern.compile("<dependency>(.*?)</dependency>", Pattern.DOTALL);
    private static final Pattern MYSMS_ARTIFACT =
            Pattern.compile("<groupId>com\\.mysms</groupId>\\s*<artifactId>([\\w-]+)</artifactId>");
    private static final String ANALYSED_PROPERTY = "mysms.spotbugs.onlyAnalyze";
    private static final Pattern ANALYSED = Pattern.compile("<" + ANALYSED_PROPERTY + ">([^<]*)</");
    private static final Pattern SQL_COMMENT = Pattern.compile("--[^\\n]*");
    private static final Pattern SQL_STRING = Pattern.compile("'(?:[^']|'')*'");
    private static final Pattern NAMED_SCHEMA = Pattern.compile(
            "(?i)\\b(?:TABLE(?:\\s+ONLY)?|INTO|UPDATE|REFERENCES|ON)\\s+([A-Za-z_]+)\\.[A-Za-z_]+");
    private static final Pattern SCHEMA_STATEMENT =
            Pattern.compile("(?i)\\b(?:ON\\s+SCHEMA|CREATE\\s+SCHEMA(?:\\s+IF\\s+NOT\\s+EXISTS)?)\\s+([A-Za-z_]+)");

    @Override
    public List<Violation> check(SourceTree tree) {
        List<Violation> found = new ArrayList<>();
        for (String module : tree.domainModules()) {
            dependencies(tree, module, found);
            packages(tree, module, found);
            for (SourceTree.Area area : tree.areas(module)) {
                migrations(tree, area, found);
            }
        }
        analysedLayers(tree, found);
        return found;
    }

    private static void dependencies(SourceTree tree, String module, List<Violation> found) {
        Path pom = tree.backend().resolve(module).resolve("pom.xml");
        List<Path> sources = tree.java(module, "main");
        boolean usesEventApi = sources.stream().anyMatch(f -> USES_EVENT_API.matcher(tree.read(f)).find());
        boolean dependsOnMessaging = false;
        Matcher dependency = DEPENDENCY.matcher(tree.read(pom));
        while (dependency.find()) {
            Matcher artifact = MYSMS_ARTIFACT.matcher(dependency.group(1));
            if (!artifact.find()) {
                continue;
            }
            String name = artifact.group(1);
            boolean messaging = "messaging".equals(name);
            boolean runtime = messaging && dependency.group(1).contains("<scope>runtime</scope>");
            dependsOnMessaging |= messaging;
            if (!ALLOWED_DEPENDENCIES.contains(name) && !(runtime || messaging && usesEventApi)) {
                found.add(new Violation("A1", tree.relative(pom), "depends on " + name));
            }
        }
        if (usesEventApi && !dependsOnMessaging) {
            found.add(new Violation("A1", tree.relative(pom), "uses the messaging event API without a dependency"));
        }
        for (Path file : sources) {
            String source = tree.read(file);
            if (BROKER_IMPORT.matcher(source).find() || RUNTIME_IMPORT.matcher(source).find()) {
                found.add(new Violation("A1", tree.relative(file),
                        "imports a broker/runtime type; modules may use only the messaging event API"));
            }
        }
    }

    /** SpotBugs skips the generated proto classes by naming the packages it analyses, so it must name each layer. */
    private static void analysedLayers(SourceTree tree, List<Violation> found) {
        Path parent = tree.backend().resolve("pom.xml");
        Matcher analysed = ANALYSED.matcher(tree.read(parent));
        if (!analysed.find()) {
            found.add(new Violation("§3", tree.relative(parent), "no " + ANALYSED_PROPERTY + " property"));
            return;
        }
        Set<String> named = Set.of(analysed.group(1).split(","));
        for (String layer : LAYERS) {
            if (!named.contains("com.mysms.${project.artifactId}." + layer + ".-")) {
                found.add(new Violation("§3", tree.relative(parent), "SpotBugs does not analyse the " + layer + " layer"));
            }
        }
        for (String module : tree.domainModules()) {
            Path pom = tree.backend().resolve(module).resolve("pom.xml");
            Matcher own = ANALYSED.matcher(tree.read(pom));
            if (!own.find()) {
                if (!tree.areas(module).equals(List.of(new SourceTree.Area(module, module)))) {
                    found.add(new Violation("§3", tree.relative(pom), "serves several areas and does not name the "
                            + "packages SpotBugs analyses"));
                }
                continue;
            }
            Set<String> packages = Set.of(own.group(1).split(","));
            if (!packages.contains("com.mysms." + module + ".*")) {
                found.add(new Violation("§3", tree.relative(pom), "SpotBugs does not analyse the root package"));
            }
            for (SourceTree.Area area : tree.areas(module)) {
                for (String layer : LAYERS) {
                    if (!packages.contains(area.packageName() + "." + layer + ".-")) {
                        found.add(new Violation("§3", tree.relative(pom), "SpotBugs does not analyse "
                                + area.packageName() + "." + layer));
                    }
                }
            }
        }
    }

    private static void packages(SourceTree tree, String module, List<Violation> found) {
        List<SourceTree.Area> areas = tree.areas(module);
        Set<String> nested = new HashSet<>();
        areas.stream().filter(a -> !a.name().equals(module)).forEach(a -> nested.add(a.packageSegment()));
        String root = "/com/mysms/" + module + "/";
        for (Path file : tree.java(module, "main")) {
            String path = tree.relative(file);
            if (!path.contains(root)) {
                found.add(new Violation("§3", path, "outside the module's package com.mysms." + module));
                continue;
            }
            String rest = path.substring(path.indexOf(root) + root.length());
            int slash = rest.indexOf('/');
            if (slash >= 0 && nested.contains(rest.substring(0, slash))) {
                rest = rest.substring(slash + 1);
                slash = rest.indexOf('/');
                if (slash < 0) {
                    found.add(new Violation("§3", path, "a class in an area's root package"));
                    continue;
                }
            }
            if (slash >= 0 && !LAYERS.contains(rest.substring(0, slash))) {
                found.add(new Violation("§3", path, "package " + rest.substring(0, slash) + " is not a layer role"));
            }
        }
    }

    private static void migrations(SourceTree tree, SourceTree.Area area, List<Violation> found) {
        Set<String> own = new HashSet<>(Set.of("pg_catalog", "public"));
        tree.areas(area.module()).forEach(a -> own.add(a.name()));
        for (Path file : tree.migrations(area)) {
            String sql = SQL_STRING.matcher(SQL_COMMENT.matcher(tree.read(file)).replaceAll(""))
                    .replaceAll("''").replace("\"", "");
            Set<String> schemas = new HashSet<>();
            NAMED_SCHEMA.matcher(sql).results().forEach(m -> schemas.add(m.group(1).toLowerCase(Locale.ROOT)));
            SCHEMA_STATEMENT.matcher(sql).results().forEach(m -> schemas.add(m.group(1).toLowerCase(Locale.ROOT)));
            schemas.removeAll(own);
            schemas.stream().sorted().forEach(other ->
                    found.add(new Violation("§14", tree.relative(file), "touches schema " + other)));
        }
    }
}
