package com.mysms.conformance;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

/**
 * api/operations-status.md matches the code. Run with {@code -Dmysms.write-reports=true} to rewrite it from the
 * current contracts and controllers ({@code mvn -pl conformance test -Dmysms.write-reports=true}).
 */
class OperationStatusReportTest {

    private static final String STANDARD_RESOURCES = "api/standard-resources.json";

    @Test
    void theOperationStatusReportMatchesTheCode() throws IOException {
        SourceTree tree = new SourceTree(Path.of("").toAbsolutePath().getParent().getParent());
        Path report = tree.repository().resolve(OperationStatusReport.LOCATION);
        String expected = OperationStatusReport.render(tree);

        if (Boolean.getBoolean("mysms.write-reports")) {
            Files.writeString(report, expected, StandardCharsets.UTF_8);
        }

        assertThat(expected).contains("| **all** | **2049** |");
        assertThat(Files.readString(report, StandardCharsets.UTF_8))
                .as("%s is stale: run mvn -pl conformance test -Dmysms.write-reports=true", OperationStatusReport.LOCATION)
                .isEqualTo(expected);
    }

    /** The clients build their record screens from api/standard-resources.json, so it may only list served operations. */
    @Test
    @SuppressWarnings("unchecked")
    void theStandardResourcesListOnlyServedOperations() throws IOException {
        SourceTree tree = new SourceTree(Path.of("").toAbsolutePath().getParent().getParent());
        Map<String, Object> catalogue = new Yaml().load(Files.readString(
                tree.repository().resolve(STANDARD_RESOURCES), StandardCharsets.UTF_8));
        List<String> listed = new ArrayList<>();
        for (Map<String, Object> resource : (List<Map<String, Object>>) catalogue.get("resources")) {
            listed.addAll(((Map<String, String>) resource.get("operations")).values());
            for (Map<String, Object> transition : (List<Map<String, Object>>) resource.get("transitions")) {
                listed.add((String) transition.get("operation"));
            }
        }

        assertThat(listed).hasSizeGreaterThan(1500);
        assertThat(OperationStatusReport.servedOperations(tree)).as(STANDARD_RESOURCES).containsAll(listed);
    }
}
