package com.mysms.privacyclient;

import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.UUID;
import tools.jackson.databind.JsonNode;

/**
 * A retention policy's cutoff, read from privacy.retention.due (design §10): the module holding the data removes what
 * the policy covers from before the cutoff, keeps what a hold covers, and answers with counts.
 *
 * @param runId        the run, the id the module answers with
 * @param policyCode   the policy, {@code <module>.<what>}
 * @param categoryCode the data category the policy keeps
 * @param module       the module holding the data
 * @param cutoff       rows whose trigger lies before it are due
 * @param action       delete, anonymise or archive (the tenant's setting)
 */
public record RetentionDue(UUID runId, String policyCode, String categoryCode, String module, OffsetDateTime cutoff,
                           String action) {

    /** The run an event's data names; {@link IllegalArgumentException} when a field is missing or malformed. */
    public static RetentionDue from(JsonNode data) {
        try {
            return new RetentionDue(UUID.fromString(PrivacyTask.text(data, "runId")),
                    PrivacyTask.text(data, "policyCode"), PrivacyTask.text(data, "categoryCode"),
                    PrivacyTask.text(data, "module"), OffsetDateTime.parse(PrivacyTask.text(data, "cutoff")),
                    PrivacyTask.text(data, "action"));
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("a malformed cutoff", e);
        }
    }
}
