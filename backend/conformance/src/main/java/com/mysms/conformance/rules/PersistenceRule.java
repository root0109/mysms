package com.mysms.conformance.rules;

import com.mysms.conformance.ConformanceRule;
import com.mysms.conformance.SourceTree;
import com.mysms.conformance.Violation;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Entities and repositories:
 * <ul>
 * <li>§5 — every entity extends BaseEntity and names its TABLE in the module's own schema.</li>
 * <li>N1 — domain/ holds entities only (projections are records in repository/); an entity carries exactly the allowed
 * Lombok set and no Jackson or mapping annotations, no Map column, no LocalDateTime; BaseEntity's audit set is exact,
 * ids are UUIDv7; the lifecycle hooks are called from business/ only.</li>
 * <li>R5 — repositories are plain classes with constant SQL: no repository interface, no String.format or formatted
 * SQL, no OFFSET paging; migrations define no database functions or procedures.</li>
 * </ul>
 */
public final class PersistenceRule implements ConformanceRule {

    /** The audit set of BaseEntity, in declaration order. */
    static final List<String> AUDIT_SET =
            List.of("id", "createdAt", "createdBy", "updatedAt", "updatedBy", "deletedAt", "rowVersion");
    private static final List<String> ENTITY_LOMBOK = List.of("@Getter", "@Setter", "@SuperBuilder",
            "@NoArgsConstructor", "@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)");
    private static final Pattern BASE_FIELD = Pattern.compile("(?m)^    private (?:final )?[\\w<>, ]+ (\\w+);");
    private static final Pattern ENTITY_CLASS = Pattern.compile("class \\w+Entity extends BaseEntity");
    private static final Pattern FORBIDDEN_ON_ENTITY =
            Pattern.compile("@(?:[\\w.]+\\.)?(Data|AllArgsConstructor|ToString|Json\\w+|Table|Column|Id|Transient)\\b");
    private static final Pattern MAP_COLUMN = Pattern.compile("private [\\w<>, ]*Map<String, ?(Object|Serializable)>");
    private static final Pattern LIFECYCLE_HOOK = Pattern.compile("\\.on(Creating|Updating|Deleting)Data\\(");
    private static final Pattern REPOSITORY_INTERFACE = Pattern.compile("(?m)^public interface\\b");
    private static final Pattern FORMATTED_SQL = Pattern.compile("String\\.format\\(|\\.formatted\\(");
    private static final Pattern OFFSET = Pattern.compile("\\bOFFSET\\b");
    private static final Pattern DATABASE_CODE =
            Pattern.compile("(?i)\\bCREATE\\s+(OR\\s+REPLACE\\s+)?(FUNCTION|PROCEDURE)\\b");

    @Override
    public List<Violation> check(SourceTree tree) {
        List<Violation> found = new ArrayList<>();
        baseEntity(tree, found);
        for (String module : tree.productModules()) {
            for (Path file : tree.java(module, "main")) {
                String path = tree.relative(file);
                if (LIFECYCLE_HOOK.matcher(tree.read(file)).find() && !path.contains("/business/")
                        && !path.endsWith("/core/entity/BaseEntity.java")) {
                    found.add(new Violation("N1", path, "lifecycle hook called outside a business service (§5.8)"));
                }
            }
        }
        for (String module : tree.domainModules()) {
            Path base = tree.backend().resolve(module).resolve("src/main/java/com/mysms").resolve(module);
            for (Path file : tree.files(base.resolve("domain"), ".java")) {
                entity(tree, module, file, found);
            }
            for (Path file : tree.files(base.resolve("repository"), ".java")) {
                repository(tree, file, found);
            }
        }
        for (Path migration : tree.migrations()) {
            String sql = tree.read(migration).replaceAll("--[^\\n]*", "").replaceAll("'(?:[^']|'')*'", "''");
            if (DATABASE_CODE.matcher(sql).find()) {
                found.add(new Violation("R5", tree.relative(migration), "defines a database function or procedure (§6)"));
            }
        }
        return found;
    }

    private static void baseEntity(SourceTree tree, List<Violation> found) {
        Path file = tree.backend().resolve("core/src/main/java/com/mysms/core/entity/BaseEntity.java");
        String source = tree.read(file);
        List<String> fields = BASE_FIELD.matcher(source).results().map(m -> m.group(1)).toList();
        if (!fields.equals(AUDIT_SET)) {
            found.add(new Violation("N1", tree.relative(file),
                    "the audit set must be exactly " + AUDIT_SET + ", found " + fields));
        }
        if (!source.contains("UuidV7.next") || !source.contains("OffsetDateTime deletedAt")) {
            found.add(new Violation("N1", tree.relative(file),
                    "ids must be UUIDv7 and deletedAt a nullable OffsetDateTime"));
        }
    }

    private static void entity(SourceTree tree, String module, Path file, List<Violation> found) {
        String source = tree.read(file);
        String path = tree.relative(file);
        if (!ENTITY_CLASS.matcher(source).find()) {
            found.add(new Violation("N1", path, "not an entity: a projection is a record in repository/ (§5.4)"));
            return;
        }
        if (SourceTree.fileName(file).endsWith("Entity.java") && !source.contains("TABLE = \"" + module + ".")) {
            found.add(new Violation("§5", path, "entity names no TABLE of its schema"));
        }
        String head = source.substring(0, source.indexOf("public class"));
        List<String> annotations = Arrays.stream(head.split("\n")).filter(l -> l.startsWith("@")).map(String::strip)
                .toList();
        if (!annotations.equals(ENTITY_LOMBOK)) {
            found.add(new Violation("N1", path, "entity annotations must be exactly " + String.join(" ", ENTITY_LOMBOK)));
        }
        if (FORBIDDEN_ON_ENTITY.matcher(source).find()) {
            found.add(new Violation("N1", path, "Lombok/Jackson/mapping annotation not allowed on an entity (§5.2, §5.3)"));
        }
        if (MAP_COLUMN.matcher(source).find() || source.contains("LocalDateTime")) {
            found.add(new Violation("N1", path, "Map column or LocalDateTime field (§5.1, §5.5)"));
        }
    }

    private static void repository(SourceTree tree, Path file, List<Violation> found) {
        String source = tree.read(file);
        String path = tree.relative(file);
        if (REPOSITORY_INTERFACE.matcher(source).find()) {
            found.add(new Violation("R5", path, "repository interface (repositories are plain classes, §6)"));
        }
        if (FORMATTED_SQL.matcher(source).find()) {
            found.add(new Violation("R5", path, "SQL built with format (constants only, §6)"));
        }
        if (OFFSET.matcher(source).find()) {
            found.add(new Violation("R5", path, "OFFSET paging (keyset only, §6)"));
        }
    }
}
