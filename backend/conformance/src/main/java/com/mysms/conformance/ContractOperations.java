package com.mysms.conformance;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

/**
 * Every operation of the module contracts under {@code api/openapi} (the module files; the shared
 * {@code common.yaml}, the joined bundle and the linter configuration hold none of their own).
 */
public final class ContractOperations {

    private static final Set<String> NOT_MODULES = Set.of("common", "redocly");
    private static final Set<String> METHODS = Set.of("get", "post", "put", "patch", "delete");
    /** The contract files are large (lms is several megabytes); the default code-point limit is too small. */
    private static final int MAX_CODE_POINTS = 64 * 1024 * 1024;

    /**
     * One operation of a module contract.
     *
     * @param module      the module (contract file) it belongs to
     * @param operationId {@code module.Name}
     * @param method      HTTP method, upper case
     * @param path        the path template
     * @param permission  its x-permission
     * @param schemes     the security schemes that may call it
     * @param anonymous   whether it may be called without a token ({@code security: []} or an empty requirement)
     * @param idempotency "required" or "optional" when it declares the Idempotency-Key header, else null
     * @param summary     its summary, or the operation id when it has none
     */
    public record Operation(String module, String operationId, String method, String path, String permission,
                            Set<String> schemes, boolean anonymous, String idempotency, String summary) {

        public Operation {
            schemes = Set.copyOf(schemes); // immutable view: the record is shared between rules
        }

        /** The controller method that implements it: the operation name with a lower-case first letter. */
        public String javaMethod() {
            String name = operationId.substring(operationId.indexOf('.') + 1);
            return Character.toLowerCase(name.charAt(0)) + name.substring(1);
        }
    }

    private ContractOperations() {
    }

    /** The operations of every module contract, module by module in file order. */
    public static List<Operation> read(SourceTree tree) {
        LoaderOptions options = new LoaderOptions();
        options.setCodePointLimit(MAX_CODE_POINTS);
        Yaml yaml = new Yaml(new SafeConstructor(options));
        List<Operation> operations = new ArrayList<>();
        for (Path file : tree.files(tree.repository().resolve("api").resolve("openapi"), ".yaml")) {
            String module = SourceTree.fileName(file).replace(".yaml", "");
            if (NOT_MODULES.contains(module)) {
                continue;
            }
            Map<String, Object> document = yaml.load(tree.read(file));
            Object defaultSecurity = document.get("security");
            map(document.get("paths")).forEach((path, item) -> map(item).forEach((method, value) -> {
                if (METHODS.contains(method)) {
                    operations.add(operation(module, method, path, map(item), map(value), defaultSecurity));
                }
            }));
        }
        return operations;
    }

    private static Operation operation(String module, String method, String path, Map<String, Object> pathItem,
                                       Map<String, Object> operation, Object defaultSecurity) {
        Set<String> schemes = new TreeSet<>();
        // an operation without its own security inherits the document's; `[]` or an empty requirement means anonymous
        Object security = operation.containsKey("security") ? operation.get("security") : defaultSecurity;
        boolean anonymous = true;
        if (security instanceof List<?> requirements) {
            anonymous = requirements.isEmpty();
            for (Object requirement : requirements) {
                Set<String> keys = map(requirement).keySet();
                schemes.addAll(keys);
                anonymous |= keys.isEmpty();
            }
        }
        String operationId = String.valueOf(operation.get("operationId"));
        Object summary = operation.get("summary");
        return new Operation(module, operationId, method.toUpperCase(Locale.ROOT), path,
                String.valueOf(operation.getOrDefault("x-permission", "")), schemes, anonymous,
                idempotency(pathItem, operation), summary == null ? operationId : summary.toString());
    }

    /**
     * "required" or "optional" when the operation (or its path item) declares the Idempotency-Key header, else null —
     * declared inline, or as a $ref to common.yaml's IdempotencyKey / IdempotencyKeyRequired parameter.
     */
    private static String idempotency(Map<String, Object> pathItem, Map<String, Object> operation) {
        String found = null;
        for (Object list : new Object[] {pathItem.get("parameters"), operation.get("parameters")}) {
            if (!(list instanceof List<?> parameters)) {
                continue;
            }
            for (Object value : parameters) {
                Map<String, Object> parameter = map(value);
                Object ref = parameter.get("$ref");
                if (ref != null) {
                    String name = String.valueOf(ref).substring(String.valueOf(ref).lastIndexOf('/') + 1);
                    found = "IdempotencyKeyRequired".equals(name) ? "required"
                            : "IdempotencyKey".equals(name) ? "optional" : found;
                } else if ("header".equals(parameter.get("in"))
                        && "Idempotency-Key".equalsIgnoreCase(String.valueOf(parameter.get("name")))) {
                    found = Boolean.TRUE.equals(parameter.get("required")) ? "required" : "optional";
                }
            }
        }
        return found;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> map(Object value) {
        return value instanceof Map<?, ?> ? (Map<String, Object>) value : Map.of();
    }
}
