package com.mysms.identity.iam.business;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class PasswordHasherTest {

    private final PasswordHasher hasher = new PasswordHasher();

    @Test
    void hashesWithArgon2idAndSalts() {
        String first = hasher.hash("correct horse battery staple");
        String second = hasher.hash("correct horse battery staple");
        assertThat(first).startsWith("$argon2id$v=19$m=19456,t=2,p=1$").isNotEqualTo(second);
        assertThat(hasher.matches("correct horse battery staple", first)).isTrue();
        assertThat(hasher.matches("correct horse battery stapl", first)).isFalse();
        assertThat(hasher.matches(null, first)).isFalse();
    }

    @Test
    void missingOrForeignHashesNeverMatch() {
        assertThat(hasher.matches("secret", null)).isFalse();
        assertThat(hasher.matches("secret", "")).isFalse();
        assertThat(hasher.matches("secret", "$2a$10$abcdefghijklmnopqrstuuJ1Bv6s0bP9oI2rWfX2XjS7z7z7z7z7a")).isFalse();
        hasher.burn("anything");
        hasher.burn(null);
    }

    @Test
    void refusesEmptyPasswords() {
        assertThatThrownBy(() -> hasher.hash("")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> hasher.hash(null)).isInstanceOf(IllegalArgumentException.class);
    }
}
