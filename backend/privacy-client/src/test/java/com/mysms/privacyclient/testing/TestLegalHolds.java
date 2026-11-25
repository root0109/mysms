package com.mysms.privacyclient.testing;

import com.mysms.core.error.UnavailableException;
import com.mysms.privacyclient.ActiveHold;
import com.mysms.privacyclient.ActiveHolds;
import com.mysms.privacyclient.LegalHolds;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * privacy's legal holds in a module's component tests (Strategy of {@link LegalHolds}), which run without privacy: a
 * test holds a subject, or makes privacy unavailable for one; every other subject has no hold. The module's handler
 * stays real; only the hop to privacy is replaced, and only in component tests. A test uses subjects of its own.
 */
public final class TestLegalHolds implements LegalHolds {

    private static final Map<UUID, List<ActiveHold>> HOLDS = new ConcurrentHashMap<>();
    private static final Set<UUID> UNAVAILABLE = ConcurrentHashMap.newKeySet();

    /** A subject hold: every row of the subject is kept. */
    public static void holdSubject(UUID subjectPersonId) {
        HOLDS.put(subjectPersonId, List.of(new ActiveHold(UUID.randomUUID(), ActiveHold.SUBJECT, subjectPersonId,
                null, null, null, Set.of())));
    }

    /** privacy cannot be asked about the subject. */
    public static void unavailableFor(UUID subjectPersonId) {
        UNAVAILABLE.add(subjectPersonId);
    }

    @Override
    public ActiveHolds activeHolds(String module, UUID subjectPersonId) {
        if (UNAVAILABLE.contains(subjectPersonId)) {
            throw new UnavailableException("privacy is unavailable in this test.");
        }
        return new ActiveHolds(HOLDS.getOrDefault(subjectPersonId, List.of()));
    }
}
