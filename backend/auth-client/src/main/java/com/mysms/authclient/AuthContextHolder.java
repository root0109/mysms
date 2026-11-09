package com.mysms.authclient;

import java.util.Optional;
import java.util.Set;

/** Thread-bound identity and raw token of the current request. Written only by auth-client's own filter. */
final class AuthContextHolder {

    private record Bound(AuthContext context, String token, Set<String> authMethods) {
    }

    private static final ThreadLocal<Bound> CURRENT = new ThreadLocal<>();

    private AuthContextHolder() {
    }

    static void set(AuthContext context, String token) {
        set(context, token, Set.of());
    }

    static void set(AuthContext context, String token, Set<String> authMethods) {
        CURRENT.set(new Bound(context, token, Set.copyOf(authMethods)));
    }

    static Set<String> authMethods() {
        return Optional.ofNullable(CURRENT.get()).map(Bound::authMethods).orElse(Set.of());
    }

    static Optional<AuthContext> current() {
        return Optional.ofNullable(CURRENT.get()).map(Bound::context);
    }

    static Optional<String> token() {
        return Optional.ofNullable(CURRENT.get()).map(Bound::token);
    }

    static void clear() {
        CURRENT.remove();
    }
}
