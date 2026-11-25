package com.mysms.privacyclient.testing;

import com.mysms.core.error.UnavailableException;
import com.mysms.privacyclient.ActivityCode;
import com.mysms.privacyclient.ConsentDecision;
import com.mysms.privacyclient.ConsentDecisionSource;
import com.mysms.privacyclient.ConsentReason;
import com.mysms.privacyclient.ConsentScope;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * privacy's answers in a module's component tests (Strategy of {@link ConsentDecisionSource}), which run without
 * privacy: a test allows a subject for an activity, or makes privacy unavailable for one; every other subject is
 * {@link ConsentReason#NOT_ANSWERED}. The checker, its cache and the module's code stay real; only the hop to privacy
 * is replaced, and only in component tests. The checker caches answers, so a test uses subjects of its own.
 */
public final class TestConsents implements ConsentDecisionSource {

    private static final Map<ActivityCode, Set<UUID>> ALLOWED = new ConcurrentHashMap<>();
    private static final Set<UUID> UNAVAILABLE = ConcurrentHashMap.newKeySet();

    /** The subject has consented to the activity, in every tenant. */
    public static void allow(ActivityCode activity, UUID subjectPersonId) {
        ALLOWED.computeIfAbsent(activity, a -> ConcurrentHashMap.newKeySet()).add(subjectPersonId);
    }

    /** privacy cannot be asked about the subject: a batch naming it fails. */
    public static void unavailableFor(UUID subjectPersonId) {
        UNAVAILABLE.add(subjectPersonId);
    }

    @Override
    public Map<UUID, ConsentDecision> decide(ActivityCode activity, List<UUID> subjectPersonIds, ConsentScope scope) {
        if (subjectPersonIds.stream().anyMatch(UNAVAILABLE::contains)) {
            throw new UnavailableException("privacy is unavailable in this test.");
        }
        Set<UUID> allowed = ALLOWED.getOrDefault(activity, Set.of());
        Map<UUID, ConsentDecision> decisions = new HashMap<>();
        for (UUID subject : subjectPersonIds) {
            decisions.put(subject, allowed.contains(subject)
                    ? new ConsentDecision(true, ConsentReason.CONSENT, "consent", 1)
                    : new ConsentDecision(false, ConsentReason.NOT_ANSWERED, "consent", 1));
        }
        return decisions;
    }
}
