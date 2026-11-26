package com.mysms.conformance.rules;

import com.mysms.conformance.ConformanceRule;
import com.mysms.conformance.SourceTree;
import com.mysms.conformance.TenantPredicates;
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
 * The SQL rules over every module's main sources:
 * <ul>
 * <li>R1 — every SELECT / UPDATE / DELETE over a tenant table filters each tenant table it reads or changes with
 * {@code tenant_id = :tenantId} at the statement's own level ({@link TenantPredicates}). The tenant and school
 * directories, read before a tenant is known, are exempt; fragments assembled in Java are listed in {@link #FRAGMENTS}
 * and covered by their class's component tests.</li>
 * <li>R2 — SQL lives in repositories, as constants (no SQL text in controllers, services or mappers).</li>
 * <li>§6 — no hard DELETE (soft delete only), except pruning module-local infrastructure tables.</li>
 * </ul>
 */
public final class SqlRule implements ConformanceRule {

    /**
     * Read before a tenant is known (sign-in, the public school finder); they hold codes, status, schools' public
     * names and opt-in flags only and have no row-level security.
     */
    private static final Set<String> EXEMPT_TABLES = Set.of("iam.tenant_directory", "org.tenant_directory",
            "org.institution_directory", "org.finder_listing");
    /**
     * Files whose SQL texts are fragments joined in Java, so a single text holds no complete statement:
     * GrantRepository's permission sub-selects are assembled from COUNTED_GRANT / NOT_DENIED.
     */
    private static final Set<String> FRAGMENTS = Set.of("GrantRepository.java");

    private static final Pattern TEXT_BLOCK = Pattern.compile("\"\"\"(.*?)\"\"\"", Pattern.DOTALL);
    private static final Pattern STRING_SQL = Pattern.compile("\"((?:SELECT|UPDATE|DELETE) [^\"]*)\"");
    private static final Pattern STATEMENT = Pattern.compile("\\b(SELECT|UPDATE|DELETE)\\b");
    private static final Pattern TABLE_ACCESS = Pattern.compile("\\b(FROM|UPDATE)\\s+[a-z]+\\.[a-z_]+");
    private static final Pattern DELETE_FROM = Pattern.compile("(?i)\\bDELETE\\s+FROM\\s+(\\S+)");
    private static final Pattern INFRASTRUCTURE = Pattern.compile("\\.(idempotency_key|outbox_event|inbox_event)$");

    @Override
    public List<Violation> check(SourceTree tree) {
        Set<String> tenantTables = new HashSet<>(tree.tenantTables());
        tenantTables.removeAll(EXEMPT_TABLES);
        TenantPredicates predicates = new TenantPredicates(tenantTables);
        List<Violation> found = new ArrayList<>();
        for (String module : tree.productModules()) {
            for (Path file : tree.java(module, "main")) {
                String source = tree.read(file);
                String layer = layer(tree, module, file);
                Matcher block = TEXT_BLOCK.matcher(source);
                while (block.find()) {
                    String sql = block.group(1);
                    String flat = String.join(" ", sql.trim().split("\\s+"));
                    if (!STATEMENT.matcher(flat).find()) {
                        continue;
                    }
                    if (layer != null && !"repository".equals(layer) && TABLE_ACCESS.matcher(flat).find()) {
                        found.add(new Violation("R2", tree.relative(file), "SQL outside the repository layer"));
                    }
                    tenantPredicate(tree, file, sql, tenantTables, predicates, found);
                    hardDelete(tree, file, flat, found);
                }
                Matcher literal = STRING_SQL.matcher(source);
                while (literal.find()) {
                    tenantPredicate(tree, file, literal.group(1), tenantTables, predicates, found);
                }
            }
        }
        return found;
    }

    /**
     * The layer package of a domain-module class (controller, business, repository…), inside its area's package when
     * the area is not named like its module; null for shared modules.
     */
    private static String layer(SourceTree tree, String module, Path file) {
        if (SourceTree.SHARED.contains(module)) {
            return null;
        }
        String path = tree.relative(file);
        String marker = "/com/mysms/" + module + "/";
        int at = path.indexOf(marker);
        String rest = path.substring(at + marker.length());
        int slash = rest.indexOf('/');
        if (slash >= 0 && tree.areas(module).stream()
                .anyMatch(a -> !a.name().equals(module) && a.name().equals(rest.substring(0, slash)))) {
            String inArea = rest.substring(slash + 1);
            int next = inArea.indexOf('/');
            return next < 0 ? "" : inArea.substring(0, next);
        }
        return slash < 0 ? "" : rest.substring(0, slash);
    }

    private static void tenantPredicate(SourceTree tree, Path file, String sql, Set<String> tenantTables,
                                        TenantPredicates predicates, List<Violation> found) {
        String lower = sql.toLowerCase(Locale.ROOT);
        if (!STATEMENT.matcher(sql).find() || tenantTables.stream().noneMatch(lower::contains)) {
            return;
        }
        String flat = String.join(" ", sql.trim().split("\\s+"));
        try {
            List<String> missing = predicates.missing(sql);
            if (!missing.isEmpty()) {
                found.add(new Violation("R1", tree.relative(file),
                        "missing tenant predicate for " + String.join(", ", missing) + ": " + clip(flat, 90)));
            }
        } catch (TenantPredicates.Unsupported e) {
            if (!FRAGMENTS.contains(SourceTree.fileName(file))) {
                found.add(new Violation("R1", tree.relative(file), e.getMessage() + ": " + clip(flat, 70)));
            }
        }
    }

    private static void hardDelete(SourceTree tree, Path file, String flat, List<Violation> found) {
        Matcher delete = DELETE_FROM.matcher(flat);
        while (delete.find()) {
            String target = delete.group(1);
            // module-local infrastructure tables are pruned by design;
            // IdempotencyStore names its table through {table} = <module>.idempotency_key
            boolean infrastructure = INFRASTRUCTURE.matcher(target).find()
                    || "{table}".equals(target) && "IdempotencyStore.java".equals(SourceTree.fileName(file));
            if (!infrastructure) {
                found.add(new Violation("§6", tree.relative(file), "hard DELETE: " + clip(flat, 70)));
            }
        }
    }

    private static String clip(String text, int length) {
        return text.length() <= length ? text : text.substring(0, length);
    }
}
