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
 * Every contract operation resolves to itself, and only to itself, from a concrete URL. The app module sees every
 * module's operation table, so the whole edge routing surface is provable here.
 */
class OperationTablesComponentTest {

    private static final List<String> MODULES = List.of("academics", "admission", "assessment", "attendance", "careers",
            "comms", "facility", "finance", "frontoffice", "hr", "iam", "library", "lms", "master", "org", "payroll", "people",
            "platform", "privacy", "sis", "timetable", "transport");

    @Test
    void everyOperationMatchesItsOwnRule() throws Exception {
        OperationTable table = OperationTable.load(MODULES);
        assertThat(table.size()).isEqualTo(2077);
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
        assertThat(checked).isEqualTo(2077);
    }

    /**
     * Every RPC of every module's gRPC contract finds its own operation ({@code <module>.<RpcMethod>}), so the gRPC
     * edge enforces exactly what the REST edge does.
     */
    @Test
    void everyGrpcMethodFindsItsOperation() throws Exception {
        OperationTable table = OperationTable.load(MODULES);
        int checked = 0;
        for (String module : MODULES) {
            String outer = "com.mysms." + module + ".v1." + Character.toUpperCase(module.charAt(0)) + module.substring(1)
                    + "Proto";
            com.google.protobuf.Descriptors.FileDescriptor file = (com.google.protobuf.Descriptors.FileDescriptor)
                    Class.forName(outer).getMethod("getDescriptor").invoke(null);
            for (com.google.protobuf.Descriptors.ServiceDescriptor service : file.getServices()) {
                for (com.google.protobuf.Descriptors.MethodDescriptor method : service.getMethods()) {
                    String full = service.getFullName() + "/" + method.getName();
                    assertThat(table.forGrpcMethod(full)).as(full).map(OperationRule::operationId)
                            .contains(module + "." + method.getName());
                    checked++;
                }
            }
        }
        assertThat(checked).isEqualTo(2077);
    }
}
