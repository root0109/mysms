package com.mysms.authclient;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AuthContextTest {

    private static AuthContext with(String... permissions) {
        return new AuthContext(UUID.randomUUID(), Optional.of(UUID.randomUUID()), Set.of(), Optional.empty(),
                Set.of(permissions), "jti", Optional.empty(), false, Optional.empty(), Optional.empty(), false);
    }

    @Test
    void exactCodesMatchOnlyThemselves() {
        AuthContext context = with("iam.role.read");
        assertThat(context.hasPermission("iam.role.read")).isTrue();
        assertThat(context.hasPermission("iam.role.create")).isFalse();
        assertThat(context.hasPermission("iam.role")).isFalse();
    }

    @Test
    void compactedSetsCoverTheirScopeOnly() {
        assertThat(with("*").hasPermission("lms.course.create")).isTrue();
        AuthContext module = with("lms.*");
        assertThat(module.hasPermission("lms.course.create")).isTrue();
        assertThat(module.hasPermission("lmsx.course.create")).isFalse();
        assertThat(module.hasPermission("finance.invoice.read")).isFalse();
        AuthContext resource = with("lms.course.*");
        assertThat(resource.hasPermission("lms.course.delete")).isTrue();
        assertThat(resource.hasPermission("lms.course_section.delete")).isFalse();
        assertThat(resource.hasPermission("lms.cohort.read")).isFalse();
    }

    @Test
    void absentOptionalsAreEmptyAndCollectionsAreCopied() {
        AuthContext context = new AuthContext(UUID.randomUUID(), null, Set.of(), null, Set.of(), "jti", null, false,
                null, null, false);
        assertThat(context.tenantId()).isEmpty();
        assertThat(context.activeInstitutionId()).isEmpty();
        assertThat(context.impersonatorId()).isEmpty();
        assertThat(context.personId()).isEmpty();
        assertThat(context.sessionId()).isEmpty();
    }
}
