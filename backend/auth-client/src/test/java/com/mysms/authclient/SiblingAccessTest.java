package com.mysms.authclient;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * A sibling area's operation called in process: permitted to a tenant's person holding its permission, as at the
 * edge.
 */
class SiblingAccessTest {

    private final SiblingAccess access = new SiblingAccess(new ClaimsPermissionChecker());

    @AfterEach
    void unbind() {
        AuthContextHolder.clear();
    }

    @Test
    void aPersonHoldingThePermissionMayAndOthersMayNot() {
        AuthContextHolder.set(new AuthContext(UUID.randomUUID(), Optional.of(UUID.randomUUID()), Set.of(),
                Optional.empty(), Set.of("timetable.session.*"), "jti", Optional.empty(), false, Optional.empty(),
                Optional.empty(), false), "token");
        assertThat(access.permits("timetable.session.read")).isTrue();
        assertThat(access.permits("timetable.session_student.read")).isFalse();
    }

    @Test
    void aServicePrincipalOrNoCallerMayNot() {
        assertThat(access.permits("timetable.session.read")).as("no caller").isFalse();
        AuthContextHolder.set(AuthContext.service("attendance", Set.of("*"), "jti"), "token");
        assertThat(access.permits("timetable.session.read")).as("the operation is not internal").isFalse();
    }

    @Test
    void platformStaffMayNotEvenHoldingThePermission() {
        AuthContextHolder.set(new AuthContext(UUID.randomUUID(), Optional.empty(), Set.of(), Optional.empty(),
                Set.of("timetable.session.*"), "jti", Optional.empty(), true, Optional.empty(), Optional.empty(),
                false), "token");
        assertThat(access.permits("timetable.session.read")).as("the operation is bearerAuth only").isFalse();
    }
}
