package com.mysms.identity.iam.business;

import static org.assertj.core.api.Assertions.assertThat;

import com.mysms.authclient.AuthContext;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PermissionCompactorTest {

    private static final List<String> CATALOGUE = List.of(
            "iam.role.read", "iam.role.create", "iam.setting.read_mine", "iam.setting.update_mine",
            "lms.course.read", "lms.course.create", "lms.course_section.read", "lms.cohort.read");

    @Test
    void everythingBecomesOneWildcard() {
        assertThat(PermissionCompactor.compact(CATALOGUE, CATALOGUE)).containsExactly("*");
    }

    @Test
    void completeModulesAndResourcesAreCompactedOthersStayExact() {
        List<String> granted = List.of("iam.role.read", "iam.role.create", "iam.setting.read_mine",
                "iam.setting.update_mine", "lms.course.read", "lms.course.create", "lms.cohort.read");
        assertThat(PermissionCompactor.compact(granted, CATALOGUE))
                .containsExactlyInAnyOrder("iam.*", "lms.course.*", "lms.cohort.*");
        assertThat(PermissionCompactor.compact(List.of("lms.course.read"), CATALOGUE)).containsExactly("lms.course.read");
        assertThat(PermissionCompactor.compact(List.of(), CATALOGUE)).isEmpty();
    }

    @Test
    void theVerifierGrantsExactlyWhatWasCompacted() {
        List<String> granted = List.of("iam.role.read", "iam.role.create", "lms.course.read", "lms.course.create");
        Set<String> compacted = PermissionCompactor.compact(granted, CATALOGUE);
        AuthContext context = new AuthContext(UUID.randomUUID(), Optional.of(UUID.randomUUID()), Set.of(),
                Optional.empty(), compacted, "jti", Optional.empty(), false, Optional.empty(), Optional.empty(), false);
        for (String code : CATALOGUE) {
            assertThat(context.hasPermission(code)).as(code).isEqualTo(granted.contains(code));
        }
    }

    @Test
    void anEmptyCatalogueNeverYieldsAWildcard() {
        assertThat(PermissionCompactor.compact(List.of(), List.of())).isEmpty();
        assertThat(PermissionCompactor.compact(List.of("flat"), List.of("flat"))).containsExactly("*");
    }
}
