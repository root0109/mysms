package com.mysms.authclient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.mysms.core.context.Actor;
import com.mysms.core.context.CallContext;
import com.mysms.core.context.CallContextHolder;
import com.mysms.core.error.PermissionDeniedException;
import com.mysms.core.imports.ImportBatch;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/** The identity an import batch runs under grants the one checked permission in the one checked scope, no more. */
class ImportCallerIdentityTest {

    private static final UUID TENANT = UUID.fromString("01960000-0000-7000-8000-0000000000c1");
    private static final UUID SCHOOL = UUID.fromString("01960000-0000-7000-8000-0000000000c2");
    private static final UUID OTHER_SCHOOL = UUID.fromString("01960000-0000-7000-8000-0000000000c3");
    private static final String CREATE = "sis.student.create";

    private final ImportCallerIdentity identity = new ImportCallerIdentity();
    private final CallContext event = new CallContext(Optional.of(TENANT), Optional.empty(), "trace-1");

    @AfterEach
    void clear() {
        CallContextHolder.clear();
        AuthContextHolder.clear();
    }

    private static ImportBatch batch(Optional<UUID> institution, boolean tenantWide, Optional<UUID> impersonator) {
        return new ImportBatch(UUID.randomUUID(), "students", institution, 1, UUID.randomUUID(), impersonator,
                tenantWide, List.of());
    }

    @Test
    void anInstitutionBatchActsOnlyThereWithOnlyThePermission() {
        CallContextHolder.set(event);
        UUID admin = UUID.randomUUID();
        ImportBatch batch = batch(Optional.of(SCHOOL), true, Optional.of(admin));
        String seen = identity.callAs(batch, CREATE, () -> {
            AuthContext caller = AuthContext.require();
            assertThat(caller.userId()).isEqualTo(batch.userId());
            assertThat(caller.tenantId()).contains(TENANT);
            assertThat(caller.hasPermission(CREATE)).isTrue();
            assertThat(caller.hasPermission("sis.student.delete")).isFalse();
            assertThat(caller.impersonatorId()).contains(admin);
            assertThat(Actor.current()).contains(batch.userId());
            assertThat(CallContextHolder.require().impersonatorId()).contains(admin);
            DataScope scope = DataScope.current(CREATE);
            scope.requireWritable(Optional.of(SCHOOL));
            assertThatThrownBy(() -> scope.requireWritable(Optional.of(OTHER_SCHOOL)))
                    .isInstanceOf(PermissionDeniedException.class);
            assertThatThrownBy(() -> scope.requireWritable(Optional.empty()))
                    .as("a tenant-wide grant does not widen an institution import")
                    .isInstanceOf(PermissionDeniedException.class);
            return "ran";
        });
        assertThat(seen).isEqualTo("ran");
        assertThat(AuthContext.current()).isEmpty();
        assertThat(CallContextHolder.require()).isEqualTo(event);
    }

    @Test
    void tenantLevelRecordsNeedTheTenantWideGrant() {
        CallContextHolder.set(event);
        identity.callAs(batch(Optional.empty(), true, Optional.empty()), CREATE, () -> {
            DataScope.current(CREATE).requireWritable(Optional.empty());
            return null;
        });
        assertThatThrownBy(() -> identity.callAs(batch(Optional.empty(), false, Optional.empty()), CREATE,
                () -> null)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void itRunsOnlyOnATenantEventThread() {
        assertThatThrownBy(() -> identity.callAs(batch(Optional.of(SCHOOL), false, Optional.empty()), CREATE,
                () -> null)).isInstanceOf(IllegalStateException.class);
        CallContextHolder.set(new CallContext(Optional.empty(), Optional.empty(), "trace-2"));
        assertThatThrownBy(() -> identity.callAs(batch(Optional.of(SCHOOL), false, Optional.empty()), CREATE,
                () -> null)).isInstanceOf(IllegalArgumentException.class);
        CallContextHolder.set(event);
        AuthContextHolder.set(AuthContext.service("sis", Set.of(), "t"), null);
        assertThatThrownBy(() -> identity.callAs(batch(Optional.of(SCHOOL), false, Optional.empty()), CREATE,
                () -> null)).isInstanceOf(IllegalStateException.class);
    }
}
