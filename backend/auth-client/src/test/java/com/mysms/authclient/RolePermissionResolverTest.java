package com.mysms.authclient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.mysms.core.error.UnavailableException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** How the filter turns role claims into permissions, and when it asks iam again. */
class RolePermissionResolverTest {

    private static final UUID TENANT = UUID.randomUUID();
    private static final UUID TEACHER = UUID.randomUUID();
    private static final UUID ADMIN = UUID.randomUUID();
    private static final UUID GONE = UUID.randomUUID();
    private static final String TOKEN = "token";

    /** iam's answers in a unit test: every role's codes at a version, and the calls made. */
    private static final class Iam implements RolePermissionSource {

        private final List<Set<UUID>> asked = new ArrayList<>();
        private long version = 3;
        private boolean down;

        @Override
        public RolePermissions rolePermissions(UUID tenant, Set<UUID> roleIds, String bearerToken) {
            if (down) {
                throw new UnavailableException("iam is down");
            }
            asked.add(Set.copyOf(roleIds));
            return new RolePermissions(version, Map.of(TEACHER, Set.of("sis.student.read", "academics.batch.read"),
                    ADMIN, Set.of("iam.role.read", "iam.role.update")), Set.of("iam.user_account.accept_terms"));
        }
    }

    private static RoleClaims claims(long version, Optional<Set<String>> abilities, String... grants) {
        return RoleClaims.of(List.of(grants), List.of("hr.staff.read"), List.of("iam.role.update"), version,
                abilities.map(List::copyOf));
    }

    @Test
    void permissionsAreTheCommonCodesTheRolesAndTheDirectGrantsMinusTheDenies() {
        RolePermissionResolver resolver = new RolePermissionResolver(new Iam(), Duration.ofMinutes(1));

        RolePermissionResolver.Resolved resolved = resolver.resolve(TENANT,
                claims(3, Optional.empty(), TEACHER.toString(), ADMIN + "@tenant",
                        GONE + "@tenant"), TOKEN);

        assertThat(resolved.permissions()).containsExactlyInAnyOrder("iam.user_account.accept_terms",
                "sis.student.read", "academics.batch.read", "iam.role.read", "hr.staff.read");
        assertThat(resolved.tenantWidePermissions()).as("tenant-wide grants and direct grants only")
                .containsExactlyInAnyOrder("iam.role.read", "hr.staff.read");
    }

    @Test
    void aLimitedApiTokensAbilitiesCapBoth() {
        RolePermissionResolver resolver = new RolePermissionResolver(new Iam(), Duration.ofMinutes(1));

        RolePermissionResolver.Resolved resolved = resolver.resolve(TENANT,
                claims(3, Optional.of(Set.of("iam.role.read", "sis.student.read")), TEACHER.toString(),
                        ADMIN + "@tenant"), TOKEN);

        assertThat(resolved.permissions()).containsExactlyInAnyOrder("iam.role.read", "sis.student.read");
        assertThat(resolved.tenantWidePermissions()).containsExactly("iam.role.read");
    }

    @Test
    void iamIsAskedOnlyForWhatTheCacheLacksOrHoldsOlderThanTheToken() {
        Iam iam = new Iam();
        RolePermissionResolver resolver = new RolePermissionResolver(iam, Duration.ofMinutes(1));

        resolver.resolve(TENANT, claims(3, Optional.empty(), TEACHER + "@tenant"), TOKEN);
        resolver.resolve(TENANT, claims(2, Optional.empty(), TEACHER + "@tenant"), TOKEN);
        resolver.resolve(TENANT, claims(3, Optional.empty(), TEACHER + "@tenant", ADMIN + "@tenant"), TOKEN);
        assertThat(iam.asked).as("a cached role is not asked again; a new one is").containsExactly(Set.of(TEACHER),
                Set.of(ADMIN));

        iam.version = 4;
        resolver.resolve(TENANT, claims(4, Optional.empty(), TEACHER + "@tenant"), TOKEN);
        assertThat(iam.asked).as("a token issued after a change refreshes the role").hasSize(3).last()
                .isEqualTo(Set.of(TEACHER));
    }

    @Test
    void whatTheCacheLacksWhileIamIsDownIsRefused() {
        Iam iam = new Iam();
        RolePermissionResolver resolver = new RolePermissionResolver(iam, Duration.ofMinutes(1));
        resolver.resolve(TENANT, claims(3, Optional.empty(), TEACHER + "@tenant"), TOKEN);
        iam.down = true;

        assertThat(resolver.resolve(TENANT, claims(3, Optional.empty(), TEACHER + "@tenant"), TOKEN).permissions())
                .as("answered from the cache").contains("sis.student.read");
        assertThatThrownBy(() -> resolver.resolve(TENANT, claims(3, Optional.empty(), ADMIN + "@tenant"), TOKEN))
                .isInstanceOf(UnavailableException.class);
        assertThatThrownBy(() -> new RolePermissionResolver(iam, Duration.ZERO))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
