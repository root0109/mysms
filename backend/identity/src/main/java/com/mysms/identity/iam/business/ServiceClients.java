package com.mysms.identity.iam.business;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * The configured service clients iam issues client-credentials tokens to, one per module. iam holds only the SHA-256
 * of each secret, compares in constant time, and grants only the configured permissions. Secrets are long random
 * strings, so a plain SHA-256 is the right hash, as for refresh tokens.
 */
public final class ServiceClients {

    private static final Pattern MODULE = Pattern.compile("[a-z]+");
    private static final Pattern SHA256_HEX = Pattern.compile("[0-9a-f]{64}");

    /**
     * One configured client.
     *
     * @param secretSha256 the lower-case hex SHA-256 of the client's secret
     * @param permissions  the permission codes its tokens carry (operations of audience internal it may call)
     */
    public record Client(String secretSha256, List<String> permissions) {

        public Client {
            permissions = permissions == null ? List.of() : List.copyOf(permissions);
        }
    }

    private final Map<String, Client> clients;
    private final Duration tokenTtl;

    /**
     * Validates the configuration; anything malformed stops the service from starting.
     *
     * @param tokenTtl the lifetime of a service token (short: the provider fetches a new one before it expires)
     */
    public ServiceClients(Map<String, Client> clients, Duration tokenTtl) {
        clients.forEach((module, client) -> {
            if (!MODULE.matcher(module).matches()) {
                throw new IllegalStateException("mysms.iam.service-clients: '" + module + "' is not a module name");
            }
            if (client.secretSha256() == null || !SHA256_HEX.matcher(client.secretSha256()).matches()) {
                throw new IllegalStateException("mysms.iam.service-clients." + module
                        + ".secret-sha256 must be the lower-case hex SHA-256 of the client secret");
            }
        });
        if (tokenTtl.isNegative() || tokenTtl.isZero() || tokenTtl.compareTo(Duration.ofHours(1)) > 0) {
            throw new IllegalStateException("mysms.iam.token.service-token-ttl must be positive and at most one hour");
        }
        this.clients = Map.copyOf(clients);
        this.tokenTtl = tokenTtl;
    }

    /** The client these credentials authenticate, or empty (unknown client and wrong secret alike). */
    public Optional<Client> authenticate(String clientId, String secret) {
        if (clientId == null || secret == null) {
            return Optional.empty();
        }
        Client client = clients.get(clientId);
        byte[] given = sha256(secret);
        // the digest is computed and compared whether or not the client exists: no timing difference
        byte[] expected = client == null ? new byte[given.length] : HexFormat.of().parseHex(client.secretSha256());
        return MessageDigest.isEqual(given, expected) && client != null ? Optional.of(client) : Optional.empty();
    }

    public Duration tokenTtl() {
        return tokenTtl;
    }

    private static byte[] sha256(String secret) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(secret.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }
}
