package com.mysms.lms.business;

import com.mysms.authclient.AuthContext;
import com.mysms.authclient.DataScope;
import com.mysms.core.error.PermissionDeniedException;
import com.mysms.core.error.ResourceNotFoundException;
import com.mysms.core.error.UnauthenticatedException;
import com.mysms.lms.domain.CohortEntity;
import com.mysms.lms.repository.CohortRepository;
import com.mysms.lms.repository.TeachingReach;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Who may do what in one cohort (the data-level rules for the cohort's workflow
 * operations), decided once for every service that acts inside a cohort. Called inside the caller's transaction.
 *
 * <ul>
 *   <li>A cohort outside the read scope of the operation's permission is 404, never a hint that it exists.</li>
 *   <li>A <em>manager</em> holds lms.cohort.update in the cohort's institution: they see drafts and act on every
 *       member.</li>
 *   <li>The cohort's <em>teaching staff</em> are its managers and its active teachers (lms.cohort_teacher).</li>
 *   <li>Everybody else sees a cohort only once it is published, or when they are placed in it.</li>
 * </ul>
 */
@Component
public class CohortAccess {

    static final String COHORT = "lms.cohort";
    static final String PERMISSION_MANAGE = "lms.cohort.update";

    private final CohortRepository cohorts;

    public CohortAccess(CohortRepository cohorts) {
        this.cohorts = cohorts;
    }

    /** The calling member; a public read without a token reaches the guest projection instead. */
    public static AuthContext member() {
        return AuthContext.current().orElseThrow(() -> new UnauthenticatedException("UNAUTHENTICATED",
                "Sign in to read this cohort."));
    }

    /** The cohorts the caller teaches, for reads that show a cohort's rows to its teaching staff. */
    public static TeachingReach reach() {
        AuthContext caller = member();
        boolean manager = caller.hasPermission(PERMISSION_MANAGE);
        DataScope manage = DataScope.of(caller, PERMISSION_MANAGE);
        return new TeachingReach(caller.userId(), manager && manage.all(),
                manager ? manage.institutions() : Set.of(), caller.hasTenantWidePermission(PERMISSION_MANAGE));
    }

    /** The cohort as the caller may see it for the operation's permission: absent, out of scope or hidden is 404. */
    public CohortEntity visible(UUID cohortId, String permission) {
        AuthContext caller = member();
        CohortEntity cohort = cohorts.findById(cohortId).orElseThrow(() -> new ResourceNotFoundException(COHORT,
                cohortId));
        if (!DataScope.of(caller, permission).canRead(institutionOf(cohort))) {
            throw new ResourceNotFoundException(COHORT, cohortId);
        }
        if (!manages(caller, cohort) && !Boolean.TRUE.equals(cohort.getIsPublished())
                && !cohorts.isStudent(cohortId, caller.userId()) && !cohorts.isTeacher(cohortId, caller.userId())) {
            throw new ResourceNotFoundException(COHORT, cohortId);
        }
        return cohort;
    }

    /** The cohort the caller acts on as its teaching staff (403 for a visible cohort they do not teach). */
    public CohortEntity taught(UUID cohortId, String permission) {
        CohortEntity cohort = visible(cohortId, permission);
        requireTeachingStaff(cohort);
        return cohort;
    }

    /** Refuses a caller who is not the cohort's teaching staff (403). */
    public void requireTeachingStaff(CohortEntity cohort) {
        if (!isTeachingStaff(cohort)) {
            throw new PermissionDeniedException("Only the cohort's teachers may do this.");
        }
    }

    /** Whether the caller manages the cohort or teaches it. */
    public boolean isTeachingStaff(CohortEntity cohort) {
        AuthContext caller = member();
        return manages(caller, cohort) || cohorts.isTeacher(cohort.getId(), caller.userId());
    }

    /** Whether the caller teaches the cohort (false for an absent one). */
    public boolean teaches(UUID cohortId) {
        return cohorts.findById(cohortId).map(this::isTeachingStaff).orElse(false);
    }

    /**
     * Refuses a caller who neither teaches nor studies in the cohort (404 when they cannot see it, else 403); true for
     * its teaching staff.
     */
    public boolean requireMember(UUID cohortId, String permission) {
        CohortEntity cohort = visible(cohortId, permission);
        if (isTeachingStaff(cohort)) {
            return true;
        }
        if (!isStudent(cohort)) {
            throw new PermissionDeniedException("Only the cohort's students and teachers may do this.");
        }
        return false;
    }

    /** Whether the caller is an active student of the cohort. */
    public boolean isStudent(CohortEntity cohort) {
        return cohorts.isStudent(cohort.getId(), member().userId());
    }

    /** The institution that owns the cohort (empty: a tenant-level cohort). */
    public static Optional<UUID> institutionOf(CohortEntity cohort) {
        return Optional.ofNullable(cohort.getInstitutionId());
    }

    /** A tenant-level cohort is managed through a tenant-wide grant only, as every tenant-level row is changed. */
    private static boolean manages(AuthContext caller, CohortEntity cohort) {
        Optional<UUID> institution = institutionOf(cohort);
        if (institution.isEmpty()) {
            return caller.hasTenantWidePermission(PERMISSION_MANAGE);
        }
        return caller.hasPermission(PERMISSION_MANAGE)
                && DataScope.of(caller, PERMISSION_MANAGE).canRead(institution);
    }
}
