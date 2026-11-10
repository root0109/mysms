package com.mysms.conformance;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

/**
 * Every module's edge operation table (src/main/resources/META-INF/mysms/operations/&lt;module&gt;.json — the rules
 * the auth filter enforces, and what core's OpenApiConfiguration restores into the written OpenAPI documents) states
 * exactly the operations of the module's contract: same ids, methods, paths, permissions, schemes, anonymity and
 * idempotency, one row per operation, sorted by id. The JSON is owned data; this test fails when it drifts from the
 * contract. JSON is a subset of YAML, so the tables are read with the same parser as the contracts.
 */
class EdgeOperationTablesTest {

    @Test
    void everyModulesOperationTableStatesItsContract() throws IOException {
        SourceTree tree = new SourceTree(Path.of("").toAbsolutePath().getParent().getParent());
        Map<String, List<Map<String, Object>>> expected = new TreeMap<>();
        for (ContractOperations.Operation operation : ContractOperations.read(tree)) {
            Map<String, Object> row = new TreeMap<>();
            row.put("operationId", operation.operationId());
            row.put("method", operation.method());
            row.put("path", operation.path());
            row.put("permission", operation.permission());
            row.put("schemes", operation.schemes().stream().sorted().toList());
            row.put("anonymous", operation.anonymous());
            if (operation.idempotency() != null) {
                row.put("idempotency", operation.idempotency());
            }
            expected.computeIfAbsent(operation.module(), m -> new ArrayList<>()).add(row);
        }
        assertThat(expected).hasSize(21);
        LoaderOptions options = new LoaderOptions();
        options.setCodePointLimit(16 * 1024 * 1024);
        Yaml json = new Yaml(new SafeConstructor(options));
        for (Map.Entry<String, List<Map<String, Object>>> module : expected.entrySet()) {
            module.getValue().sort(java.util.Comparator.comparing(row -> (String) row.get("operationId")));
            Path table = tree.backend().resolve(module.getKey())
                    .resolve("src/main/resources/META-INF/mysms/operations/" + module.getKey() + ".json");
            assertThat(table).as("every module owns its operation table").exists();
            List<Map<String, Object>> actual = new ArrayList<>();
            for (Object row : (List<?>) json.load(Files.readString(table, StandardCharsets.UTF_8))) {
                @SuppressWarnings("unchecked")
                Map<String, Object> fields = new TreeMap<>((Map<String, Object>) row);
                // scheme order in the table is the contract's; the comparison is by content
                fields.put("schemes", ((List<?>) fields.get("schemes")).stream().map(String::valueOf).sorted().toList());
                actual.add(fields);
            }
            assertThat(actual).as("%s (regenerate it from the contract when an operation changes)", table)
                    .isEqualTo(module.getValue());
        }
    }
}
