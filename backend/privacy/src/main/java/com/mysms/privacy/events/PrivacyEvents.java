package com.mysms.privacy.events;

import com.mysms.privacy.domain.ConsentRecordEntity;
import com.mysms.privacy.model.ScopeState;
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

    /** The aggregate of privacy.consent_state.changed: the subject's effective consent state. */
    public static final String CONSENT_STATE = "privacy.consent_state";

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
