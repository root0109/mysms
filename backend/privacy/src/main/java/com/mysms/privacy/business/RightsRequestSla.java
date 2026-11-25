package com.mysms.privacy.business;

import java.time.OffsetDateTime;
import java.time.Period;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * The response deadline of a rights request: its submission plus the module's period for its type
 * ({@code mysms.privacy.rights.sla.<type>}, withdraw-all for withdraw_all). A type without a period is a programming
 * error, never a request without a deadline; a period that is not positive stops the application at start-up.
 */
@Component
public final class RightsRequestSla {

    private final Map<String, Period> periods;

    public RightsRequestSla(@Value("${mysms.privacy.rights.sla.access}") Period access,
            @Value("${mysms.privacy.rights.sla.correction}") Period correction,
            @Value("${mysms.privacy.rights.sla.grievance}") Period grievance,
            @Value("${mysms.privacy.rights.sla.nomination}") Period nomination,
            @Value("${mysms.privacy.rights.sla.erasure}") Period erasure,
            @Value("${mysms.privacy.rights.sla.export}") Period export,
            @Value("${mysms.privacy.rights.sla.withdraw-all}") Period withdrawAll) {
        this.periods = Map.of("access", positive("access", access), "correction", positive("correction", correction),
                "grievance", positive("grievance", grievance), "nomination", positive("nomination", nomination),
                "erasure", positive("erasure", erasure), "export", positive("export", export),
                "withdraw_all", positive("withdraw-all", withdrawAll));
    }

    /** When a request of the type submitted at that time must be answered. */
    public OffsetDateTime dueAt(String type, OffsetDateTime submittedAt) {
        Period period = periods.get(type);
        if (period == null) {
            throw new IllegalStateException("no response period is configured for rights requests of type " + type);
        }
        return submittedAt.plus(period);
    }

    private static Period positive(String type, Period period) {
        if (period.isNegative() || period.isZero()) {
            throw new IllegalArgumentException("mysms.privacy.rights.sla." + type + " must be a positive period");
        }
        return period;
    }
}
