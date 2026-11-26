package com.mysms.identity.iam.business;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;

class TotpTest {

    /** RFC 6238 appendix B, SHA-1 seed "12345678901234567890", last six digits of the eight-digit codes. */
    private static final String RFC_SECRET = Totp.base32("12345678901234567890".getBytes(StandardCharsets.US_ASCII));

    @Test
    void matchesTheRfcTestVectors() {
        assertThat(Totp.codeAt(RFC_SECRET, 59)).isEqualTo("287082");
        assertThat(Totp.codeAt(RFC_SECRET, 1111111109)).isEqualTo("081804");
        assertThat(Totp.codeAt(RFC_SECRET, 1111111111)).isEqualTo("050471");
        assertThat(Totp.codeAt(RFC_SECRET, 1234567890)).isEqualTo("005924");
        assertThat(Totp.codeAt(RFC_SECRET, 2000000000)).isEqualTo("279037");
    }

    @Test
    void acceptsOneStepOfDriftAndNeverAStepTwice() {
        long t = 1234567890;
        long step = t / Totp.STEP_SECONDS;
        assertThat(Totp.verify(RFC_SECRET, Totp.codeAt(RFC_SECRET, t), t, Long.MIN_VALUE)).hasValue(step);
        assertThat(Totp.verify(RFC_SECRET, Totp.codeAt(RFC_SECRET, t - 30), t, Long.MIN_VALUE)).hasValue(step - 1);
        assertThat(Totp.verify(RFC_SECRET, Totp.codeAt(RFC_SECRET, t + 30), t, Long.MIN_VALUE)).hasValue(step + 1);
        assertThat(Totp.verify(RFC_SECRET, Totp.codeAt(RFC_SECRET, t + 60), t, Long.MIN_VALUE)).isEmpty();
        assertThat(Totp.verify(RFC_SECRET, Totp.codeAt(RFC_SECRET, t), t, step)).as("replay").isEmpty();
        assertThat(Totp.verify(RFC_SECRET, "12345", t, Long.MIN_VALUE)).isEmpty();
        assertThat(Totp.verify(RFC_SECRET, "abcdef", t, Long.MIN_VALUE)).isEmpty();
        assertThat(Totp.verify(RFC_SECRET, null, t, Long.MIN_VALUE)).isEmpty();
    }

    @Test
    void newSecretsAreBase32AndRoundTrip() {
        String secret = Totp.newSecret();
        assertThat(secret).matches("[A-Z2-7]{32}");
        assertThat(Totp.base32(Totp.base32Decode(secret))).isEqualTo(secret);
        assertThat(Totp.newSecret()).isNotEqualTo(secret);
        assertThatThrownBy(() -> Totp.base32Decode("not base32!")).isInstanceOf(IllegalArgumentException.class);
        assertThat(Totp.otpauthUrl("MySMS", "a b@x", "ABC")).isEqualTo(
                "otpauth://totp/MySMS:a%20b%40x?secret=ABC&issuer=MySMS&algorithm=SHA1&digits=6&period=30");
    }

    @Test
    void recoveryCodesAreSingleUse() {
        List<String> codes = RecoveryCodeSet.generate();
        assertThat(codes).hasSize(10).doesNotHaveDuplicates().allMatch(c -> c.matches("[a-z2-7]{5}-[a-z2-7]{5}"));
        String stored = RecoveryCodeSet.hashes(codes);
        assertThat(stored).doesNotContain(codes.get(0).replace("-", ""));
        String after = RecoveryCodeSet.consume(stored, codes.get(3).toUpperCase(java.util.Locale.ROOT)).orElseThrow();
        assertThat(after.split(",")).hasSize(9);
        assertThat(RecoveryCodeSet.consume(after, codes.get(3))).isEmpty();
        assertThat(RecoveryCodeSet.consume(after, codes.get(4).replace("-", " "))).isPresent();
        assertThat(RecoveryCodeSet.consume(stored, "not-a-code!")).isEmpty();
        assertThat(RecoveryCodeSet.consume(null, codes.get(0))).isEmpty();
        assertThat(RecoveryCodeSet.consume(stored, null)).isEmpty();
    }
}
