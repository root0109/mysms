package com.mysms.authclient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.InputStream;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** Every one of the 2052 contract operations resolves to itself — and only to itself — from a concrete URL. */
class OperationTableTest {

    private static final List<String> MODULES = List.of("academics", "admission", "assessment", "attendance", "careers",
            "comms", "facility", "finance", "frontoffice", "hr", "iam", "library", "lms", "master", "org", "payroll", "people",
            "platform", "sis", "timetable", "transport");

    @Test
    void everyOperationMatchesItsOwnRule() throws Exception {
        OperationTable table = OperationTable.load(MODULES);
        assertThat(table.size()).isEqualTo(2052);
        JsonMapper json = JsonMapper.builder().build();
        int checked = 0;
        for (String module : MODULES) {
            String resource = "META-INF/mysms/operations/" + module + ".json";
            try (InputStream in = getClass().getClassLoader().getResourceAsStream(resource)) {
                for (JsonNode row : json.readTree(in)) {
                    String concrete = row.get("path").asString().replaceAll("\\{[^}]+}", UUID.randomUUID().toString());
                    OperationRule rule = table.match(row.get("method").asString(), concrete).orElseThrow(
                            () -> new AssertionError("no match for " + row.get("operationId").asString()));
                    assertThat(rule.operationId()).as(concrete).isEqualTo(row.get("operationId").asString());
                    assertThat(rule.permission()).isEqualTo(row.get("permission").asString());
                    checked++;
                }
            }
        }
        assertThat(checked).isEqualTo(2052);
    }

    @Test
    void unknownRoutesAndMethodsDoNotMatch() {
        OperationTable table = OperationTable.load(List.of("master"));
        assertThat(table.match("GET", "/v1/master/countries")).map(OperationRule::operationId).contains("master.ListCountries");
        assertThat(table.match("PUT", "/v1/master/countries")).isEmpty();
        assertThat(table.match("GET", "/v1/master/secrets")).isEmpty();
        assertThat(table.match("GET", "/v1/finance/invoices")).isEmpty();
        assertThat(table.match("GET", "/v1/master/countries").orElseThrow().isPublic()).isTrue();
        assertThat(table.match("POST", "/v1/master/countries").orElseThrow().schemes()).containsExactly("platformAdminAuth");
    }

    @Test
    void refusesMissingTables() {
        assertThatThrownBy(() -> OperationTable.load(List.of())).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> OperationTable.load(List.of("nope"))).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> OperationTable.load(List.of("master", "master"))).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("duplicate");
    }
}
