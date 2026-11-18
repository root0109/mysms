package com.mysms.conformance.rules;

import com.mysms.conformance.ConformanceRule;
import com.mysms.conformance.SourceTree;
import com.mysms.conformance.Violation;
import java.io.IOException;
import java.io.StringReader;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * G9: every domain module is configured by its own {@code src/main/resources/application.properties}, and the monolith
 * combines them.
 * <ul>
 * <li>each domain module has the file; it names the module ({@code spring.application.name},
 * {@code mysms.auth.modules[0]}) and configures a gRPC client for every service its {@code integration} package
 * calls;</li>
 * <li>{@code app}'s file names every module ({@code mysms.auth.modules[i]}) and points a client at the in-process
 * server for every module's service;</li>
 * <li>a key two module files set to different values is set in {@code app}'s file, so the order in which the monolith
 * combines the module files never decides a value;</li>
 * <li>the libraries (core, auth-client, messaging, conformance) ship no {@code application.properties}, which would
 * shadow or join a service's;</li>
 * <li>the test kit's list of services ({@code TestDatabase.SERVICES}) is the domain modules.</li>
 * </ul>
 */
public final class ConfigurationRule implements ConformanceRule {

    private static final String FILE = "src/main/resources/application.properties";
    private static final Set<String> LIBRARIES = Set.of("core", "auth-client", "messaging", "conformance");
    private static final Pattern SERVICE_KEY = Pattern.compile("SERVICE_KEY = \"([a-z]+)-service\"");
    private static final Pattern SERVICES = Pattern.compile("SERVICES = List\\.of\\(([^;]*)\\);");
    private static final Pattern NAME = Pattern.compile("\"([a-z]+)\"");

    @Override
    public List<Violation> check(SourceTree tree) {
        List<Violation> found = new ArrayList<>();
        Map<String, Map<String, String>> values = new TreeMap<>();
        for (String module : tree.domainModules()) {
            Path file = tree.backend().resolve(module).resolve(FILE);
            if (!Files.isRegularFile(file)) {
                found.add(new Violation("G9", tree.relative(tree.backend().resolve(module)), "no " + FILE));
                continue;
            }
            Properties properties = load(tree, file);
            moduleFile(tree, module, file, properties, found);
            for (String key : properties.stringPropertyNames()) {
                values.computeIfAbsent(key, k -> new TreeMap<>()).put(module, properties.getProperty(key));
            }
        }
        Path app = tree.backend().resolve("app").resolve(FILE);
        Properties monolith = Files.isRegularFile(app) ? load(tree, app) : new Properties();
        appFile(tree, app, monolith, found);
        values.forEach((key, byModule) -> {
            if (new TreeSet<>(byModule.values()).size() > 1 && !monolith.containsKey(key)) {
                found.add(new Violation("G9", tree.relative(app), key + " differs between " + byModule.keySet()
                        + " and is not set here"));
            }
        });
        for (String library : LIBRARIES) {
            Path file = tree.backend().resolve(library).resolve(FILE);
            if (Files.exists(file)) {
                found.add(new Violation("G9", tree.relative(file), "a library ships no application.properties"));
            }
        }
        testKitServices(tree, found);
        return found;
    }

    private static void moduleFile(SourceTree tree, String module, Path file, Properties properties,
                                   List<Violation> found) {
        String where = tree.relative(file);
        if (!module.equals(properties.getProperty("spring.application.name"))) {
            found.add(new Violation("G9", where, "spring.application.name is not " + module));
        }
        if (!module.equals(properties.getProperty("mysms.auth.modules[0]"))) {
            found.add(new Violation("G9", where, "mysms.auth.modules[0] is not " + module));
        }
        for (Path source : tree.java(module, "main")) {
            if (!tree.relative(source).contains("/com/mysms/" + module + "/integration/")) {
                continue;
            }
            Matcher key = SERVICE_KEY.matcher(tree.read(source));
            while (key.find()) {
                String client = "grpc.client." + key.group(1) + "-service.address";
                if (!properties.containsKey(client)) {
                    found.add(new Violation("G9", where, "no " + client + " for " + tree.relative(source)));
                }
            }
        }
    }

    private static void appFile(SourceTree tree, Path app, Properties monolith, List<Violation> found) {
        String where = tree.relative(app);
        List<String> modules = tree.domainModules();
        for (int i = 0; i < modules.size(); i++) {
            if (!modules.get(i).equals(monolith.getProperty("mysms.auth.modules[" + i + "]"))) {
                found.add(new Violation("G9", where, "mysms.auth.modules[" + i + "] is not " + modules.get(i)));
            }
        }
        for (String module : modules) {
            String address = monolith.getProperty("grpc.client." + module + "-service.address", "");
            if (!address.contains("in-process:")) {
                found.add(new Violation("G9", where, "the " + module + " client does not call the in-process server"));
            }
        }
    }

    private static void testKitServices(SourceTree tree, List<Violation> found) {
        Path kit = tree.backend().resolve("core/src/test/java/com/mysms/core/testkit/TestDatabase.java");
        Matcher list = SERVICES.matcher(tree.read(kit).replace("\n", " "));
        Set<String> services = new TreeSet<>();
        if (list.find()) {
            Matcher name = NAME.matcher(list.group(1));
            while (name.find()) {
                services.add(name.group(1));
            }
        }
        if (!services.equals(new TreeSet<>(tree.domainModules()))) {
            found.add(new Violation("G9", tree.relative(kit), "SERVICES " + services + " is not the domain modules"));
        }
    }

    private static Properties load(SourceTree tree, Path file) {
        Properties properties = new Properties();
        try {
            properties.load(new StringReader(tree.read(file)));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return properties;
    }
}
