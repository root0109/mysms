package com.mysms.conformance.rules;

import com.mysms.conformance.ConformanceRule;
import com.mysms.conformance.ContractOperations;
import com.mysms.conformance.SourceTree;
import com.mysms.conformance.Violation;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

/**
 * The gRPC rules, checked over the whole tree:
 * <ul>
 * <li>G1 — each module owns its protos ({@code src/main/resources/proto/mysms/<module>/v1}); a caller keeps a
 * byte-identical copy of the callee's proto, only of a callee it may depend on (or one a proto it carries imports); the
 * shared protos are core's alone.</li>
 * <li>G2 — field numbers are law: every field of every owned message has the number
 * {@code conformance/src/main/resources/field-numbers.lock.json} records, and the lock records every message.</li>
 * <li>G3 — every operation of a module's operation table is an RPC of its own proto, and every RPC an operation.</li>
 * <li>G4 — no class outside {@code <module>/integration} uses another module's gRPC stub.</li>
 * <li>G5 — every gRPC server implementation is a {@code @GrpcService}.</li>
 * <li>G6 — no REST client between modules (no RestClient, RestTemplate, WebClient, HTTP interface clients).</li>
 * <li>G7 — no {@code Transactions} helper: transactions are {@code AppTransactionManager}'s alone.</li>
 * <li>G8 — every module has its {@code <Module>OpenApiComponentTest} writing {@code target/openapi/api-docs.yml}.</li>
 * </ul>
 * G9 is {@link ConfigurationRule}'s.
 */
public final class GrpcGuidelineRule implements ConformanceRule {

    /** Where every OpenAPI component test writes its document. */
    public static final String OPEN_API_OUTPUT_LOCATION = "target/openapi/api-docs.yml";

    private static final String PROTO_ROOT = "src/main/resources/proto";
    private static final Pattern COMMENT = Pattern.compile("//[^\\n]*");
    private static final Pattern IMPORT = Pattern.compile("(?m)^import \"mysms/([a-z]+)/v1/[a-z_]+\\.proto\";");
    private static final Pattern PACKAGE = Pattern.compile("(?m)^package mysms\\.([a-z]+)\\.v1;");
    private static final Pattern RPC = Pattern.compile("\\brpc (\\w+)\\(");
    private static final Pattern MESSAGE = Pattern.compile("^message (\\w+) \\{");
    private static final Pattern FIELD =
            Pattern.compile("^\\s*(?:optional |repeated )?[\\w.<>, ]+?\\s+(\\w+)\\s*=\\s*(\\d+)\\s*[;\\[]");
    private static final Pattern ROW = Pattern.compile("(?m)^\\| `([a-z]+)` \\| (.*) \\|$");
    private static final Pattern CELL = Pattern.compile("`([a-z]+)`");
    private static final Pattern STUB_IMPORT = Pattern.compile("(?m)^import com\\.mysms\\.([a-z]+)\\.v1\\.\\w+Grpc;");
    private static final Pattern SERVER_IMPL = Pattern.compile("extends \\w+Grpc\\.\\w+ImplBase\\b");
    private static final Pattern REST_CLIENT = Pattern.compile("(?m)^import (?:static )?org\\.springframework\\.web\\."
            + "(?:client\\.RestClient|client\\.RestTemplate|reactive\\.function\\.client|service\\.)");
    private static final Pattern TRANSACTIONS =
            Pattern.compile("\\bcom\\.mysms\\.core\\.tx\\b|\\bTransactions\\.(commit|rollback)\\(");
    private static final int MAX_CODE_POINTS = 16 * 1024 * 1024;

    @Override
    public List<Violation> check(SourceTree tree) {
        List<Violation> found = new ArrayList<>();
        Map<String, Set<String>> allowed = allowedDependencies(tree);
        for (String module : tree.domainModules()) {
            protos(tree, module, allowed.getOrDefault(module, Set.of()), found);
            operations(tree, module, found);
            code(tree, module, found);
            openApiTest(tree, module, found);
        }
        fieldNumbers(tree, found);
        transactions(tree, found);
        return found;
    }

    // ---------------------------------------------------------------- G1 owned protos and verbatim copies

