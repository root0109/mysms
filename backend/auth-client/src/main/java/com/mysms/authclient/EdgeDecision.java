package com.mysms.authclient;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;

/** What the edge decided about one call, for whichever transport carries it ({@link EdgeAuthorizer}). */
public sealed interface EdgeDecision {

    /**
     * The call is refused; each transport writes the answer its own way (problem+json, google.rpc.Status).
     *
     * @param challenge true for a 401 that invites a (new) bearer token ({@code WWW-Authenticate} on REST)
     */
    record Refused(HttpStatus status, String code, String detail, boolean challenge) implements EdgeDecision {
    }

    /**
     * The call may run as {@code caller}: empty for an operation callable without a token that came without one.
     *
     * @param token       the raw bearer token, forwarded on the caller's SDK calls
     * @param authMethods how the token was obtained ({@code amr})
     * @param guestTenant for a guest (no caller), the tenant its {@code X-Tenant-Code} named; empty otherwise
     */
    record Admitted(Optional<AuthContext> caller, String token, Set<String> authMethods, Optional<UUID> guestTenant)
            implements EdgeDecision {

        public Admitted {
            authMethods = Set.copyOf(authMethods);
            if (caller.isPresent() && guestTenant.isPresent()) {
                throw new IllegalArgumentException("a caller with a token is never a guest");
            }
        }

        public Admitted(Optional<AuthContext> caller, String token, Set<String> authMethods) {
            this(caller, token, authMethods, Optional.empty());
        }

        /** A guest reading the school its code named. */
        static Admitted guest(UUID tenant) {
            return new Admitted(Optional.empty(), null, Set.of(), Optional.of(tenant));
        }
    }
}
