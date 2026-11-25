package com.mysms.privacyclient;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Answers whether personal data of subjects may be used for a module's processing activity, in the tenant of the
 * current call: the explicit check a business service makes next to its {@code DataScope} check (design §7.1). It
 * fails closed: an answer privacy cannot give is {@link ConsentReason#UNAVAILABLE}, never an allow. A person reading
 * their own data, or a guardian their child's, is access, not a new purpose: no check.
 */
public interface ConsentChecker {

    /** One subject's decision. */
    ConsentDecision check(ActivityCode activity, UUID subjectPersonId, ConsentScope scope);

    /** One decision per subject, keyed by subject; what the cache lacks is asked in batches of at most 1000. */
    Map<UUID, ConsentDecision> checkAll(ActivityCode activity, Collection<UUID> subjectPersonIds, ConsentScope scope);

    /** The subjects whose data may be used: a filter's convenience over {@link #checkAll}. */
    default Set<UUID> allowed(ActivityCode activity, Collection<UUID> subjectPersonIds, ConsentScope scope) {
        return checkAll(activity, subjectPersonIds, scope).entrySet().stream().filter(e -> e.getValue().allowed())
                .map(Map.Entry::getKey).collect(Collectors.toUnmodifiableSet());
    }
}
