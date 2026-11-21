package com.mysms.authclient.testing;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The roles of the tokens {@link TestTokens} signs: a token's permissions become one role per scope, and this registry
 * remembers each role's codes so {@link TestRoleData} can answer for it as iam would. A role's id is derived from its
 * codes, so the same permissions always make the same role, in every test of the JVM.
 */
public final class TestRoles {

    private static final Map<UUID, Set<String>> CODES = new ConcurrentHashMap<>();

    private TestRoles() {
    }

    /** The role holding exactly these codes. */
    public static UUID role(Set<String> codes) {
        Set<String> sorted = new TreeSet<>(codes);
        UUID id = UUID.nameUUIDFromBytes(("mysms:test-role:" + String.join(",", sorted))
                .getBytes(StandardCharsets.UTF_8));
        CODES.putIfAbsent(id, Set.copyOf(sorted));
        return id;
    }

    /** The codes of a role made here; empty for any other role. */
    static Optional<Set<String>> codes(UUID role) {
        return Optional.ofNullable(CODES.get(role));
    }
}
