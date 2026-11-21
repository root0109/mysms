package com.mysms.privacy.business;

import com.mysms.privacy.model.ConsentFacts;
import com.mysms.privacy.model.ConsentReason;
import com.mysms.privacy.model.ConsentScope;
import com.mysms.privacy.model.GiverDecision;
import com.mysms.privacy.model.ScopeState;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * The default {@link EffectiveConsentRule}, design §4.2 for a consent-based activity, per scope W of the purpose:
 * <ol>
 * <li>a purpose forbidden for minors is denied for anyone not known to be an adult (FORBIDDEN_FOR_MINOR);</li>
 * <li>a purpose the tenant has switched off, or a width above the tenant's ceiling, is denied (PURPOSE_DISABLED);</li>
 * <li>only entitled givers count — the adult subject alone, or for a minor (or a subject of unknown age) the persons
 * with an active legal-guardian link — and only their verified decisions; a decision past its valid_until no longer
 * counts;</li>
 * <li>a counting refusal at W or narrower denies (REFUSED); for a minor, a guardian's grant narrower than W denies
 * (NARROWER_GRANT: the most restrictive guardian decides, while a silent guardian does not block);</li>
 * <li>a counting grant at W or wider allows (CONSENT), unless it was recorded before the latest notice version that
 * asks everyone again was published (RECONSENT_REQUIRED);</li>
 * <li>otherwise a lapsed decision explains the silence (EXPIRED), and silence never allows (NOT_ANSWERED).</li>
 * </ol>
 * Refusals and narrower grants restrict even when they predate a re-consent notice: asking again never widens what a
 * guardian limited.
 */
@Component
public class DefaultEffectiveConsentRule implements EffectiveConsentRule {

    @Override
    public List<ScopeState> decide(ConsentFacts facts) {
        List<ConsentScope> scopes = ConsentScope.forKind(facts.scopeKind());
        Set<UUID> givers = facts.adult() ? Set.of(facts.subjectPersonId()) : facts.legalGuardians();
        List<GiverDecision> counted = facts.decisions().stream()
                .filter(d -> d.verified() && givers.contains(d.giverPersonId()) && scopes.contains(d.scope()))
                .toList();
        List<GiverDecision> live = counted.stream().filter(d -> !lapsed(d, facts)).toList();
        boolean lapsed = live.size() < counted.size();
        return scopes.stream().map(scope -> decide(facts, scope, live, lapsed)).toList();
    }

    private static ScopeState decide(ConsentFacts facts, ConsentScope scope, List<GiverDecision> live,
            boolean lapsed) {
        if (facts.forbiddenForMinors() && !facts.adult()) {
            return ScopeState.denied(scope, ConsentReason.FORBIDDEN_FOR_MINOR);
        }
        ConsentScope ceiling = facts.maxScope();
        if (!facts.purposeEnabled() || (ceiling != null && !scope.within(ceiling))) {
            return ScopeState.denied(scope, ConsentReason.PURPOSE_DISABLED);
        }
        if (live.stream().anyMatch(d -> !d.granted() && d.scope().within(scope))) {
            return ScopeState.denied(scope, ConsentReason.REFUSED);
        }
        if (!facts.adult() && live.stream().anyMatch(d -> d.granted() && !scope.within(d.scope()))) {
            return ScopeState.denied(scope, ConsentReason.NARROWER_GRANT);
        }
        List<GiverDecision> covering = live.stream().filter(d -> d.granted() && scope.within(d.scope())).toList();
        if (covering.stream().anyMatch(d -> !predatesReconsent(d, facts))) {
            return ScopeState.allowed(scope);
        }
        if (!covering.isEmpty()) {
            return ScopeState.denied(scope, ConsentReason.RECONSENT_REQUIRED);
        }
        return ScopeState.denied(scope, lapsed ? ConsentReason.EXPIRED : ConsentReason.NOT_ANSWERED);
    }

    /** Past its last day: valid_until is the last day the decision counts. */
    private static boolean lapsed(GiverDecision decision, ConsentFacts facts) {
        LocalDate lastDay = decision.validUntil();
        return lastDay != null && lastDay.isBefore(facts.today());
    }

    private static boolean predatesReconsent(GiverDecision decision, ConsentFacts facts) {
        OffsetDateTime askedAgain = facts.reconsentSince();
        return askedAgain != null && decision.validFrom().isBefore(askedAgain);
    }
}
