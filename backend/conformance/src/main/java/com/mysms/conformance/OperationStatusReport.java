package com.mysms.conformance;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * {@code api/operations-status.md}: for every contract operation, whether this build serves it, read from the code.
 * An operation is served when a controller of its module implements its method with a body that does not throw
 * NotImplementedException; every other operation answers 501 NOT_IMPLEMENTED at the edge.
 */
public final class OperationStatusReport {

    /** Where the report lives, relative to the repository root. */
    public static final String LOCATION = "api/operations-status.md";

    private static final Pattern CONTROLLER_METHOD = Pattern.compile(
            "    @Override\\n    public ResponseEntity<[^\\n]*?> (\\w+)\\((.*?)\\n    }\\n", Pattern.DOTALL);
    private static final Pattern STANDARD = Pattern.compile("^(List|Get|Create|Update|Delete)[A-Z]");

    private OperationStatusReport() {
    }

    /** The ids of the contract operations this build serves (for example {@code platform.ListTenants}). */
    public static Set<String> servedOperations(SourceTree tree) {
        Map<String, Map<String, Boolean>> served = servedMethods(tree);
        return ContractOperations.read(tree).stream().filter(operation -> isServed(served, operation))
                .map(ContractOperations.Operation::operationId).collect(Collectors.toCollection(TreeSet::new));
    }

    private static boolean isServed(Map<String, Map<String, Boolean>> served, ContractOperations.Operation operation) {
        return served.getOrDefault(operation.module(), Map.of()).getOrDefault(operation.javaMethod(), false);
    }

    /** The report as it follows from the contracts and the controllers in the tree. */
    public static String render(SourceTree tree) {
        Map<String, Map<String, Boolean>> served = servedMethods(tree);
        Map<String, List<String>> rows = new TreeMap<>();
        Map<String, int[]> totals = new TreeMap<>();
        for (ContractOperations.Operation operation : ContractOperations.read(tree)) {
            boolean isServed = isServed(served, operation);
            String name = operation.operationId().substring(operation.operationId().indexOf('.') + 1);
            String why = isServed ? "" : STANDARD.matcher(name).find()
                    ? "standard operation not built yet (public, platform-staff or extra-parameter variant)"
                    : "workflow / custom operation";
            rows.computeIfAbsent(operation.module(), m -> new ArrayList<>()).add(String.format(
                    "| `%s` | %s | `%s` | `%s` | %s | %s |", operation.operationId(), operation.method(),
                    operation.path(), operation.permission(), isServed ? "served" : "501", why));
            int[] counts = totals.computeIfAbsent(operation.module(), m -> new int[2]);
            counts[0]++;
            counts[1] += isServed ? 1 : 0;
        }
        StringBuilder out = new StringBuilder("""
                # Operation status

                Written by the build (`OperationStatusReport` in `backend/conformance`) from the contracts and the
                controllers — do not edit by hand. *Served* operations are implemented and covered by component tests
                against PostgreSQL; every other operation answers **501 `NOT_IMPLEMENTED`** at the edge, never a
                misleading 404.

                | Module | Operations | Served | 501 |
                |---|---:|---:|---:|
                """);
        int all = 0;
        int allServed = 0;
        for (Map.Entry<String, int[]> module : totals.entrySet()) {
            int[] counts = module.getValue();
            out.append(String.format("| %s | %d | %d | %d |%n", module.getKey(), counts[0], counts[1],
                    counts[0] - counts[1]));
            all += counts[0];
            allServed += counts[1];
        }
        out.append(String.format("| **all** | **%d** | **%d** | **%d** |%n", all, allServed, all - allServed));
        rows.forEach((module, lines) -> {
            out.append(String.format("%n## %s%n%n| Operation | Method | Path | Permission | Status | Why not served |%n"
                    + "|---|---|---|---|---|---|%n", module));
            lines.forEach(line -> out.append(line).append('\n'));
        });
        return out.toString().replace("\r\n", "\n");
    }

    /** {module: {controller method: served}} from every controller of the domain modules. */
    private static Map<String, Map<String, Boolean>> servedMethods(SourceTree tree) {
        Map<String, Map<String, Boolean>> served = new HashMap<>();
        for (String module : tree.domainModules()) {
            Path controllers = tree.backend().resolve(module).resolve("src/main/java/com/mysms").resolve(module)
                    .resolve("controller");
            for (Path file : tree.files(controllers, "Controller.java")) {
                String source = tree.read(file).replace("\r\n", "\n").replace('\r', '\n');
                Matcher method = CONTROLLER_METHOD.matcher(source);
                while (method.find()) {
                    served.computeIfAbsent(module, m -> new HashMap<>())
                            .put(method.group(1), !method.group(2).contains("NotImplementedException"));
                }
            }
        }
        return served;
    }
}
