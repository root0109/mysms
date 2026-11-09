package com.mysms.authclient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.mysms.core.error.PermissionDeniedException;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class DataScopeTest {

    private static final UUID A = UUID.randomUUID();
    private static final UUID B = UUID.randomUUID();
    private static final String UPDATE = "org.holiday.update";

    private static AuthContext context(Set<UUID> institutions, Optional<UUID> active, boolean all, Set<String> tenantWide) {
        return new AuthContext(UUID.randomUUID(), Optional.of(UUID.randomUUID()), institutions, active,
                Set.of("org.holiday.read", UPDATE), "jti", Optional.empty(), false, Optional.empty(), Optional.empty(),
                all, tenantWide);
    }

    @Test
    void tenantWideCallersSeeAndChangeEverything() {
        DataScope scope = DataScope.of(context(Set.of(), Optional.empty(), true, Set.of("org.*")), UPDATE);
        assertThat(scope.all()).isTrue();
        assertThat(scope.canRead(Optional.of(A))).isTrue();
        assertThatNoException().isThrownBy(() -> scope.requireWritable(Optional.empty()));
        assertThatNoException().isThrownBy(() -> scope.requireWritable(Optional.of(B)));
    }

    @Test
    void schoolCallersStayInsideTheirInstitutionsAndCannotChangeTenantLevelRows() {
        DataScope scope = DataScope.of(context(Set.of(A), Optional.empty(), false, Set.of()), UPDATE);
        assertThat(scope.all()).isFalse();
        assertThat(scope.institutionArray()).containsExactly(A);
        assertThat(scope.canRead(Optional.of(A))).isTrue();
        assertThat(scope.canRead(Optional.of(B))).isFalse();
        assertThat(scope.canRead(Optional.empty())).as("tenant-level rows are readable").isTrue();
        assertThatNoException().isThrownBy(() -> scope.requireWritable(Optional.of(A)));
        assertThatThrownBy(() -> scope.requireWritable(Optional.of(B))).isInstanceOf(PermissionDeniedException.class);
        assertThatThrownBy(() -> scope.requireWritable(Optional.empty())).isInstanceOf(PermissionDeniedException.class)
                .hasMessageContaining("tenant-wide");
    }

    @Test
    void aTenantWideGrantOfAnotherPermissionNeverWidensThisOne() {
        // tenant-wide read-only role + a school-A role that may update: the update stays in school A
        AuthContext mixed = context(Set.of(A), Optional.empty(), true, Set.of("org.holiday.read"));
        DataScope update = DataScope.of(mixed, UPDATE);
        assertThat(update.all()).isFalse();
        assertThat(update.tenantWide()).isFalse();
        assertThatNoException().isThrownBy(() -> update.requireWritable(Optional.of(A)));
        assertThatThrownBy(() -> update.requireWritable(Optional.of(B))).isInstanceOf(PermissionDeniedException.class);
        assertThatThrownBy(() -> update.requireWritable(Optional.empty())).isInstanceOf(PermissionDeniedException.class);
        assertThat(DataScope.of(mixed, "org.holiday.read").all()).isTrue();
    }

    @Test
    void aSelectedInstitutionNarrowsEvenATenantWideCaller() {
        DataScope scope = DataScope.of(context(Set.of(), Optional.of(B), true, Set.of("*")), UPDATE);
        assertThat(scope.all()).isFalse();
        assertThat(scope.canRead(Optional.of(A))).isFalse();
        assertThat(scope.canRead(Optional.of(B))).isTrue();
        assertThatNoException().isThrownBy(() -> scope.requireWritable(Optional.empty()));
        assertThatThrownBy(() -> scope.requireWritable(Optional.of(A))).isInstanceOf(PermissionDeniedException.class);
        DataScope narrow = DataScope.of(context(Set.of(A), Optional.of(B), true, Set.of()), UPDATE);
        assertThat(narrow.canRead(Optional.of(B))).as("selected but not granted for this permission").isFalse();
    }

    @Test
    void theShortConstructorTreatsATenantWideCallersPermissionsAsTenantWide() {
        AuthContext legacy = new AuthContext(UUID.randomUUID(), Optional.empty(), Set.of(), Optional.empty(),
                Set.of(UPDATE), "jti", Optional.empty(), false, Optional.empty(), Optional.empty(), true);
        assertThat(legacy.hasTenantWidePermission(UPDATE)).isTrue();
        assertThat(legacy.withActiveInstitution(A).tenantWidePermissions()).containsExactly(UPDATE);
    }
}
