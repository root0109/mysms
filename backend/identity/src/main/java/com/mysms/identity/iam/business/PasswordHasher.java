package com.mysms.identity.iam.business;

import java.security.SecureRandom;
import java.util.HexFormat;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;

/**
 * argon2id password hashing with the OWASP parameters: 19 MiB memory,
 * 2 iterations, parallelism 1, 16-byte salt, 32-byte hash. Every verification costs the same whether or not the
 * account exists or has a usable hash: a missing or foreign hash is checked against a dummy hash, so response times
 * do not reveal which accounts exist.
 */
public final class PasswordHasher {

    private static final int SALT_BYTES = 16;
    private static final int HASH_BYTES = 32;
    private static final int PARALLELISM = 1;
    private static final int MEMORY_KIB = 19_456;
    private static final int ITERATIONS = 2;
    private static final String ARGON2ID_PREFIX = "$argon2id$";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final Argon2PasswordEncoder encoder =
            new Argon2PasswordEncoder(SALT_BYTES, HASH_BYTES, PARALLELISM, MEMORY_KIB, ITERATIONS);
    private final String dummyHash;

    public PasswordHasher() {
        byte[] random = new byte[SALT_BYTES];
        RANDOM.nextBytes(random);
        this.dummyHash = encoder.encode(HexFormat.of().formatHex(random));
    }

    public String hash(String password) {
        if (password == null || password.isEmpty()) {
            throw new IllegalArgumentException("password is required");
        }
        return encoder.encode(password);
    }

    /** Constant-cost check; false for a missing, empty or non-argon2id hash. */
    public boolean matches(String password, String hash) {
        String candidate = password == null ? "" : password;
        if (hash == null || !hash.startsWith(ARGON2ID_PREFIX)) {
            encoder.matches(candidate, dummyHash);
            return false;
        }
        return encoder.matches(candidate, hash);
    }

    /** Spends one verification's time without an account, for refusals that must not be faster than a check. */
    public void burn(String password) {
        encoder.matches(password == null ? "" : password, dummyHash);
    }
}
