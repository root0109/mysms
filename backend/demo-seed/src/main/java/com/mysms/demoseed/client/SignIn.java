package com.mysms.demoseed.client;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

/**
 * A tenant user signed in with password through {@code iam.Login}; an expired access token is replaced by signing in
 * again, so a long seed run outlives the 15-minute token. The password stays in memory only.
 */
public final class SignIn implements Bearer {

    private final ApiClient anonymous;
    private final String tenantCode;
    private final String identifier;
    private final String password;
    private String token;

    private SignIn(ApiClient anonymous, String tenantCode, String identifier, String password) {
        this.anonymous = anonymous;
        this.tenantCode = tenantCode;
        this.identifier = identifier;
        this.password = password;
    }

    /** Signs in now; a refusal (wrong password, unknown or closed tenant) is the {@link ApiException} of iam. */
    public static SignIn of(ApiClient anonymous, String tenantCode, String identifier, String password) {
        SignIn session = new SignIn(anonymous, tenantCode, identifier, password);
        session.renew();
        return session;
    }

    @Override
    public synchronized String token() {
        return token;
    }

    @Override
    public synchronized boolean renew() {
        ObjectNode request = anonymous.object().put("tenantCode", tenantCode).put("identifier", identifier)
                .put("password", password);
        JsonNode result = anonymous.post("/v1/iam/auth:login", request);
        String access = result.path("tokens").path("accessToken").asString(null);
        if (access == null) {
            throw new IllegalStateException("sign-in of " + identifier + " asks for a second factor; demo accounts "
                    + "and the bootstrap administrator must sign in with password only");
        }
        token = access;
        return true;
    }
}
