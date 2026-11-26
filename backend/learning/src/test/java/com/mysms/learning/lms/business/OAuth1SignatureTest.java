package com.mysms.learning.lms.business;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** OAuth 1.0a HMAC-SHA1 signing as LTI 1.1 uses it: sign and verify agree, headers parse, the body hash is SHA-1. */
class OAuth1SignatureTest {

    @Test
    void aSignedParameterSetVerifiesAndTamperingBreaksIt() {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("oauth_consumer_key", "key");
        params.put("oauth_signature_method", "HMAC-SHA1");
        params.put("oauth_timestamp", "1700000000");
        params.put("oauth_nonce", "abc123");
        params.put("user_id", "u-1");
        params.put("oauth_signature", OAuth1Signature.sign("POST", "https://tool.example.com/launch", params,
                "secret"));
        assertThat(OAuth1Signature.verify("POST", "https://tool.example.com/launch", params, "secret")).isTrue();
        assertThat(OAuth1Signature.verify("POST", "https://tool.example.com/launch", params, "other")).isFalse();
        params.put("user_id", "u-2");
        assertThat(OAuth1Signature.verify("POST", "https://tool.example.com/launch", params, "secret")).isFalse();
    }

    @Test
    void theUrlIsNormalisedAndTheQueryIgnored() {
        Map<String, String> params = new LinkedHashMap<>(Map.of("oauth_nonce", "n"));
        String plain = OAuth1Signature.sign("post", "HTTPS://Tool.Example.com:443/launch", params, "s");
        assertThat(OAuth1Signature.sign("POST", "https://tool.example.com/launch", params, "s")).isEqualTo(plain);
        assertThat(OAuth1Signature.sign("POST", "https://tool.example.com:8443/launch", params, "s"))
                .isNotEqualTo(plain);
    }

    @Test
    void readsAnOAuthHeaderAndIgnoresTheRealm() {
        Map<String, String> read = OAuth1Signature.header(
                "OAuth realm=\"x\", oauth_consumer_key=\"key\", oauth_body_hash=\"a%2Bb%3D\", oauth_timestamp=\"17\"");
        assertThat(read).containsEntry("oauth_consumer_key", "key").containsEntry("oauth_body_hash", "a+b=")
                .containsEntry("oauth_timestamp", "17").doesNotContainKey("realm");
        assertThat(OAuth1Signature.header("Bearer abc")).isEmpty();
        assertThat(OAuth1Signature.header(null)).isEmpty();
    }

    @Test
    void theBodyHashIsBase64OfSha1() {
        assertThat(OAuth1Signature.bodyHash(new byte[0])).isEqualTo("2jmj7l5rSw0yVb/vlWAYkK/YBwk=");
        assertThat(OAuth1Signature.bodyHash("hello".getBytes(StandardCharsets.UTF_8)))
                .isEqualTo("qvTGHdzF6KLavt4PO0gs2a6pQ00=");
    }

    @Test
    void encodesTheRfc5849Way() {
        assertThat(OAuth1Signature.encode("a b+c~d-e_f.g/h")).isEqualTo("a%20b%2Bc~d-e_f.g%2Fh");
    }
}
