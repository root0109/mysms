package com.mysms.privacy.model;

/**
 * Where a consent decision came from, as consent evidence records it: the client address and the SHA-256 of its
 * User-Agent (the agent itself is never kept). A REST controller reads both from the request; a gRPC call has neither.
 *
 * @param ipAddress     the client's IP address literal, or null
 * @param userAgentHash lowercase hex SHA-256 of the User-Agent header, or null
 */
public record RequestOrigin(String ipAddress, String userAgentHash) {

    /** A call without a client of its own to record (gRPC: another service or a test). */
    public static final RequestOrigin UNKNOWN = new RequestOrigin(null, null);

    /** The origin of a REST request from its remote address and User-Agent header (either may be absent). */
    public static RequestOrigin of(String remoteAddress, String userAgent) {
        return new RequestOrigin(remoteAddress == null || remoteAddress.isBlank() ? null : remoteAddress,
                userAgent == null || userAgent.isBlank() ? null : Sha256.hex(userAgent));
    }
}
