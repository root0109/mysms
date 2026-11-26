package com.mysms.core.testkit;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * The module boundary checks every module runs as a unit test: its SQL names no schema but its areas' own, and it
 * imports only the gRPC contracts of its areas and of the areas in their rows of ARCHITECTURE.md's allowed-dependency
 * table. An area is one schema with its contract; a module serves one or more areas (its pom's migration folders).
 */
public final class ModuleChecks {

    /** The 22 area schemas. */
    public static final Set<String> SCHEMAS = Set.of("master", "platform", "iam", "org", "people", "academics", "sis",
            "hr", "payroll", "admission", "attendance", "facility", "timetable", "assessment", "lms", "finance",
            "library", "transport", "comms", "frontoffice", "careers", "privacy");

    private static final Pattern TEXT_BLOCK = Pattern.compile("\"\"\"(.*?)\"\"\"", Pattern.DOTALL);
    private static final Pattern STRING = Pattern.compile("\"((?:[^\"\\\\\\n]|\\\\.)*)\"");
    private static final Pattern SQL = Pattern.compile("\\b(SELECT|INSERT|UPDATE|DELETE|FROM|JOIN|INTO|TABLE)\\b",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern STATEMENT = Pattern.compile("^\\s*(SELECT|INSERT|UPDATE|DELETE|WITH)\\b",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern UNQUALIFIED = Pattern.compile(
            "(?<!\\bDO\\s)\\b(FROM|JOIN|INTO|UPDATE)\\s+([a-z_][a-z0-9_]*)\\b(?!\\s*[.(])", Pattern.CASE_INSENSITIVE);
    private static final Set<String> NOT_TABLES = Set.of("lateral", "unnest", "only", "generate_series");
    private static final Pattern CTE_NAME = Pattern.compile(
            "(?:\\bWITH(?:\\s+RECURSIVE)?\\s+|,\\s*)([a-z_][a-z0-9_]*)\\s+AS\\s*\\(", Pattern.CASE_INSENSITIVE);
    private static final Pattern QUALIFIED = Pattern.compile("\\b([a-z]+)\\.([a-z_][a-z0-9_]*)\\b");
    private static final Pattern GRPC_IMPORT =
            Pattern.compile("^import\\s+(?:static\\s+)?com\\.mysms\\.([a-z]+)\\.v1\\.", Pattern.MULTILINE);
    private static final Pattern ROW = Pattern.compile("^\\| `([a-z]+)` \\| (.*) \\|$", Pattern.MULTILINE);
    private static final Pattern CELL = Pattern.compile("`([a-z]+)`");
    private static final Pattern MIGRATIONS = Pattern.compile("\\$\\{mysms\\.migrations\\.dir\\}/([a-z]+)<");

    private ModuleChecks() {
    }

    /** The schemas (areas) of a module: the migration folders its pom packages ({@code ${mysms.migrations.dir}/x}). */
    public static Set<String> moduleSchemas(Path pom) {
        Set<String> schemas = new TreeSet<>();
        Matcher folders = MIGRATIONS.matcher(read(pom));
        while (folders.find()) {
            schemas.add(folders.group(1));
        }
        if (schemas.isEmpty()) {
            throw new IllegalArgumentException(pom + " packages no migrations");
        }
        return schemas;
    }

    /**
     * Violations of the own-schema rule: "File: other.table" for every SQL text naming a schema outside the module's
     * own, in Java string literals and text blocks and in .sql resources.
     */
    public static List<String> foreignSchemaReferences(Set<String> ownSchemas, Path sourceRoot) {
        List<String> violations = new ArrayList<>();
        for (Path file : sqlSources(sourceRoot)) {
            for (String sql : sqlTexts(file)) {
                Matcher qualified = QUALIFIED.matcher(sql);
                while (qualified.find()) {
                    String schema = qualified.group(1);
                    if (SCHEMAS.contains(schema) && !ownSchemas.contains(schema)) {
                        violations.add(file.getFileName() + ": " + qualified.group());
                    }
                }
            }
        }
        return violations;
    }

    /**
     * Table references without a schema ("File: FROM item"): every table must be schema-qualified so the own-schema
     * check sees it. A name the statement declares as a CTE is not a table.
     */
    public static List<String> unqualifiedTableReferences(Path sourceRoot) {
        List<String> violations = new ArrayList<>();
        for (Path file : sqlSources(sourceRoot)) {
            for (String sql : sqlTexts(file)) {
                if (!STATEMENT.matcher(sql).find()) {
                    continue;
                }
                Set<String> cteNames = new HashSet<>();
                Matcher cte = CTE_NAME.matcher(sql);
                while (cte.find()) {
                    cteNames.add(cte.group(1).toLowerCase(java.util.Locale.ROOT));
                }
                Matcher unqualified = UNQUALIFIED.matcher(sql);
                while (unqualified.find()) {
                    String name = unqualified.group(2).toLowerCase(java.util.Locale.ROOT);
                    if (!NOT_TABLES.contains(name) && !cteNames.contains(name)) {
                        violations.add(file.getFileName() + ": " + unqualified.group().replaceAll("\\s+", " "));
                    }
                }
            }
        }
        return violations;
    }

    /** SQL-looking texts of a file: the literals of a .java file that contain SQL keywords, or a whole .sql file. */
    private static List<String> sqlTexts(Path file) {
        String source = read(file);
        if (file.toString().endsWith(".sql")) {
            return List.of(source);
        }
        return literals(source).stream().filter(l -> SQL.matcher(l).find()).toList();
    }

    private static List<Path> sqlSources(Path sourceRoot) {
        List<Path> files = new ArrayList<>(javaFiles(sourceRoot));
        Path resources = sourceRoot.getParent() == null ? null : sourceRoot.getParent().resolve("resources");
        if (resources != null && Files.isDirectory(resources)) {
            try (Stream<Path> walk = Files.walk(resources)) {
                walk.filter(p -> p.toString().endsWith(".sql")).sorted().forEach(files::add);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
        return files;
    }

    /**
     * Violations of the gRPC allowlist: "File.java imports the gRPC contract of x" for every import of
     * {@code com.mysms.<x>.v1} outside the module's areas and their rows (the shared {@code common} is always allowed).
     */
    public static List<String> grpcImportsOutsideAllowlist(Set<String> areas, Path sourceRoot, Path architectureMd) {
        Set<String> allowed = new TreeSet<>(areas);
        for (String area : areas) {
            allowed.addAll(allowedDependencies(area, architectureMd));
        }
        allowed.add("common");
        List<String> violations = new ArrayList<>();
        for (Path file : javaFiles(sourceRoot)) {
            Matcher imports = GRPC_IMPORT.matcher(read(file));
            while (imports.find()) {
                if (!allowed.contains(imports.group(1))) {
                    violations.add(file.getFileName() + " imports the gRPC contract of " + imports.group(1));
                }
            }
        }
        return violations;
    }

    /** The area's row of the "May depend on" table in ARCHITECTURE.md. */
    public static Set<String> allowedDependencies(String area, Path architectureMd) {
        Matcher rows = ROW.matcher(read(architectureMd));
        while (rows.find()) {
            if (rows.group(1).equals(area)) {
                Set<String> allowed = new TreeSet<>();
                Matcher cells = CELL.matcher(rows.group(2));
                while (cells.find()) {
                    allowed.add(cells.group(1));
                }
                return allowed;
            }
        }
        throw new IllegalArgumentException("area " + area + " has no row in " + architectureMd);
    }

    private static List<String> literals(String source) {
        List<String> out = new ArrayList<>();
        Matcher blocks = TEXT_BLOCK.matcher(source);
        while (blocks.find()) {
            out.add(blocks.group(1));
        }
        Matcher strings = STRING.matcher(TEXT_BLOCK.matcher(source).replaceAll(""));
        while (strings.find()) {
            out.add(strings.group(1));
        }
        return out;
    }

    private static List<Path> javaFiles(Path root) {
        if (!Files.isDirectory(root)) {
            return List.of();
        }
        try (Stream<Path> files = Files.walk(root)) {
            return files.filter(p -> p.toString().endsWith(".java")).sorted().toList();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static String read(Path file) {
        try {
            return Files.readString(file, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
