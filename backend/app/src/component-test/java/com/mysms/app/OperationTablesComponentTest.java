package com.mysms.app;

import static org.assertj.core.api.Assertions.assertThat;

import com.mysms.authclient.OperationRule;
import com.mysms.authclient.OperationTable;
import java.io.InputStream;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Every one of the 2049 contract operations resolves to itself — and only to itself — from a concrete URL. The app
 * module sees every domain module's own operation table (META-INF/mysms/operations/&lt;module&gt;.json), so this is
 * where the whole edge routing surface is provable; the conformance build checks the same tables against the
 * contracts.
 */
class OperationTablesComponentTest {

    private static final List<String> MODULES = List.of("academics", "admission", "assessment", "attendance", "careers",
            "comms", "facility", "finance", "frontoffice", "hr", "iam", "library", "lms", "master", "org", "payroll", "people",
            "platform", "sis", "timetable", "transport");

    @Test
    void everyOperationMatchesItsOwnRule() throws Exception {
        OperationTable table = OperationTable.load(MODULES);
        assertThat(table.size()).isEqualTo(2049);
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
        assertThat(checked).isEqualTo(2049);
    }
}
