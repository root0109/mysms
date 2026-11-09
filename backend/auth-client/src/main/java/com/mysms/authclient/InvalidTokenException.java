package com.mysms.authclient;

/** The bearer token is missing, malformed, badly signed, from another issuer or expired. Answered with 401. */
public final class InvalidTokenException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public static final String TOKEN_EXPIRED = "TOKEN_EXPIRED";
    public static final String UNAUTHENTICATED = "UNAUTHENTICATED";

    private final String code;

    public InvalidTokenException(String code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    public String code() {
        return code;
    }
}
