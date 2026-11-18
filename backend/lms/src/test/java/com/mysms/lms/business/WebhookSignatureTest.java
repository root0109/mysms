package com.mysms.lms.business;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.mysms.core.error.UnauthenticatedException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HexFormat;
import org.junit.jupiter.api.Test;

/** The MySMS callback signature: HMAC-SHA256 of timestamp.body within five minutes; anything else is 401. */
class WebhookSignatureTest {

    private static final byte[] BODY = "{\"x\":1}".getBytes(StandardCharsets.UTF_8);
    private static final Instant NOW = Instant.ofEpochSecond(1_700_000_000);

    private static String signature(String secret, long seconds) {
        return HexFormat.of().formatHex(WebhookSignature.sign(secret, seconds + ".", BODY));
    }

    @Test
    void aFreshCorrectlySignedCallbackPasses() {
        WebhookSignature.verify("secret", "1700000000", signature("secret", 1_700_000_000), BODY, NOW);
        WebhookSignature.verify("secret", " 1699999800 ", signature("secret", 1_699_999_800), BODY, NOW);
    }

    @Test
    void theWrongSecretBodyOrTimestampIsRefused() {
        assertThatThrownBy(() -> WebhookSignature.verify("other", "1700000000",
                signature("secret", 1_700_000_000), BODY, NOW)).isInstanceOf(UnauthenticatedException.class);
        assertThatThrownBy(() -> WebhookSignature.verify("secret", "1700000001",
                signature("secret", 1_700_000_000), BODY, NOW)).isInstanceOf(UnauthenticatedException.class);
        assertThatThrownBy(() -> WebhookSignature.verify("secret", "not-a-number", "00", BODY, NOW))
                .isInstanceOf(UnauthenticatedException.class);
        assertThatThrownBy(() -> WebhookSignature.verify("secret", "1700000000", null, BODY, NOW))
                .isInstanceOf(UnauthenticatedException.class);
    }

    @Test
    void aReplayOutsideTheToleranceIsRefused() {
        long stale = NOW.getEpochSecond() - WebhookSignature.TOLERANCE.getSeconds() - 1;
        assertThatThrownBy(() -> WebhookSignature.verify("secret", Long.toString(stale), signature("secret", stale),
                BODY, NOW)).isInstanceOf(UnauthenticatedException.class);
        assertThat(WebhookSignature.refused().getMessage()).contains("signature");
    }
}
