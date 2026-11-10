package com.mysms.conformance.rules;

import com.mysms.conformance.ConformanceRule;
import com.mysms.conformance.SourceTree;
import com.mysms.conformance.Violation;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

/**
 * M0: backend/ is the Maven multi-module project and nothing else — poms, sources, build configuration; no scripts
 * and no tooling (every tool is Java code inside the build). Eclipse's own project files are tolerated.
 */
public final class MavenLayoutRule implements ConformanceRule {

    private static final Set<String> ECLIPSE = Set.of(".classpath", ".factorypath", ".project", ".settings");
    private static final Set<String> MODULE_ENTRIES = Set.of("pom.xml", "src", "target", "README.md");
    private static final List<String> SCRIPTS =
            List.of(".py", ".sh", ".js", ".mjs", ".ts", ".rb", ".pl", ".bat", ".ps1");

    @Override
    public List<Violation> check(SourceTree tree) {
        List<Violation> found = new ArrayList<>();
        Set<String> allowedTop = new HashSet<>(Set.of("pom.xml", "README.md", ".gitignore", ".mvn", ".metadata",
                "config", "target"));
        allowedTop.addAll(tree.modules());
        for (Path entry : list(tree.backend())) {
            if (!allowedTop.contains(name(entry))) {
                found.add(new Violation("M0", tree.relative(entry), "not part of the Maven project"));
            }
        }
        for (String extra : List.of("config", ".mvn")) {
            for (Path file : tree.files(tree.backend().resolve(extra), "")) {
                String name = name(file);
                if (!(name.endsWith(".xml") || name.endsWith(".config") || name.endsWith(".properties"))) {
                    found.add(new Violation("M0", tree.relative(file), "not build configuration"));
                }
            }
        }
        for (String module : tree.modules()) {
            Path dir = tree.backend().resolve(module);
            for (Path entry : list(dir)) {
                if (!MODULE_ENTRIES.contains(name(entry)) && !ECLIPSE.contains(name(entry))) {
                    found.add(new Violation("M0", tree.relative(entry), "not part of the Maven module"));
                }
            }
            for (Path file : tree.files(dir.resolve("src"), "")) {
                if (SCRIPTS.stream().anyMatch(name(file)::endsWith)) {
                    found.add(new Violation("M0", tree.relative(file), "script inside a Maven module"));
                }
            }
        }
        return found;
    }

    private static String name(Path path) {
        return SourceTree.fileName(path);
    }

    private static List<Path> list(Path directory) {
        try (Stream<Path> entries = Files.list(directory)) {
            return entries.sorted().toList();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
