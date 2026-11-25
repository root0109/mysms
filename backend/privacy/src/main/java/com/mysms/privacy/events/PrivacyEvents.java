package com.mysms.privacy.events;

import com.mysms.privacy.domain.BreachEntity;
import com.mysms.privacy.domain.BreachNotificationEntity;
import com.mysms.privacy.domain.ConsentRecordEntity;
import com.mysms.privacy.domain.LegalHoldEntity;
import com.mysms.privacy.domain.NomineeEntity;
import com.mysms.privacy.domain.RetentionRunEntity;
import com.mysms.privacy.domain.RightsRequestEntity;
import com.mysms.privacy.domain.RightsRequestTaskEntity;
import com.mysms.privacy.domain.TenantRetentionPolicyEntity;
import com.mysms.privacy.model.ScopeState;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Domain events privacy publishes through its outbox: {@code <schema>.<table>.<step>} with ids and codes only, never
 * the text of a notice or a name; consumers keep their copies and consent decisions fresh from these.
 */
public final class PrivacyEvents {

    public static final String CREATED = ".created";
    public static final String UPDATED = ".updated";
    public static final String DELETED = ".deleted";
    public static final String PUBLISHED = ".published";
    public static final String RETIRED = ".retired";
    public static final String RECORDED = ".recorded";
    public static final String SUPERSEDED = ".superseded";
    public static final String WITHDRAWN = ".withdrawn";
    public static final String VERIFIED = ".verified";
    public static final String CHANGED = ".changed";
    public static final String EXPIRED = ".expired";
    public static final String MAJORITY_REACHED = ".majority_reached";
    public static final String SUBMITTED = ".submitted";
    public static final String ASSIGNED = ".assigned";
    public static final String EXTENDED = ".extended";
    public static final String REJECTED = ".rejected";
    public static final String CLOSED = ".closed";
    public static final String REVOKED = ".revoked";
    public static final String ACTIVATED = ".activated";
    public static final String REPORTED = ".reported";
    public static final String TRIAGED = ".triaged";
    public static final String CONTAINED = ".contained";
    public static final String NOTIFICATION_RECORDED = ".notification_recorded";
    public static final String NOT_A_BREACH = ".not_a_breach";
    public static final String PLACED = ".placed";
    public static final String RELEASED = ".released";
    public static final String FULFILMENT_STARTED = ".fulfilment_started";
    public static final String TASK_RETRIED = ".task_retried";
    public static final String ACTION_REQUIRED = ".action_required";

    /** A module's task of an erasure or export request, dispatched to it (design §9.2). */
    public static final String TASK_ASSIGNED = "privacy.rights_request.task_assigned";
    /** An erasure request fulfilled: the tombstone a restore replays (design §9.5). */
    public static final String ERASURE_COMPLETED = "privacy.erasure.completed";
    /** A retention policy's cutoff, sent to the module holding the data (design §10). */
    public static final String RETENTION_DUE = "privacy.retention.due";

    /** The aggregate of privacy.consent_state.changed: the subject's effective consent state. */
    public static final String CONSENT_STATE = "privacy.consent_state";
    /** The aggregate of privacy.subject.majority_reached: a data principal. */
    public static final String SUBJECT = "privacy.subject";

    private PrivacyEvents() {
    }

    public static Map<String, Object> payload(UUID id) {
        return Map.of("id", id.toString());
    }

    /** A version that became its notice's active one; requiresReconsent tells consumers to ask again. */
    public static Map<String, Object> noticeVersionPublished(UUID id, UUID noticeId, int version,
            List<String> purposeCodes, boolean requiresReconsent) {
        return Map.of("id", id.toString(), "noticeId", noticeId.toString(), "version", version,
                "purposeCodes", List.copyOf(purposeCodes), "requiresReconsent", requiresReconsent);
    }

    /** A tenant's setting of a catalogue purpose after a change. */
    public static Map<String, Object> tenantPurposeUpdated(UUID id, UUID purposeId, boolean enabled) {
        return Map.of("id", id.toString(), "purposeId", purposeId.toString(), "enabled", enabled);
    }

