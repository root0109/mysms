package com.mysms.conformance.rules;

import com.mysms.conformance.ConformanceRule;
import com.mysms.conformance.ContractOperations;
import com.mysms.conformance.SourceTree;
import com.mysms.conformance.Violation;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;

/**
 * P1: every permission a tenant user can be granted (the x-permission of each operation callable with a tenant user's
 * token) is seeded in iam's catalogue by iam's migrations, with an id derived from the code
 * ({@code md5('mysms.permission:' || code)::uuid}) so it is the same in every environment. Before the first release a
 * new permission goes in iam's baseline; after it, in a new iam migration.
 */
public final class PermissionCatalogueRule implements ConformanceRule {

    private static final Pattern SEEDED = Pattern.compile("md5\\('mysms\\.permission:' \\|\\| '([^']+)'\\)");
    private static final String TENANT_USER = "bearerAuth";

    @Override
    public List<Violation> check(SourceTree tree) {
        Set<String> seeded = new HashSet<>();
        for (Path migration : tree.migrations("iam")) {
            SEEDED.matcher(tree.read(migration)).results().forEach(m -> seeded.add(m.group(1)));
        }
        Set<String> missing = new TreeSet<>();
        for (ContractOperations.Operation operation : ContractOperations.read(tree)) {
            if (operation.schemes().contains(TENANT_USER) && !seeded.contains(operation.permission())) {
                missing.add(operation.permission());
            }
        }
        List<Violation> found = new ArrayList<>();
        for (String permission : missing) {
            found.add(new Violation("P1", "database/migrations/iam",
                    "permission " + permission + " is not seeded in the iam permission catalogue"));
        }
        return found;
    }
}
