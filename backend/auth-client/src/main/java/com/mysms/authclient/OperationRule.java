package com.mysms.authclient;

import java.util.Set;

/**
 * One operation of a module contract as the edge enforces it (generated into contracts as
 * META-INF/mysms/operations/&lt;module&gt;.json).
 *
 * @param schemes   the security schemes whose tokens may call it
 * @param anonymous   true when the operation may also be called without a token (`security: []` or `{}` in the
 *                    contract)
 * @param idempotency "required" or "optional" when the operation declares the Idempotency-Key header, else null
 */
public record OperationRule(String operationId, String method, String path, String permission, Set<String> schemes,
                            boolean anonymous, String idempotency) {

    public static final String BEARER = "bearerAuth";
    public static final String PLATFORM_ADMIN = "platformAdminAuth";

    public OperationRule {
        schemes = Set.copyOf(schemes);
    }

    public OperationRule(String operationId, String method, String path, String permission, Set<String> schemes,
                         boolean anonymous) {
        this(operationId, method, path, permission, schemes, anonymous, null);
    }

    /** Callable without a token; a token, when sent, is still verified and bound. */
    public boolean isPublic() {
        return anonymous;
    }
}
