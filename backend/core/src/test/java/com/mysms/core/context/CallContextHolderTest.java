package com.mysms.core.context;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class CallContextHolderTest {

    private static final String TRACE = "4bf92f3577b34da6a3ce929d0e0e4736";

    @AfterEach
    void clear() {
        CallContextHolder.clear();
    }

    @Test
    void aGlobalWriteSetsTheTenantAsideAndGetsItBack() {
        UUID tenant = UUID.randomUUID();
        UUID user = UUID.randomUUID();
        CallContextHolder.set(CallContext.anonymous(TRACE).withIdentity(tenant, user));

        try (CallContextHolder.Scope global = CallContextHolder.withoutTenant()) {
            CallContext inside = CallContextHolder.require();
            assertThat(inside.tenantId()).isEmpty();
            assertThat(inside.userId()).contains(user);
            assertThat(inside.traceId()).isEqualTo(TRACE);
        }
        assertThat(CallContextHolder.require().tenantId()).contains(tenant);
    }

    @Test
    void withoutATenantNothingChanges() {
        try (CallContextHolder.Scope global = CallContextHolder.withoutTenant()) {
            assertThat(CallContextHolder.current()).isEmpty();
        }
        CallContext anonymous = CallContext.anonymous(TRACE);
        CallContextHolder.set(anonymous);
        try (CallContextHolder.Scope global = CallContextHolder.withoutTenant()) {
            assertThat(CallContextHolder.require()).isSameAs(anonymous);
        }
    }
}
