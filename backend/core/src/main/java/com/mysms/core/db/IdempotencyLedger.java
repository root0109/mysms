package com.mysms.core.db;

import com.mysms.core.crypto.FieldCipher;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.json.JsonMapper;

/**
 * Idempotency rules shared by REST's Idempotency-Key and gRPC's {@code request_id}, so a key behaves the same either
 * way: retention, stale-lock takeover and encryption of kept responses (Facade over {@link IdempotencyStore}).
 */
public final class IdempotencyLedger {

    /** A kept response is replayed for this long. */
    public static final Duration RETENTION = Duration.ofHours(24);
    /** A claim whose request has been running longer than this is taken over by a retry. */
    public static final Duration STALE_LOCK = Duration.ofMinutes(2);

    private static final Logger LOG = LoggerFactory.getLogger(IdempotencyLedger.class);

    private final Map<String, IdempotencyStore> stores;
    private final FieldCipher cipher;
    private final JsonMapper json;
    private final Clock clock;

    public IdempotencyLedger(List<IdempotencyStore> stores, FieldCipher cipher, JsonMapper json, Clock clock) {
        Map<String, IdempotencyStore> byModule = new LinkedHashMap<>();
        stores.forEach(s -> byModule.put(s.module(), s));
        this.stores = Map.copyOf(byModule);
        this.cipher = cipher;
        this.json = json;
        this.clock = clock;
    }

    /**
     * One claimed key: which store, operation, tenant and user it belongs to, and the store's decision.
     *
     * @param claim the outcome and, for a replay, the kept (still encrypted) response
     */
    public record Ticket(IdempotencyStore store, String operation, UUID key, UUID tenant, UUID user,
                         IdempotencyStore.Claim claim) {

        public IdempotencyStore.Outcome outcome() {
            return claim.outcome();
        }
    }

    /** The store of the module that owns the operation ({@code <module>.<Name>}); empty for master, which has none. */
    public Optional<IdempotencyStore> storeFor(String operationId) {
        int dot = operationId == null ? -1 : operationId.indexOf('.');
        return dot <= 0 ? Optional.empty() : Optional.ofNullable(stores.get(operationId.substring(0, dot)));
    }

    /** Claims the key for this request (its own short transaction). */
    public Ticket claim(IdempotencyStore store, String operation, UUID key, UUID tenant, UUID user, String hash) {
        OffsetDateTime now = now();
        IdempotencyStore.Claim claim = store.claim(key, user, operation, hash, now, now.plus(RETENTION),
                now.minus(STALE_LOCK));
        return new Ticket(store, operation, key, tenant, user, claim);
    }

    /**
     * Keeps the response of a claimed key, encrypted for the tenant. The request has run (its own transaction may have
     * committed), so a failure here never frees the key: it is tried once more, then the lock is left in place and the
     * key answers "in progress" until it goes stale.
     *
     * @param status the HTTP status of the answer (gRPC: its HTTP equivalent)
     * @param kept   what a replay needs (headers and body, or the gRPC status and message)
     */
    public void complete(Ticket ticket, int status, Map<String, Object> kept) {
        String sealed = cipher.encrypt(json.writeValueAsString(kept), ticket.tenant(), column(ticket));
        for (int attempt = 1; ; attempt++) {
            try {
                ticket.store().complete(ticket.claim().id(), status, sealed, now());
                return;
            } catch (RuntimeException e) {
                if (attempt == 2) {
                    LOG.error("idempotent response of {} not kept; the key stays locked", ticket.operation(), e);
                    return;
                }
            }
        }
    }

    /** Frees a claimed key whose request failed on the server, so a retry runs it again. */
    public void release(Ticket ticket) {
        try {
            ticket.store().release(ticket.claim().id(), now());
        } catch (RuntimeException e) {
            LOG.error("idempotency key of {} not released; it frees itself when the lock goes stale",
                    ticket.operation(), e);
        }
    }

    /** The kept response of a {@link IdempotencyStore.Outcome#REPLAY} ticket, decrypted. */
    @SuppressWarnings("unchecked")
    public Map<String, Object> kept(Ticket ticket) {
        return json.readValue(cipher.decrypt(ticket.claim().storedBody(), ticket.tenant(), column(ticket))
                .orElse("{}"), Map.class);
    }

    /** SHA-256 over the request's identifying parts, hex: two requests are "the same" when their hashes are equal. */
    public static String hash(String head, byte[] body) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(head.getBytes(StandardCharsets.UTF_8));
            digest.update(body);
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }

    private static String column(Ticket ticket) {
        return ticket.store().module() + ".idempotency_key.response_body";
    }

    private OffsetDateTime now() {
        return OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC).truncatedTo(ChronoUnit.MICROS);
    }
}
