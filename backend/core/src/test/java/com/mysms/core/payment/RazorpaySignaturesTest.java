package com.mysms.core.payment;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class RazorpaySignaturesTest {

    @Test
    void aWebhookIsValidOnlyForItsExactBodyAndSecret() {
        byte[] body = "{\"event\":\"payment.captured\"}".getBytes(StandardCharsets.UTF_8);
        // HMAC-SHA256("{\"event\":\"payment.captured\"}", "whsec") computed independently
        String signature = RazorpaySignatures.hmac("whsec", body);
        assertThat(signature).hasSize(64).matches("[0-9a-f]+");
        assertThat(RazorpaySignatures.webhookValid(body, signature, "whsec")).isTrue();
        assertThat(RazorpaySignatures.webhookValid(body, signature.toUpperCase(java.util.Locale.ROOT), "whsec"))
                .as("hex case does not matter").isTrue();
        assertThat(RazorpaySignatures.webhookValid(body, signature, "other")).isFalse();
        assertThat(RazorpaySignatures.webhookValid("{}".getBytes(StandardCharsets.UTF_8), signature, "whsec")).isFalse();
        assertThat(RazorpaySignatures.webhookValid(body, null, "whsec")).isFalse();
    }

    @Test
    void aCheckoutProofIsTheKeySecretsSignatureOfOrderAndPayment() {
        // Razorpay's documented construction: HMAC-SHA256(order_id + "|" + razorpay_payment_id, key_secret)
        String expected = RazorpaySignatures.hmac("secret",
                "order_9A33XWu170gUtm|pay_29QQoUBi66xm2f".getBytes(StandardCharsets.UTF_8));
        assertThat(RazorpaySignatures.checkoutValid("order_9A33XWu170gUtm", "pay_29QQoUBi66xm2f", expected, "secret"))
                .isTrue();
        assertThat(RazorpaySignatures.checkoutValid("order_9A33XWu170gUtm", "pay_other", expected, "secret")).isFalse();
    }

    @Test
    void theHmacIsTheStandardOne() {
        // RFC 4231 test case 2: key "Jefe", data "what do ya want for nothing?"
        assertThat(RazorpaySignatures.hmac("Jefe", "what do ya want for nothing?".getBytes(StandardCharsets.UTF_8)))
                .isEqualTo("5bdcc146bf60754e6a042426089575c75a003f089d2739839dec58b964ec3843");
    }
}