    private static void protos(SourceTree tree, String module, Set<String> callees, List<Violation> found) {
        Path root = tree.backend().resolve(module).resolve(PROTO_ROOT);
        Path own = ownProto(tree, module);
        if (!Files.isRegularFile(own)) {
            found.add(new Violation("G1", tree.relative(tree.backend().resolve(module)), "has no proto of its own"));
            return;
        }
        Set<String> carried = new TreeSet<>();
        Set<String> needed = new HashSet<>(callees);
        for (Path file : tree.files(root, ".proto")) {
            String owner = owner(tree.read(file));
            if (owner == null) {
                found.add(new Violation("G1", tree.relative(file), "has no mysms.<module>.v1 package"));
                continue;
            }
            needed.addAll(imports(tree.read(file)));
            if (owner.equals(module)) {
                continue;
            }
            carried.add(owner);
            Path original = ownProto(tree, owner).resolveSibling(SourceTree.fileName(file));
            if ("common".equals(owner)) {
                found.add(new Violation("G1", tree.relative(file), "copies core's shared protos"));
            } else if (!Files.isRegularFile(original)) {
                found.add(new Violation("G1", tree.relative(file), "copies a proto " + owner + " does not own"));
            } else if (!Arrays.equals(bytes(original), bytes(file))) {
                found.add(new Violation("G1", tree.relative(file), "differs from " + tree.relative(original)));
            }
        }
        for (String owner : carried) {
            if (!needed.contains(owner)) {
                found.add(new Violation("G1", tree.relative(root), "carries the proto of " + owner
                        + ", which it may not call (ARCHITECTURE §2) and no proto it carries imports"));
            }
        }
    }

    // ---------------------------------------------------------------- G2 field numbers

    private static void fieldNumbers(SourceTree tree, List<Violation> found) {
        Path lockFile = tree.backend().resolve("conformance/src/main/resources/field-numbers.lock.json");
        LoaderOptions options = new LoaderOptions();
        options.setCodePointLimit(MAX_CODE_POINTS);
        Map<?, ?> lock = new Yaml(new SafeConstructor(options)).load(tree.read(lockFile));
        Set<String> declared = new TreeSet<>();
        List<Path> owned = new ArrayList<>(tree.files(tree.backend().resolve("core").resolve(PROTO_ROOT), ".proto"));
        for (String module : tree.domainModules()) {
            owned.add(ownProto(tree, module));
        }
        for (Path file : owned) {
            if (!Files.isRegularFile(file)) {
                continue;
            }
            String source = tree.read(file);
            String owner = owner(source);
            messages(source).forEach((message, fields) -> {
                String key = owner + "." + message;
                declared.add(key);
                Object locked = lock.get(key);
                if (!(locked instanceof Map<?, ?> numbers)) {
                    found.add(new Violation("G2", tree.relative(file), key + " is not in the field-number lock"));
                    return;
                }
                Map<String, Integer> expected = new TreeMap<>();
                numbers.forEach((name, number) -> expected.put(String.valueOf(name), ((Number) number).intValue()));
                if (!expected.equals(fields)) {
                    found.add(new Violation("G2", tree.relative(file), key + " has fields " + fields
                            + " but the lock records " + expected));
                }
            });
        }
        for (Object key : lock.keySet()) {
            if (!declared.contains(String.valueOf(key))) {
                found.add(new Violation("G2", tree.relative(lockFile), key + " is locked but declared by no proto"));
            }
        }
    }

    /** The top-level messages of a proto and their fields (name → number); nested enums are not fields. */
    public static Map<String, Map<String, Integer>> messages(String proto) {
        Map<String, Map<String, Integer>> messages = new LinkedHashMap<>();
        int depth = 0;
        Map<String, Integer> current = null;
        for (String line : COMMENT.matcher(proto).replaceAll("").split("\n")) {
            Matcher message = MESSAGE.matcher(line);
            if (depth == 0 && message.find()) {
                current = new TreeMap<>();
                messages.put(message.group(1), current);
            } else if (depth == 1 && current != null) {
                Matcher field = FIELD.matcher(line);
                if (field.find() && !line.trim().startsWith("reserved") && !line.trim().startsWith("option ")) {
                    current.put(field.group(1), Integer.parseInt(field.group(2)));
                }
            }
            depth += count(line, '{') - count(line, '}');
            if (depth == 0) {
                current = null;
            }
        }
        return messages;
    }

    // ---------------------------------------------------------------- G3 operations and RPCs

    private static void operations(SourceTree tree, String module, List<Violation> found) {
        Path own = ownProto(tree, module);
        if (!Files.isRegularFile(own)) {
            return;
        }
        Set<String> rpcs = new TreeSet<>();
        Matcher rpc = RPC.matcher(COMMENT.matcher(tree.read(own)).replaceAll(""));
        while (rpc.find()) {
            rpcs.add(rpc.group(1));
        }
        Set<String> operations = ContractOperations.read(tree).stream().filter(op -> op.module().equals(module))
                .map(ContractOperations.Operation::name).collect(Collectors.toCollection(TreeSet::new));
        for (String operation : operations) {
            if (!rpcs.contains(operation)) {
                found.add(new Violation("G3", tree.relative(own), module + "." + operation + " has no RPC"));
            }
        }
        for (String method : rpcs) {
            if (!operations.contains(method)) {
                found.add(new Violation("G3", tree.relative(ContractOperations.table(tree, module)),
                        "RPC " + module + "." + method + " is no operation of the table"));
            }
        }
    }

