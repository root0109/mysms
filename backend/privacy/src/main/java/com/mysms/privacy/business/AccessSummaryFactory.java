package com.mysms.privacy.business;

import com.mysms.privacy.model.AccessSummarySnapshot;
import com.mysms.privacy.model.ConsentScope;
import com.mysms.privacy.model.Majority;
import com.mysms.privacy.repository.AccessSummaryRepository;
import com.mysms.privacy.repository.AppliedActivityView;
import com.mysms.privacy.repository.SubjectRepository;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Generates the access summary of a data principal from privacy's own data (design §9.3): the tenant's register
 * activities that may concern them — the ones not on the consent basis always, a consent-based one only while the
 * tenant has its purpose switched on and, for anyone not known to be an adult, only an activity that processes data of
 * minors and never one whose purpose is forbidden for minors — with, for a consent-based one, the widths their
 * effective consent allows now. The activities are those whose audiences include one of the person's roles at the
 * school (student, guardian, staff: {@link SubjectRepository#roles}); a person privacy knows no role of — a visitor,
 * or someone whose records have not arrived — gets every activity that may concern them, the conservative "may
 * concern" answer. Runs in the caller's transaction.
 */
@Component
public class AccessSummaryFactory {

    private static final String CONSENT = "consent";

    private final AccessSummaryRepository register;
    private final SubjectRepository subjects;

    public AccessSummaryFactory(AccessSummaryRepository register, SubjectRepository subjects) {
        this.register = register;
        this.subjects = subjects;
    }

    /** The subject's summary as of {@code generatedAt}; a person privacy has no row for is of unknown age. */
    public AccessSummarySnapshot build(UUID subjectPersonId, OffsetDateTime generatedAt) {
        // Two statements under READ COMMITTED: a consent change between them is ruled out only when the caller holds
        // the subject's row lock (as FulfilAccessRequest does, as every consent change takes it); a tenant switching a
        // purpose on or off is not serialised by that lock and may land between them (design §9.6).
        String status = subjects.majority(subjectPersonId).map(Majority::status).orElse(Majority.UNKNOWN);
        List<String> roles = subjects.roles(subjectPersonId);
        List<AccessSummarySnapshot.Activity> activities = register
                .applied(subjectPersonId, Majority.ADULT.equals(status), roles).stream()
                .map(AccessSummaryFactory::activity).toList();
        return new AccessSummarySnapshot(AccessSummarySnapshot.SCHEMA_VERSION, subjectPersonId, status, roles,
                generatedAt, activities);
    }

    private static AccessSummarySnapshot.Activity activity(AppliedActivityView view) {
        List<String> allowed = CONSENT.equals(view.legalBasis()) ? view.allowedScopes().stream()
                .map(ConsentScope::of).sorted().map(ConsentScope::value).toList() : null;
        return new AccessSummarySnapshot.Activity(view.code(), view.module(), view.purposeCode(), view.legalBasis(),
                view.categories(), view.recipients(), view.crossBorder(), view.retentionPolicyCode(), allowed);
    }
}
