package com.mysms.privacyclient;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.mysms.core.context.CallContext;
import com.mysms.core.context.CallContextHolder;
import com.mysms.core.error.UnavailableException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The consent checker of every module (Decorator over a {@link ConsentDecisionSource}, cache-aside): privacy's answers
 * are kept per process in a Caffeine cache keyed by tenant, activity, scope and subject for a short TTL, and only what
 * the cache lacks is asked, in batches. There is no invalidation event — every replica of a module shares one event
 * queue, so an event would reach one replica only — so a change shows everywhere within the TTL, as the role cache's
 * do (owner decision, 23 Nov 2026). Refusals are cached too.
 *
 * <p>An activity privacy allowed on another legal basis than consent ({@link ConsentReason#NON_CONSENT_BASIS}) is
 * answered without asking again for one TTL from that answer, so legitimate-use processing keeps working through a
 * short privacy outage; any other answer about the activity forgets the basis at once. Fail closed: subjects privacy
 * could not answer for are {@link ConsentReason#UNAVAILABLE} (never cached), and only their count, the activity and
 * privacy's reason are logged, never a subject. Any other exception is a programming error and reaches the caller —
 * a check inside a database transaction, for one, which no gRPC call may make.
 */
public final class CachingConsentChecker implements ConsentChecker {

    /** Decisions kept per process: far more than one TTL of a busy process asks. */
    static final long MAXIMUM_ENTRIES = 200_000;
    /** Activities whose non-consent basis is kept: the register lists a few dozen. */
    static final long MAXIMUM_ACTIVITIES = 1_000;

    private static final Logger LOG = LoggerFactory.getLogger(CachingConsentChecker.class);

    private record Key(UUID tenant, ActivityCode activity, ConsentScope scope, UUID subject) {
    }

    private final ConsentDecisionSource source;
    private final Cache<Key, ConsentDecision> decisions;
    private final Cache<ActivityCode, String> bases;

    public CachingConsentChecker(ConsentDecisionSource source, Duration ttl) {
        if (ttl.isNegative() || ttl.isZero()) {
            throw new IllegalArgumentException("mysms.privacy-client.cache-ttl must be positive");
        }
        this.source = source;
        this.decisions = Caffeine.newBuilder().expireAfterWrite(ttl).maximumSize(MAXIMUM_ENTRIES).build();
        this.bases = Caffeine.newBuilder().expireAfterWrite(ttl).maximumSize(MAXIMUM_ACTIVITIES).build();
    }

    @Override
    public ConsentDecision check(ActivityCode activity, UUID subjectPersonId, ConsentScope scope) {
        return checkAll(activity, List.of(subjectPersonId), scope).get(subjectPersonId);
    }

    @Override
    public Map<UUID, ConsentDecision> checkAll(ActivityCode activity, Collection<UUID> subjectPersonIds,
            ConsentScope scope) {
        UUID tenant = CallContextHolder.current().flatMap(CallContext::tenantId)
                .orElseThrow(() -> new IllegalStateException("a consent check needs the call's tenant"));
        Set<UUID> subjects = new LinkedHashSet<>(subjectPersonIds);
        Map<UUID, ConsentDecision> result = new HashMap<>();
        String basis = bases.getIfPresent(activity);
        if (basis != null) {
            subjects.forEach(subject -> result.put(subject, ConsentDecision.nonConsent(basis)));
            return Map.copyOf(result);
        }
        List<UUID> misses = new ArrayList<>();
        for (UUID subject : subjects) {
            ConsentDecision cached = decisions.getIfPresent(new Key(tenant, activity, scope, subject));
            if (cached == null) {
                misses.add(subject);
            } else {
                result.put(subject, cached);
            }
        }
        for (int from = 0; from < misses.size(); from += ConsentDecisionSource.MAX_BATCH) {
            List<UUID> batch = misses.subList(from, Math.min(misses.size(), from + ConsentDecisionSource.MAX_BATCH));
            result.putAll(ask(tenant, activity, batch, scope));
        }
        return Map.copyOf(result);
    }

    /** One batch from privacy, cached; a failure answers every subject of the batch as unavailable. */
    private Map<UUID, ConsentDecision> ask(UUID tenant, ActivityCode activity, List<UUID> batch, ConsentScope scope) {
        Map<UUID, ConsentDecision> answered;
        try {
            answered = source.decide(activity, batch, scope);
        } catch (UnavailableException e) {
            LOG.warn("privacy could not decide {} subject(s) for {}: refused ({})", batch.size(), activity,
                    e.getMessage());
            Map<UUID, ConsentDecision> refused = new HashMap<>();
            batch.forEach(subject -> refused.put(subject, ConsentDecision.unavailable()));
            return refused;
        }
        Map<UUID, ConsentDecision> result = new HashMap<>();
        for (UUID subject : batch) {
            ConsentDecision decision = answered.get(subject);
            if (decision == null) {
                // a source that skipped a subject gave no answer for it
                result.put(subject, ConsentDecision.unavailable());
                continue;
            }
            decisions.put(new Key(tenant, activity, scope, subject), decision);
            learnBasis(activity, decision);
            result.put(subject, decision);
        }
        return result;
    }

    /**
     * Remembers a non-consent basis only from an allow that rests on it, from the first such answer (later ones do
     * not extend it); any other answer means the activity rests on consent, and the basis is forgotten.
     */
    private void learnBasis(ActivityCode activity, ConsentDecision decision) {
        String basis = decision.legalBasis();
        boolean nonConsent = decision.allowed() && decision.reason() == ConsentReason.NON_CONSENT_BASIS
                && basis != null && !basis.isBlank() && !ConsentDecision.CONSENT_BASIS.equals(basis);
        if (nonConsent) {
            bases.asMap().putIfAbsent(activity, basis);
        } else {
            bases.invalidate(activity);
        }
    }
}
