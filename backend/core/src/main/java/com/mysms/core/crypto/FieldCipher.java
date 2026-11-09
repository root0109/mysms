package com.mysms.core.crypto;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Application-level encryption of the `*_enc` columns (database/README "Secrets"): the envelope
 * {@code v1.<keyId>.<iv>.<ciphertext>} with AES-256-GCM.
 *
 * <p>Each tenant gets its own data key, derived with HKDF-SHA256 from a master key and the tenant id, so a value
 * can only be opened for the tenant it was written for. The associated data binds each value to its tenant and
 * column, so a ciphertext copied to another row's column or another tenant fails to open. Master keys come only
 * from the environment ({@code mysms.secrets.keys}, never the database); several may be configured so values
 * written under an older key stay readable while {@code mysms.secrets.active-key} names the key new values use.
 * Plaintext is never logged and never part of an exception message.
 */
public final class FieldCipher {

    private static final String VERSION = "v1";
    private static final int IV_BYTES = 12;
    private static final int TAG_BITS = 128;
    private static final int KEY_BYTES = 32;
    private static final byte[] SALT = "mysms-field-encryption-v1".getBytes(StandardCharsets.UTF_8);
    private static final Base64.Encoder B64 = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder B64D = Base64.getUrlDecoder();

    private final Map<String, byte[]> masterKeys;
    private final String activeKey;
    private final SecureRandom random = new SecureRandom();

    private FieldCipher(Map<String, byte[]> masterKeys, String activeKey) {
        this.masterKeys = masterKeys;
        this.activeKey = activeKey;
    }

    /**
     * From the settings: {@code keys} is a comma-separated list of {@code <keyId>:<base64 32-byte key>}; blank keys
     * give a cipher that refuses to work (modules with secret columns check {@link #requireConfigured()} at start).
     *
     * @throws IllegalArgumentException for a malformed key list, a key that is not 32 bytes, or an active key that
     *                                  is not in the list
     */
    public static FieldCipher fromSettings(String keys, String activeKey) {
        Map<String, byte[]> parsed = new LinkedHashMap<>();
        if (keys != null && !keys.isBlank()) {
            for (String entry : keys.split(",")) {
                String trimmed = entry.trim();
                int colon = trimmed.indexOf(':');
                if (colon <= 0) {
                    throw new IllegalArgumentException("mysms.secrets.keys: each entry is <keyId>:<base64 key>");
                }
                String id = trimmed.substring(0, colon);
                if (!id.matches("[A-Za-z0-9_-]{1,32}")) {
                    throw new IllegalArgumentException("mysms.secrets.keys: key ids use letters, digits, - and _");
                }
                byte[] key;
                try {
                    key = Base64.getDecoder().decode(trimmed.substring(colon + 1).trim());
                } catch (IllegalArgumentException e) {
                    throw new IllegalArgumentException("mysms.secrets.keys: key " + id + " is not base64", e);
                }
                if (key.length != KEY_BYTES) {
                    throw new IllegalArgumentException("mysms.secrets.keys: key " + id + " must be 32 bytes");
                }
                if (parsed.put(id, key) != null) {
                    throw new IllegalArgumentException("mysms.secrets.keys: key " + id + " is listed twice");
                }
            }
        }
        String active = activeKey == null || activeKey.isBlank()
                ? parsed.keySet().stream().reduce((first, second) -> second).orElse(null) : activeKey.trim();
        if (active != null && !parsed.containsKey(active)) {
            throw new IllegalArgumentException("mysms.secrets.active-key " + active + " is not in mysms.secrets.keys");
        }
        return new FieldCipher(Map.copyOf(parsed), active);
    }

    /** Fails fast when no master key is configured; called by every service that stores secret columns. */
    public void requireConfigured() {
        if (activeKey == null) {
            throw new IllegalStateException("mysms.secrets.keys (MYSMS_SECRETS_KEYS) is not configured");
        }
    }

    /** The envelope of a value for one tenant and column, or null for null. */
    public String encrypt(String plaintext, UUID tenantId, String column) {
        if (plaintext == null) {
            return null;
        }
        requireConfigured();
        byte[] iv = new byte[IV_BYTES];
        random.nextBytes(iv);
        try {
            Cipher cipher = cipher(Cipher.ENCRYPT_MODE, activeKey, tenantId, iv);
            cipher.updateAAD(aad(tenantId, column));
            byte[] sealed = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            return VERSION + "." + activeKey + "." + B64.encodeToString(iv) + "." + B64.encodeToString(sealed);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("field encryption failed", e);
        }
    }

    /**
     * The plaintext of an envelope written for this tenant and column; empty for null. A value that does not open
     * (other tenant, other column, altered, unknown key) is an error, never silently returned.
     */
    public Optional<String> decrypt(String envelope, UUID tenantId, String column) {
        if (envelope == null) {
            return Optional.empty();
        }
        String[] parts = envelope.split("\\.");
        if (parts.length != 4 || !VERSION.equals(parts[0]) || !masterKeys.containsKey(parts[1])) {
            throw new IllegalStateException("unreadable secret envelope in " + column);
        }
        try {
            byte[] iv = B64D.decode(parts[2]);
            if (iv.length != IV_BYTES) {
                throw new IllegalStateException("unreadable secret envelope in " + column);
            }
            Cipher cipher = cipher(Cipher.DECRYPT_MODE, parts[1], tenantId, iv);
            cipher.updateAAD(aad(tenantId, column));
            return Optional.of(new String(cipher.doFinal(B64D.decode(parts[3])), StandardCharsets.UTF_8));
        } catch (GeneralSecurityException | IllegalArgumentException e) {
            throw new IllegalStateException("secret in " + column + " does not open for this tenant", e);
        }
    }

    /** The display form of a secret: "••••" and its last four characters (just "••••" for eight or fewer). */
    public static String mask(String plaintext) {
        if (plaintext == null) {
            return null;
        }
        int length = plaintext.codePointCount(0, plaintext.length());
        if (length <= 8) {
            return "••••";
        }
        return "••••" + plaintext.substring(plaintext.offsetByCodePoints(0, length - 4));
    }

    /** The masked display form of a stored envelope (decrypts in memory only). */
    public String maskStored(String envelope, UUID tenantId, String column) {
        return decrypt(envelope, tenantId, column).map(FieldCipher::mask).orElse(null);
    }

    private Cipher cipher(int mode, String keyId, UUID tenantId, byte[] iv) throws GeneralSecurityException {
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(mode, new SecretKeySpec(tenantKey(keyId, tenantId), "AES"), new GCMParameterSpec(TAG_BITS, iv));
        return cipher;
    }

    /** HKDF-SHA256 (RFC 5869) of the master key with the tenant id as context: one 32-byte key per tenant. */
    private byte[] tenantKey(String keyId, UUID tenantId) throws GeneralSecurityException {
        Mac extract = Mac.getInstance("HmacSHA256");
        extract.init(new SecretKeySpec(SALT, "HmacSHA256"));
        byte[] prk = extract.doFinal(masterKeys.get(keyId));
        Mac expand = Mac.getInstance("HmacSHA256");
        expand.init(new SecretKeySpec(prk, "HmacSHA256"));
        expand.update(("tenant:" + tenantId).getBytes(StandardCharsets.UTF_8));
        expand.update((byte) 1);
        return expand.doFinal();
    }

    private static byte[] aad(UUID tenantId, String column) {
        return (tenantId + "|" + column).getBytes(StandardCharsets.UTF_8);
    }
}
