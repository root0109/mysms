package com.mysms.privacy.business;

import com.mysms.privacy.model.ConsentFacts;
import com.mysms.privacy.model.ScopeState;
import java.util.List;

/**
 * Strategy: the effective consent rule (design §4.2) — from the facts about one subject and one purpose, whether
 * processing is allowed at each of the purpose's scopes, and why. A pure function with no I/O, so a policy engine can
 * replace the default ({@link DefaultEffectiveConsentRule}) without touching who gathers the facts or stores the answer
 * ({@link ConsentStateWriter}).
 */
public interface EffectiveConsentRule {

    /** One answer per scope of the purpose ({@code ConsentScope.forKind}), narrowest first. */
    List<ScopeState> decide(ConsentFacts facts);
}
