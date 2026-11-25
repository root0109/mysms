package com.mysms.demoseed.client;

/** Where an {@link ApiClient} gets its access token, and how it gets a fresh one when the token has expired. */
public interface Bearer {

    String token();

    /** Replaces an expired token; false when this bearer cannot (a token handed in from outside). */
    boolean renew();

    /** A token handed in as it is (the platform operator's): it cannot be renewed. */
    static Bearer fixed(String token) {
        return new Bearer() {
            @Override
            public String token() {
                return token;
            }

            @Override
            public boolean renew() {
                return false;
            }
        };
    }
}
