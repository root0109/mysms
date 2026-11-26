package com.mysms.identity.people.business;

import com.mysms.authclient.AuthContext;
import com.mysms.core.error.FieldError;
import com.mysms.core.error.ResourceNotFoundException;
import com.mysms.core.error.UnavailableException;
import com.mysms.core.error.ValidationFailedException;
import com.mysms.privacyclient.ActivityCode;
import com.mysms.privacyclient.ConsentChecker;
import com.mysms.privacyclient.ConsentDecision;
import com.mysms.privacyclient.ConsentReason;
import com.mysms.privacyclient.ConsentScope;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * The consent a public profile rests on (design PRIVACY-AND-CONSENT-CENTRE §8, activity {@code people.public_profile},
 * one decision for the whole school): a profile is created only for a person who consented, and shown to anyone but
 * its own person only while that consent holds — a withdrawal hides it, within the consent cache's TTL. Checked
 * outside the database transactions, since no gRPC call is made inside one. While privacy cannot answer, nothing is
 * created or shown (503, fail closed).
 */
@Component
public class PublicProfileConsentChecker {

    static final ActivityCode PUBLIC_PROFILE = new ActivityCode("people.public_profile");
    static final String CONSENT_REQUIRED = "CONSENT_REQUIRED";

    private final ConsentChecker consent;

    public PublicProfileConsentChecker(ConsentChecker consent) {
        this.consent = consent;
    }

    /** Before a profile is created: 422 {@code CONSENT_REQUIRED} on personId without the person's consent. */
    public void requireToPublish(UUID personId) {
        ConsentDecision decision = consent.check(PUBLIC_PROFILE, personId, ConsentScope.TENANT);
        if (decision.allowed()) {
            return;
        }
        refuseWhileUnavailable(decision);
        throw new ValidationFailedException(List.of(new FieldError("personId",
                "needs the person's consent, which has not been given", CONSENT_REQUIRED)));
    }

    /**
     * Before a profile is shown to the caller: its own person always sees it (their own data, no new purpose); anyone
     * else only with the person's consent, else 404 as if there were no profile.
     */
    public void requireToShow(UUID personId, UUID profileId) {
        if (AuthContext.require().personId().filter(personId::equals).isPresent()) {
            return;
        }
        ConsentDecision decision = consent.check(PUBLIC_PROFILE, personId, ConsentScope.TENANT);
        if (decision.allowed()) {
            return;
        }
        refuseWhileUnavailable(decision);
        throw new ResourceNotFoundException(PersonService.PUBLIC_PROFILE_AGGREGATE, profileId);
    }

    private static void refuseWhileUnavailable(ConsentDecision decision) {
        if (decision.reason() == ConsentReason.UNAVAILABLE) {
            throw new UnavailableException("The person's consent cannot be checked right now; try again.");
        }
    }
}
