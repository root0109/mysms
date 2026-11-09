package com.mysms.core.testkit;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * The module boundary checks every module runs as a plain unit test (JAVA-CODING-RULES §2; no ArchUnit, no Modulith):
 * <ul>
 *   <li><b>own-schema SQL</b> (R3): SQL text in the module references no other module's schema;</li>
 *   <li><b>SDK allowlist</b>: the module imports SDK clients only of modules in its row of the allowed-dependency
 *   table in ARCHITECTURE.md.</li>
 * </ul>
 */
public final class ModuleChecks {

    /** The 21 module schemas. */
    public static final Set<String> MODULES = Set.of("master", "platform", "iam", "org", "people", "academics", "sis",
            "hr", "payroll", "admission", "attendance", "facility", "timetable", "assessment", "lms", "finance",
            "library", "transport", "comms", "frontoffice", "careers");

    private static final Pattern TEXT_BLOCK = Pattern.compile("\"\"\"(.*?)\"\"\"", Pattern.DOTALL);
    private static final Pattern STRING = Pattern.compile("\"((?:[^\"\\\\\\n]|\\\\.)*)\"");
    private static final Pattern SQL = Pattern.compile("\\b(SELECT|INSERT|UPDATE|DELETE|FROM|JOIN|INTO|TABLE)\\b",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern STATEMENT = Pattern.compile("^\\s*(SELECT|INSERT|UPDATE|DELETE|WITH)\\b",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern UNQUALIFIED = Pattern.compile(
            "(?<!\\bDO\\s)\\b(FROM|JOIN|INTO|UPDATE)\\s+([a-z_][a-z0-9_]*)\\b(?!\\s*[.(])", Pattern.CASE_INSENSITIVE);
    private static final Set<String> NOT_TABLES = Set.of("lateral", "unnest", "only", "generate_series");
    private static final Pattern QUALIFIED = Pattern.compile("\\b([a-z]+)\\.([a-z_][a-z0-9_]*)\\b");
    private static final Pattern SDK_IMPORT = Pattern.compile("^import\\s+(?:static\\s+)?com\\.mysms\\.sdk\\.([a-z]+)\\.",
            Pattern.MULTILINE);
    private static final Pattern ROW = Pattern.compile("^\\| `([a-z]+)` \\| (.*) \\|$", Pattern.MULTILINE);
    private static final Pattern CELL = Pattern.compile("`([a-z]+)`");

    private ModuleChecks() {
    }

    /**
     * Violations of the own-schema rule: "File: other.table" for every SQL text naming another module's schema, in Java
     * string literals and text blocks and in .sql resources.
     */
    public static List<String> foreignSchemaReferences(String module, Path sourceRoot) {
        List<String> violations = new ArrayList<>();
        for (Path file : sqlSources(sourceRoot)) {
            for (String sql : sqlTexts(file)) {
                Matcher qualified = QUALIFIED.matcher(sql);
                while (qualified.find()) {
                    String schema = qualified.group(1);
                    if (MODULES.contains(schema) && !schema.equals(module)) {
                        violations.add(file.getFileName() + ": " + qualified.group());
                    }
                }
            }
        }
        return violations;
    }

    /**
     * Table references without a schema in SQL statements ("File: FROM item"): every table must be schema-qualified
     * (JAVA-CODING-RULES §6), so the own-schema check above can see every table a statement touches.
     */
    public static List<String> unqualifiedTableReferences(Path sourceRoot) {
        List<String> violations = new ArrayList<>();
        for (Path file : sqlSources(sourceRoot)) {
            for (String sql : sqlTexts(file)) {
                if (!STATEMENT.matcher(sql).find()) {
                    continue;
                }
                Matcher unqualified = UNQUALIFIED.matcher(sql);
                while (unqualified.find()) {
                    if (!NOT_TABLES.contains(unqualified.group(2).toLowerCase(java.util.Locale.ROOT))) {
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

    /** Violations of the SDK allowlist: "File.java imports the sdk of x" for every import outside the module's row. */
    public static List<String> sdkImportsOutsideAllowlist(String module, Path sourceRoot, Path architectureMd) {
        Set<String> allowed = allowedDependencies(module, architectureMd);
        List<String> violations = new ArrayList<>();
        for (Path file : javaFiles(sourceRoot)) {
            Matcher imports = SDK_IMPORT.matcher(read(file));
            while (imports.find()) {
                if (!allowed.contains(imports.group(1))) {
                    violations.add(file.getFileName() + " imports the sdk of " + imports.group(1));
                }
            }
        }
        return violations;
    }

    /** The module's row of the "May depend on" table in ARCHITECTURE.md. */
    public static Set<String> allowedDependencies(String module, Path architectureMd) {
        Matcher rows = ROW.matcher(read(architectureMd));
        while (rows.find()) {
            if (rows.group(1).equals(module)) {
                Set<String> allowed = new TreeSet<>();
                Matcher cells = CELL.matcher(rows.group(2));
                while (cells.find()) {
                    allowed.add(cells.group(1));
                }
                return allowed;
            }
        }
        throw new IllegalArgumentException("module " + module + " has no row in " + architectureMd);
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
