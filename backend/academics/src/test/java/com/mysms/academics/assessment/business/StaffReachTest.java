package com.mysms.academics.assessment.business;

import static org.assertj.core.api.Assertions.assertThat;

import com.mysms.authclient.AuthContext;
import com.mysms.authclient.DataScope;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** The staff reach is the union of the staff permissions' scopes, and none without them. */
class StaffReachTest {

    private final UUID school = UUID.randomUUID();
    private final UUID other = UUID.randomUUID();

    @Test
    void theReachUnitesTheScopesOfTheStaffPermissionsHeld() {
        AuthContext teacher = caller(Set.of(school, other), "assessment.assignment_submission.read",
                "assessment.assignment_submission.grade_assignment_submission");
        DataScope reach = StaffReach.of(teacher, "assessment.assignment_submission.grade_assignment_submission",
                "assessment.assignment_submission.accept_assignment_submission");
        assertThat(reach.institutions()).containsExactlyInAnyOrder(school, other);
        assertThat(reach.all()).isFalse();
        assertThat(StaffReach.covers(reach, DataScope.of(teacher, "assessment.assignment_submission.read"))).isTrue();
    }

    @Test
    void aCallerWithoutAStaffPermissionReachesNothingThoughTheReadScopeIsTheSchool() {
        AuthContext student = caller(Set.of(school), "assessment.assignment_submission.read");
        DataScope reach = StaffReach.of(student, "assessment.assignment_submission.grade_assignment_submission");
        assertThat(reach.institutions()).isEmpty();
        assertThat(reach.canRead(Optional.of(school))).isFalse();
        assertThat(StaffReach.covers(reach, DataScope.of(student, "assessment.assignment_submission.read"))).isFalse();
    }

    private static AuthContext caller(Set<UUID> institutions, String... permissions) {
        return new AuthContext(UUID.randomUUID(), Optional.of(UUID.randomUUID()), institutions, Optional.empty(),
                Set.of(permissions), "jti", Optional.empty(), false, Optional.empty(), Optional.empty(), false);
    }
}