    // ---------------------------------------------------------------- G4, G5, G6 code

    private static void code(SourceTree tree, String module, List<Violation> found) {
        String integration = "/com/mysms/" + module + "/integration/";
        for (Path file : tree.java(module, "main")) {
            String path = tree.relative(file);
            String source = tree.read(file);
            Matcher stub = STUB_IMPORT.matcher(source);
            while (stub.find()) {
                if (!stub.group(1).equals(module) && !path.contains(integration)) {
                    found.add(new Violation("G4", path, "uses the gRPC stub of " + stub.group(1)
                            + " outside " + module + "/integration"));
                }
            }
            if (SERVER_IMPL.matcher(source).find() && !source.contains("\n@GrpcService\n")) {
                found.add(new Violation("G5", path, "a gRPC server implementation that is not a @GrpcService"));
            }
            if (REST_CLIENT.matcher(source).find()) {
                found.add(new Violation("G6", path, "a REST client: modules call each other over gRPC only"));
            }
        }
    }

    // ---------------------------------------------------------------- G7 no Transactions

    private static void transactions(SourceTree tree, List<Violation> found) {
        for (String module : tree.productModules()) {
            for (String root : List.of("main", "test", "component-test")) {
                for (Path file : tree.java(module, root)) {
                    if (TRANSACTIONS.matcher(tree.read(file)).find()
                            || SourceTree.fileName(file).equals("Transactions.java")) {
                        found.add(new Violation("G7", tree.relative(file),
                                "a Transactions helper: transactions are AppTransactionManager's"));
                    }
                }
            }
        }
    }

    // ---------------------------------------------------------------- G8 OpenAPI component test

    private static void openApiTest(SourceTree tree, String module, List<Violation> found) {
        String name = Character.toUpperCase(module.charAt(0)) + module.substring(1) + "OpenApiComponentTest.java";
        Path test = tree.java(module, "component-test").stream()
                .filter(file -> SourceTree.fileName(file).equals(name)).findFirst().orElse(null);
        if (test == null) {
            found.add(new Violation("G8", tree.relative(tree.backend().resolve(module)), "no " + name));
            return;
        }
        String source = tree.read(test);
        if (!source.contains("\"" + OPEN_API_OUTPUT_LOCATION + "\"") || !source.contains("/v3/api-docs.yaml")) {
            found.add(new Violation("G8", tree.relative(test), "does not write /v3/api-docs.yaml to "
                    + OPEN_API_OUTPUT_LOCATION));
        }
    }

    // ---------------------------------------------------------------- helpers

    private static Path ownProto(SourceTree tree, String module) {
        Path base = "common".equals(module) ? tree.backend().resolve("core") : tree.backend().resolve(module);
        return base.resolve(PROTO_ROOT).resolve("mysms").resolve(module).resolve("v1").resolve(module + ".proto");
    }

    private static String owner(String proto) {
        Matcher owner = PACKAGE.matcher(proto);
        return owner.find() ? owner.group(1) : null;
    }

    private static Set<String> imports(String proto) {
        Set<String> imported = new HashSet<>();
        IMPORT.matcher(proto).results().forEach(match -> imported.add(match.group(1)));
        return imported;
    }

    /** Each module's row of ARCHITECTURE.md §2 ("May depend on"). */
    private static Map<String, Set<String>> allowedDependencies(SourceTree tree) {
        Map<String, Set<String>> rows = new TreeMap<>();
        Matcher row = ROW.matcher(tree.read(tree.repository().resolve("ARCHITECTURE.md")));
        while (row.find()) {
            Set<String> cells = new TreeSet<>();
            CELL.matcher(row.group(2)).results().forEach(cell -> cells.add(cell.group(1)));
            rows.put(row.group(1), cells);
        }
        return rows;
    }

    private static byte[] bytes(Path file) {
        try {
            return Files.readAllBytes(file);
        } catch (java.io.IOException e) {
            throw new java.io.UncheckedIOException(e);
        }
    }

    private static int count(String line, char c) {
        return (int) line.chars().filter(ch -> ch == c).count();
    }
}
