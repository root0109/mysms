package com.mysms.privacy.model;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Everything the effective consent rule (design §4.2) needs about one subject and one purpose, as of one day.
 *
 * @param subjectPersonId    the subject
 * @param subjectStatus      privacy.subject.status: adult, minor or unknown (anything but adult counts as a minor)
 * @param forbiddenForMinors the catalogue purpose is never allowed for a minor
 * @param scopeKind          privacy.purpose.scope_kind: width or tenant
 * @param purposeEnabled     the tenant processes data for the purpose
 * @param maxScope           the tenant's widest allowed width for a width purpose; null = no ceiling
 * @param decisions          every giver's current decision about the subject and purpose, entitled or not
 * @param legalGuardians     the persons with an active legal-guardian link to the subject
 * @param reconsentSince     when the latest notice version asking everyone again was published; null = never
 * @param today              the day the answer is for (expiry)
 */
public record ConsentFacts(UUID subjectPersonId, String subjectStatus, boolean forbiddenForMinors, String scopeKind,
                           boolean purposeEnabled, ConsentScope maxScope, List<GiverDecision> decisions,
                           Set<UUID> legalGuardians, OffsetDateTime reconsentSince, LocalDate today) {

    public ConsentFacts {
        decisions = List.copyOf(decisions);
        legalGuardians = Set.copyOf(legalGuardians);
    }

    /** Only an adult decides for themself; a minor and a person of unknown age are decided for by legal guardians. */
    public boolean adult() {
        return Majority.ADULT.equals(subjectStatus);
    }
}
