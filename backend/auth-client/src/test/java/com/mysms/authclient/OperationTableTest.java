package com.mysms.authclient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Loading and matching semantics of the operation table, on this module's own test fixtures
 * (src/test/resources/META-INF/mysms/operations: a hand-written table plus frozen copies of master and org). The live
 * tables are each domain module's own resource; the app module checks all of them against the contracts, and every
 * module's service component test exercises its own at the real edge.
 */
class OperationTableTest {

    @Test
    void matchesTheConcreteUrlToItsOwnRule() {
        OperationTable table = OperationTable.load(List.of("authtest"));
        assertThat(table.size()).isEqualTo(3);
        OperationRule create = table.match("POST", "/v1/authtest/widgets").orElseThrow();
        assertThat(create.operationId()).isEqualTo("authtest.CreateWidget");
        assertThat(create.permission()).isEqualTo("authtest.widget.create");
        assertThat(create.schemes()).containsExactly("platformAdminAuth");
        assertThat(create.idempotency()).isEqualTo("optional");
        OperationRule get = table.match("GET", "/v1/authtest/widgets/6d5c9d41-9d1a-4f8e-9f93-1ad2cf6a1f55").orElseThrow();
        assertThat(get.operationId()).isEqualTo("authtest.GetWidget");
        assertThat(get.isPublic()).isFalse();
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
