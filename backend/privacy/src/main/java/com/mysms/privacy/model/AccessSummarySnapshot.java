package com.mysms.privacy.model;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * The access summary of one data principal (DPDP Act s.11) as privacy generates it from its own register, and as
 * privacy.rights_request.access_summary keeps it once the access request is fulfilled (jsonb, written and read by the
 * request's repository). It holds no other module's data: per-module counts and the export package come with the
 * fulfilment saga.
 *
 * <p>A kept summary is never rewritten, so it carries the version of its shape: a change to the shape that a reader
 * must know about raises {@link #SCHEMA_VERSION}, and the repository reads a kept summary skipping properties it does
 * not know.
 *
 * @param schemaVersion the version of this shape the summary was generated with ({@link #SCHEMA_VERSION} for a new
 *                      one)
 * @param subjectStatus minor, adult or unknown (privacy.subject.status when generated; unknown without a row)
 * @param subjectRoles  the subject's roles at the school the activities were filtered by (student, guardian, staff);
 *                      empty when none was known (then nothing was left out for its audience); null in a summary of
 *                      schema version 1, which was not filtered by role
 * @param activities    the tenant's register activities that may concern the subject, by activity code
 */
public record AccessSummarySnapshot(int schemaVersion, UUID subjectPersonId, String subjectStatus,
                                    List<String> subjectRoles, OffsetDateTime generatedAt, List<Activity> activities) {

    /**
     * The version of the shape a summary is generated with now: 2 adds the subject's roles and filters the activities
     * by them; a kept summary of version 1 reads with no roles (null).
     */
    public static final int SCHEMA_VERSION = 2;

    public AccessSummarySnapshot {
        subjectRoles = subjectRoles == null ? null : List.copyOf(subjectRoles);
        activities = List.copyOf(activities);
    }

    /**
     * One processing activity of the register that may concern the subject.
     *
     * @param dataCategories       the codes of the data categories it touches, sorted
     * @param recipients           who receives the data, as the register names them
     * @param retentionPolicyCode  the retention policy that applies; null while none is set
     * @param consentAllowedScopes for an activity on the consent basis, the widths the subject's effective consent
     *                             allows, narrowest first (empty = not allowed); null for any other basis
     */
    public record Activity(String activityCode, String module, String purposeCode, String legalBasis,
                           List<String> dataCategories, List<Recipient> recipients, boolean crossBorder,
                           String retentionPolicyCode, List<String> consentAllowedScopes) {

        public Activity {
            dataCategories = List.copyOf(dataCategories);
            recipients = List.copyOf(recipients);
            consentAllowedScopes = consentAllowedScopes == null ? null : List.copyOf(consentAllowedScopes);
        }
    }

    /**
     * A recipient of an activity's data (an element of privacy.processing_activity.recipients).
     *
     * @param kind    processor or fiduciary
     * @param country where the recipient processes the data; null when the register does not say
     */
    public record Recipient(String kind, String name, String country) {
    }
}
