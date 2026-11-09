package com.mysms.core.crypto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Base64;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class FieldCipherTest {

    private static final String K1 = Base64.getEncoder().encodeToString(new byte[32]);
    private static final String K2 = Base64.getEncoder().encodeToString(filled(7));
    private static final UUID TENANT = UUID.fromString("01960000-0000-7000-8000-000000000001");
    private static final UUID OTHER = UUID.fromString("01960000-0000-7000-8000-000000000002");
    private static final String COLUMN = "people.person.national_id_enc";

    private static byte[] filled(int value) {
        byte[] out = new byte[32];
        java.util.Arrays.fill(out, (byte) value);
        return out;
    }

    @Test
    void roundTripsUnderTheActiveKeyWithAFreshIvEachTime() {
        FieldCipher cipher = FieldCipher.fromSettings("k1:" + K1, "k1");
        String a = cipher.encrypt("1234-5678-9012", TENANT, COLUMN);
        String b = cipher.encrypt("1234-5678-9012", TENANT, COLUMN);
        assertThat(a).startsWith("v1.k1.").doesNotContain("1234").isNotEqualTo(b);
        assertThat(a.split("\\.")).hasSize(4);
        assertThat(cipher.decrypt(a, TENANT, COLUMN)).contains("1234-5678-9012");
        assertThat(cipher.encrypt(null, TENANT, COLUMN)).isNull();
        assertThat(cipher.decrypt(null, TENANT, COLUMN)).isEmpty();
    }

    @Test
    void aValueOpensOnlyForItsTenantAndColumn() {
        FieldCipher cipher = FieldCipher.fromSettings("k1:" + K1, null);
        String sealed = cipher.encrypt("secret-value-1", TENANT, COLUMN);
        assertThatThrownBy(() -> cipher.decrypt(sealed, OTHER, COLUMN)).isInstanceOf(IllegalStateException.class)
                .hasMessageNotContaining("secret-value-1");
        assertThatThrownBy(() -> cipher.decrypt(sealed, TENANT, "admission.application.national_id_enc"))
                .isInstanceOf(IllegalStateException.class);
        String[] parts = sealed.split("\\.");
        String altered = parts[0] + "." + parts[1] + "." + parts[2] + "." + parts[3].substring(1) + "A";
        assertThatThrownBy(() -> cipher.decrypt(altered, TENANT, COLUMN)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> cipher.decrypt("plain", TENANT, COLUMN)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> cipher.decrypt("v1.k9.AAAA.AAAA", TENANT, COLUMN))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void rotationKeepsOldValuesReadable() {
        String old = FieldCipher.fromSettings("k1:" + K1, "k1").encrypt("rotate-me-please", TENANT, COLUMN);
        FieldCipher rotated = FieldCipher.fromSettings("k1:" + K1 + ", k2:" + K2, "k2");
        assertThat(rotated.decrypt(old, TENANT, COLUMN)).contains("rotate-me-please");
        assertThat(rotated.encrypt("new", TENANT, COLUMN)).startsWith("v1.k2.");
        // the last listed key is active when none is named
        assertThat(FieldCipher.fromSettings("k1:" + K1 + ",k2:" + K2, " ").encrypt("x", TENANT, COLUMN))
                .startsWith("v1.k2.");
    }

    @Test
    void refusesMissingOrMalformedKeys() {
        FieldCipher none = FieldCipher.fromSettings("", null);
        assertThatThrownBy(none::requireConfigured).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("MYSMS_SECRETS_KEYS");
        assertThatThrownBy(() -> none.encrypt("x", TENANT, COLUMN)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> FieldCipher.fromSettings("k1", null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> FieldCipher.fromSettings("k1:" + Base64.getEncoder().encodeToString(new byte[16]),
                null)).hasMessageContaining("32 bytes");
        assertThatThrownBy(() -> FieldCipher.fromSettings("k1:%%%", null)).hasMessageContaining("base64");
        assertThatThrownBy(() -> FieldCipher.fromSettings("k1:" + K1 + ",k1:" + K2, null))
                .hasMessageContaining("twice");
        assertThatThrownBy(() -> FieldCipher.fromSettings("k 1:" + K1, null)).hasMessageContaining("key ids");
        assertThatThrownBy(() -> FieldCipher.fromSettings("k1:" + K1, "k2")).hasMessageContaining("not in");
    }

    @Test
    void masksAllButTheLastFourOfLongValues() {
        assertThat(FieldCipher.mask("123456789012")).isEqualTo("••••9012");
        assertThat(FieldCipher.mask("12345678")).isEqualTo("••••");
        assertThat(FieldCipher.mask(null)).isNull();
        FieldCipher cipher = FieldCipher.fromSettings("k1:" + K1, null);
        assertThat(cipher.maskStored(cipher.encrypt("ACCT-00001234", TENANT, COLUMN), TENANT, COLUMN))
                .isEqualTo("••••1234");
        assertThat(cipher.maskStored(null, TENANT, COLUMN)).isNull();
    }
}
