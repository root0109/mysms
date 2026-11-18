package com.mysms.lms.repository;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * The cohorts a caller teaches, as a SQL condition: those where they are an active teacher, and those they manage
 * (lms.cohort.update in the cohort's institution; a tenant-level cohort through a tenant-wide grant only). Rows that
 * belong to a cohort are shown to its teaching staff through {@link #TAUGHT_COHORT_IDS}, bound with {@link #params()}.
 *
 * @param user         the caller
 * @param all          manages cohorts in every institution
 * @param institutions the institutions where the caller manages cohorts
 * @param tenantLevel  manages tenant-level cohorts
 */
public record TeachingReach(UUID user, boolean all, Set<UUID> institutions, boolean tenantLevel) {

    /** The ids of the cohorts the caller teaches: a subquery for {@code <cohort id> IN (…)}. */
    public static final String TAUGHT_COHORT_IDS = """
            SELECT tc.id FROM lms.cohort tc
             WHERE tc.tenant_id = :tenantId AND tc.deleted_at IS NULL
               AND (EXISTS (SELECT 1 FROM lms.cohort_teacher tt WHERE tt.tenant_id = :tenantId AND tt.cohort_id = tc.id
                               AND tt.user_id = :reachUser AND tt.is_active AND tt.deleted_at IS NULL)
                    OR (tc.institution_id IS NULL AND :reachTenantLevel)
                    OR (tc.institution_id IS NOT NULL AND (:reachAll OR tc.institution_id = ANY(:reachIds))))
            """;

    public TeachingReach {
        institutions = Set.copyOf(institutions);
    }

    /** The bind parameters of {@link #TAUGHT_COHORT_IDS}. */
    public Map<String, Object> params() {
        return Map.of("reachUser", user, "reachAll", all, "reachIds", institutions.toArray(UUID[]::new),
                "reachTenantLevel", tenantLevel);
    }
}
