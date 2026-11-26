package com.mysms.conformance;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

/**
 * Every contract operation, read from each module's edge operation table
 * ({@code src/main/resources/META-INF/mysms/operations/<module>.json}). JSON is a subset of YAML, so it is read with
 * the same parser as the rest of the checks.
 */
public final class ContractOperations {

    /** The tables are large (lms holds hundreds of rows); the default code-point limit is too small. */
    private static final int MAX_CODE_POINTS = 16 * 1024 * 1024;

    /**
     * One operation of a module contract.
     *
     * @param area        the area whose operation table lists it
     * @param operationId {@code area.Name}
     * @param method HTTP method, upper case
     * @param permission its x-permission
     * @param schemes the security schemes that may call it
     */
    public record Operation(String area, String operationId, String method, String path, String permission,
                            Set<String> schemes) {

        public Operation {
            schemes = Set.copyOf(schemes); // immutable view: the record is shared between rules
        }

        /** The operation name without its area ({@code ListCountries}): the controller method and the RPC. */
        public String name() {
            return operationId.substring(operationId.indexOf('.') + 1);
        }

        /** The controller method that implements it: the operation name with a lower-case first letter. */
        public String javaMethod() {
            return Character.toLowerCase(name().charAt(0)) + name().substring(1);
        }
    }

    private ContractOperations() {
    }

    /** Where an area's operation table lives: in its module's resources, named after the area. */
    public static Path table(SourceTree tree, String area) {
        return tree.resources(tree.area(area).module()).resolve("META-INF/mysms/operations").resolve(area + ".json");
    }

    /** The operations of every area, area by area (sorted by name), in table order. */
    public static List<Operation> read(SourceTree tree) {
        LoaderOptions options = new LoaderOptions();
        options.setCodePointLimit(MAX_CODE_POINTS);
        Yaml yaml = new Yaml(new SafeConstructor(options));
        List<Operation> operations = new ArrayList<>();
        for (SourceTree.Area area : tree.areas()) {
            String module = area.name();
            Object rows = yaml.load(tree.read(table(tree, module)));
            for (Object value : (List<?>) rows) {
                Map<?, ?> row = (Map<?, ?>) value;
                List<String> schemes = new ArrayList<>();
                for (Object scheme : (List<?>) row.get("schemes")) {
                    schemes.add(String.valueOf(scheme));
                }
                operations.add(new Operation(module, String.valueOf(row.get("operationId")),
                        String.valueOf(row.get("method")), String.valueOf(row.get("path")),
                        String.valueOf(row.get("permission")), Set.copyOf(schemes)));
            }
        }
        return operations;
    }
}
