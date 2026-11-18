package com.mysms.admission.business;

import com.mysms.admission.domain.ApplicationEntity;
import com.mysms.admission.repository.ApplicationRepository;
import com.mysms.authclient.AuthContext;
import com.mysms.authclient.DataScope;
import com.mysms.core.error.InvalidStateException;
import com.mysms.core.paging.SqlFragment;
import java.util.Optional;
import java.util.UUID;

/**
 * Which applications a caller reaches with one permission. The admissions office reaches every application of its
 * institutions. An applicant has no institution in scope and reaches only the applications they started in an admission
 * cycle (another applicant's are 404); they may change one only while it is a draft.
 * @param scope the caller's institution scope for the permission
 */
record ApplicationAccess(DataScope scope, UUID user) {

    static final String DRAFT = "draft";

    /** The reach of the current request's caller for the operation's permission. */
    static ApplicationAccess current(String permission) {
        return new ApplicationAccess(DataScope.current(permission), AuthContext.require().userId());
    }

    /** Whether the caller may see the application at all; anything else does not exist for them (404). */
    boolean reads(ApplicationEntity application) {
        return inScope(application) || own(application);
    }

    /** The caller reaches the application only as the applicant who started it. */
    boolean selfService(ApplicationEntity application) {
        return !inScope(application) && own(application);
    }

    /**
     * Refuses a change the caller may not make: the office needs the institution in scope (403), an applicant a
     * draft of their own (409 once it is submitted).
     */
    void requireWritable(ApplicationEntity application) {
        if (selfService(application)) {
            if (!DRAFT.equals(application.getStatus())) {
                throw new InvalidStateException("An applicant changes an application only while it is a draft.");
            }
            return;
        }
        scope.requireWritable(Optional.ofNullable(application.getInstitutionId()));
    }

    /** The list predicate of what {@link #reads} allows, over admission.application rows aliased r. */
    SqlFragment listFilter() {
        return ApplicationRepository.reach(scope, user);
    }

    /** The list predicate of the rows the caller reaches as the office: their institutions only. */
    SqlFragment officeFilter() {
        return ApplicationRepository.inScope(scope);
    }

    /** Whether the caller reaches no institution at all: an applicant, who reaches only their own applications. */
    boolean applicantOnly() {
        return !scope.all() && scope.institutions().isEmpty();
    }

    private boolean inScope(ApplicationEntity application) {
        return scope.canRead(Optional.ofNullable(application.getInstitutionId()));
    }

    private boolean own(ApplicationEntity application) {
        return application.getAdmissionCycleId() != null && user.equals(application.getCreatedBy());
    }
}
