package com.mysms.authclient;

import java.util.Set;

/**
 * One operation of a module contract as the edge enforces it, from META-INF/mysms/operations/&lt;module&gt;.json.
 *
 * @param schemes   the security schemes whose tokens may call it
 * @param anonymous   true when the operation may also be called without a token
 * @param idempotency "required" or "optional" when the operation declares the Idempotency-Key header, else null
 * @param audience    who may call it: {@link #PUBLIC} — people (tenant users, or platform
 *                    staff where its schemes say so), never a service token; {@link #PLATFORM} — platform staff only;
 *                    {@link #INTERNAL} — MySMS services only, never a person's token
 * @param ownAuthorization true when the contract gives the operation an {@code Authorization} header of its own (a
 *                    provider's callback signing its body, e.g. LTI 1.1's OAuth 1.0a): the operation verifies that
 *                    header itself, so the edge reads no token from it. Only an anonymous public operation may say so.
 */
public record OperationRule(String operationId, String method, String path, String permission, Set<String> schemes,
                            boolean anonymous, String idempotency, String audience, boolean ownAuthorization) {

    public static final String BEARER = "bearerAuth";
    public static final String PLATFORM_ADMIN = "platformAdminAuth";
    public static final String PUBLIC = "public";
    public static final String INTERNAL = "internal";
    public static final String PLATFORM = "platform";
    private static final Set<String> AUDIENCES = Set.of(PUBLIC, INTERNAL, PLATFORM);

    public OperationRule {
        schemes = Set.copyOf(schemes);
        audience = audience == null ? PUBLIC : audience;
        if (!AUDIENCES.contains(audience)) {
            throw new IllegalArgumentException("operation " + operationId + ": unknown audience " + audience);
        }
        if (ownAuthorization && (!anonymous || !PUBLIC.equals(audience))) {
            throw new IllegalArgumentException("operation " + operationId
                    + ": only an anonymous public operation verifies its own Authorization header");
        }
    }

    public OperationRule(String operationId, String method, String path, String permission, Set<String> schemes,
                         boolean anonymous, String idempotency, String audience) {
        this(operationId, method, path, permission, schemes, anonymous, idempotency, audience, false);
    }

    public OperationRule(String operationId, String method, String path, String permission, Set<String> schemes,
                         boolean anonymous, String idempotency) {
        this(operationId, method, path, permission, schemes, anonymous, idempotency, PUBLIC, false);
    }

    public OperationRule(String operationId, String method, String path, String permission, Set<String> schemes,
                         boolean anonymous) {
        this(operationId, method, path, permission, schemes, anonymous, null, PUBLIC, false);
    }

    /** Callable by MySMS services only. */
    public boolean isInternal() {
        return INTERNAL.equals(audience);
    }

    /** Callable without a token; a token, when sent, is still verified and bound. */
    public boolean isPublic() {
        return anonymous;
    }
}
