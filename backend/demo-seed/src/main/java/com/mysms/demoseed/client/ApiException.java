package com.mysms.demoseed.client;

/** A refused API call: the HTTP status and the problem's stable {@code code}, never the request body. */
public final class ApiException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final int status;
    private final String code;

    public ApiException(String request, int status, String code, String problem) {
        super(request + " answered " + status + " " + code + ": " + problem);
        this.status = status;
        this.code = code;
    }

    public int status() {
        return status;
    }

    public String code() {
        return code;
    }

    /** The operation is in the contract but this release does not serve it. */
    public boolean notImplemented() {
        return status == 501;
    }
}
