package com.mysms.privacy.model;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * One entry of the consent evidence ledger (a row of privacy.consent_evidence without its own hash), and the hash that
 * chains it: {@code entry_hash} is the lowercase hex SHA-256 of {@link #canonical()} — every field in a fixed order,
 * separated by {@code |}, absent ones empty, ending with the tenant's previous entry hash. The canonical text depends
 * only on values, not on how PostgreSQL prints them: the time is epoch microseconds and an IP address is in Java's
 * literal form, so an entry read back hashes to the same value.
 *
 * @param id                  the entry's id
 * @param tenantId            the tenant whose chain it belongs to
 * @param consentRecordId     the consent record it is about
 * @param event               recorded, verified, withdrawn, superseded …
 * @param actorUserId         the account that acted, or null
 * @param actorPersonId       the person that acted, or null
 * @param onBehalfOfCapacity  the giver's capacity the step was for, or null
 * @param channel             the channel of the step
 * @param noticeContentSha256 the hash of the notice wording shown, or null
 * @param ipAddress           the client address, or null
 * @param userAgentHash       the hash of the client's User-Agent, or null
 * @param impersonatorUserId  the administrator acting as the user, or null
 * @param occurredAt          when it happened (microsecond precision, as stored)
 * @param previousHash        the tenant's previous entry hash, or null for its first entry
 */
public record EvidenceEntry(UUID id, UUID tenantId, UUID consentRecordId, String event, UUID actorUserId,
                            UUID actorPersonId, String onBehalfOfCapacity, String channel, String noticeContentSha256,
                            String ipAddress, String userAgentHash, UUID impersonatorUserId, OffsetDateTime occurredAt,
                            String previousHash) {

    private static final Pattern IP_LITERAL = Pattern.compile("^[0-9A-Fa-f:.]+$");
    private static final long MICROS_PER_SECOND = 1_000_000L;
    private static final long NANOS_PER_MICRO = 1_000L;

    /** The text the entry hash is computed over. */
    public String canonical() {
        Instant at = occurredAt.toInstant();
        long micros = at.getEpochSecond() * MICROS_PER_SECOND + at.getNano() / NANOS_PER_MICRO;
        return String.join("|", text(id), text(tenantId), text(consentRecordId), text(event), text(actorUserId),
                text(actorPersonId), text(onBehalfOfCapacity), text(channel), text(noticeContentSha256),
                text(literal(ipAddress)), text(userAgentHash), text(impersonatorUserId), Long.toString(micros),
                text(previousHash));
    }

    /** The entry's hash: lowercase hex SHA-256 of its canonical text. */
    public String entryHash() {
        return Sha256.hex(canonical());
    }

    private static String text(Object value) {
        return Objects.toString(value, "");
    }

    /**
     * An IP address in one spelling whatever spelled it ({@code ::1} and {@code 0:0:0:0:0:0:0:1} are one address).
     * Only a literal is parsed, so no name is ever looked up.
     */
    private static String literal(String ipAddress) {
        if (ipAddress == null || !IP_LITERAL.matcher(ipAddress).matches()) {
            return ipAddress;
        }
        try {
            return InetAddress.getByName(ipAddress).getHostAddress();
        } catch (UnknownHostException e) {
            return ipAddress;
        }
    }
}
