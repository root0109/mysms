package com.mysms.conformance;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * The repository as the rules see it: the Maven modules under {@code backend/}, their sources, and the migrations.
 * Files are read once and shared by every rule; paths in violations are relative to the repository root.
 */
public final class SourceTree {

    /** Modules that are libraries or the assembly, not one of the 22 domain modules. */
    public static final Set<String> SHARED =
            Set.of("core", "auth-client", "privacy-client", "messaging", "app", "conformance");

    private static final Pattern CREATE_TABLE =
            Pattern.compile("(?i)CREATE TABLE ([a-z_]+\\.[a-z_]+) \\((.*?)\\n\\)", Pattern.DOTALL);
    private static final Pattern TENANT_COLUMN = Pattern.compile("(?m)^\\s+tenant_id\\s");

    private final Path repository;
    private final Map<Path, String> contents = new ConcurrentHashMap<>();
    private final List<String> modules;
    private final Set<String> tenantTables;

    public SourceTree(Path repository) {
        this.repository = repository.toAbsolutePath().normalize();
        try (Stream<Path> children = Files.list(backend())) {
            this.modules = children.filter(dir -> Files.isRegularFile(dir.resolve("pom.xml")))
                    .map(SourceTree::fileName).sorted().toList();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        this.tenantTables = readTenantTables();
    }

    public Path repository() {
        return repository;
    }

    public Path backend() {
        return repository.resolve("backend");
    }

    /** Every Maven module under backend/, sorted. */
    public List<String> modules() {
        return Collections.unmodifiableList(modules);
    }

    /**
     * The modules whose code the rules judge: every module except this check itself, which has to name what it bans.
     */
    public List<String> productModules() {
        return modules.stream().filter(m -> !"conformance".equals(m)).toList();
    }

    /** The 22 domain modules (one per schema). */
    public List<String> domainModules() {
        return modules.stream().filter(m -> !SHARED.contains(m)).toList();
    }

    /** Java files of a module's source root ({@code main}, {@code test} or {@code component-test}), sorted. */
    public List<Path> java(String module, String root) {
        return files(backend().resolve(module).resolve("src").resolve(root).resolve("java"), ".java");
    }

    /** Files under a directory with a name suffix, recursively and sorted; empty when the directory is missing. */
    public List<Path> files(Path directory, String suffix) {
        if (!Files.isDirectory(directory)) {
            return List.of();
        }
        try (Stream<Path> walk = Files.walk(directory)) {
            return walk.filter(Files::isRegularFile).filter(p -> fileName(p).endsWith(suffix))
                    .sorted().toList();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** A module's migrations ({@code database/migrations/<module>}). */
    public List<Path> migrations(String module) {
        return files(repository.resolve("database").resolve("migrations").resolve(module), ".sql");
    }

    /** Every migration of every module. */
    public List<Path> migrations() {
        return files(repository.resolve("database").resolve("migrations"), ".sql");
    }

    /** The file's text (UTF-8), read once. */
    public String read(Path file) {
        return contents.computeIfAbsent(file.toAbsolutePath().normalize(), p -> {
            try {
                return Files.readString(p, StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });
    }

    /** The path relative to the repository root, with forward slashes. */
    public String relative(Path file) {
        return repository.relativize(file.toAbsolutePath().normalize()).toString().replace('\\', '/');
    }

    /** The path relative to backend/, with forward slashes (how rule exceptions name files). */
    public String relativeToBackend(Path file) {
        return backend().relativize(file.toAbsolutePath().normalize()).toString().replace('\\', '/');
    }

    /** Every table the migrations create with a tenant column ({@code schema.table}). */
    public Set<String> tenantTables() {
        return Collections.unmodifiableSet(tenantTables);
    }

    /** The last element of a path ("" for a root, which has none). */
    public static String fileName(Path path) {
        return String.valueOf(path.getFileName());
    }

    private Set<String> readTenantTables() {
        return migrations().stream().flatMap(file -> {
            Matcher table = CREATE_TABLE.matcher(read(file));
            return table.results().filter(m -> TENANT_COLUMN.matcher(m.group(2)).find())
                    .map(m -> m.group(1).toLowerCase(Locale.ROOT));
        }).collect(Collectors.toUnmodifiableSet());
    }
}