    /**
     * A consent record after a step (recorded, superseded, withdrawn, verified): design §6.3's payload, ids and codes
     * only; validUntil and withdrawnAt are null while they do not apply.
     */
    public static Map<String, Object> consentRecord(ConsentRecordEntity record) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("id", record.getId().toString());
        payload.put("subjectPersonId", record.getSubjectPersonId().toString());
        payload.put("giverPersonId", record.getGiverPersonId().toString());
        payload.put("giverCapacity", record.getGiverCapacity());
        payload.put("purposeCode", record.getPurposeCode());
        payload.put("scope", record.getScope());
        payload.put("decision", record.getDecision());
        payload.put("validUntil", Objects.toString(record.getValidUntil(), null));
        payload.put("verificationStatus", record.getVerificationStatus());
        payload.put("withdrawnAt", Objects.toString(record.getWithdrawnAt(), null));
        payload.put("rowVersion", record.getRowVersion());
        return payload;
    }

    /**
     * A rights request after a step (submitted, verified, assigned, extended, rejected, closed): ids, type and status
     * only, never what the requester wrote; assigneeUserId is null until assigned.
     */
    public static Map<String, Object> rightsRequest(RightsRequestEntity request) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("id", request.getId().toString());
        payload.put("subjectPersonId", request.getSubjectPersonId().toString());
        payload.put("requesterPersonId", request.getRequesterPersonId().toString());
        payload.put("assigneeUserId", Objects.toString(request.getAssigneeUserId(), null));
        payload.put("type", request.getType());
        payload.put("status", request.getStatus());
        return payload;
    }

    /**
     * The payload of a privacy.nominee event: ids and status only — never the nominee's name or contact, whose envelopes
     * no consumer could open anyway.
     */
    public static Map<String, Object> nominee(NomineeEntity nominee) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("id", nominee.getId().toString());
        payload.put("principalPersonId", nominee.getPrincipalPersonId().toString());
        payload.put("nomineePersonId", Objects.toString(nominee.getNomineePersonId(), null));
        payload.put("status", nominee.getStatus());
        payload.put("activated", nominee.getActivatedAt() != null);
        return payload;
    }

    /**
     * A breach after a step (reported, updated, triaged, contained, closed, not_a_breach): its id, number, status and
     * the Board's deadline, never what the staff wrote about it.
     */
    public static Map<String, Object> breach(BreachEntity breach) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("id", breach.getId().toString());
        payload.put("number", breach.getNumber());
        payload.put("status", breach.getStatus());
        payload.put("boardReportDueAt", breach.getBoardReportDueAt().toString());
        payload.put("boardReportSent", breach.getBoardReportSentAt() != null);
        return payload;
    }

    /** A breach after a notice about it was recorded: {@link #breach} with the notice's id, audience and kind. */
    public static Map<String, Object> breachNotification(BreachEntity breach, BreachNotificationEntity notice) {
        Map<String, Object> payload = breach(breach);
        payload.put("notificationId", notice.getId().toString());
        payload.put("audience", notice.getAudience());
        payload.put("kind", notice.getKind());
        return payload;
    }

    /**
     * A legal hold after a step (placed, updated, released): what it covers in ids and codes, its review date and
     * whether it is still active.
     */
    public static Map<String, Object> legalHold(LegalHoldEntity hold) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("id", hold.getId().toString());
        payload.put("reasonCode", hold.getReasonCode());
        payload.put("scope", hold.getScope());
        payload.put("subjectPersonId", Objects.toString(hold.getSubjectPersonId(), null));
        payload.put("categoryCode", hold.getCategoryCode());
        payload.put("module", hold.getModule());
        payload.put("reviewBy", hold.getReviewBy().toString());
        payload.put("active", hold.getReleasedAt() == null);
        return payload;
    }

    /**
     * A module's task, dispatched to it (design §6.3): ids, codes and the request's deadline only. It carries no hold:
     * the module asks privacy which holds apply (ListActiveHolds) when it runs the task.
     */
    public static Map<String, Object> taskAssigned(RightsRequestEntity request, RightsRequestTaskEntity task) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("requestId", request.getId().toString());
        payload.put("taskId", task.getId().toString());
        payload.put("type", request.getType());
        payload.put("action", task.getAction());
        payload.put("subjectPersonId", request.getSubjectPersonId().toString());
        payload.put("module", task.getModule());
        payload.put("wave", task.getWave());
        payload.put("deadline", request.getDueAt().toString());
        return payload;
    }

    /** An erasure request fulfilled: the request and the erased principal, ids only. */
    public static Map<String, Object> erasureCompleted(RightsRequestEntity request) {
        return Map.of("requestId", request.getId().toString(),
                "subjectPersonId", request.getSubjectPersonId().toString());
    }

    /** A tenant's setting of a retention policy after a change: the period and action now in force. */
    public static Map<String, Object> retentionSetting(TenantRetentionPolicyEntity setting, String policyCode) {
        return Map.of("id", setting.getId().toString(), "policyId", setting.getPolicyId().toString(),
                "policyCode", policyCode, "periodDays", setting.getPeriodDays(), "action", setting.getAction());
    }

    /** A retention run dispatched to the module holding the data (design §6.3). */
    public static Map<String, Object> retentionDue(RetentionRunEntity run, String policyCode, String categoryCode) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("runId", run.getId().toString());
        payload.put("policyCode", policyCode);
        payload.put("categoryCode", categoryCode);
        payload.put("module", run.getModule());
        payload.put("cutoff", run.getCutoff().toString());
        payload.put("action", run.getAction());
        return payload;
    }

    /** A minor who came of age (design §6.3), with the subject's state version after the answers were recomputed. */
    public static Map<String, Object> majorityReached(UUID personId, LocalDate majorityDate, long stateVersion) {
        return Map.of("personId", personId.toString(), "majorityDate", majorityDate.toString(),
                "stateVersion", stateVersion);
    }

    /**
     * A subject's effective consent to a purpose after a recompute changed it: every width's answer, and the
     * subject's state version, which only grows, so a consumer drops an event older than what it holds.
     */
    public static Map<String, Object> consentStateChanged(UUID subjectPersonId, String purposeCode,
            List<ScopeState> states, long stateVersion) {
        List<Map<String, Object>> scopes = states.stream().map(state -> Map.<String, Object>of(
                "scope", state.scope().value(), "allowed", state.allowed(), "reason", state.reason().name())).toList();
        return Map.of("subjectPersonId", subjectPersonId.toString(), "purposeCode", purposeCode, "states", scopes,
                "stateVersion", stateVersion);
    }
}
