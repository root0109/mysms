package com.mysms.privacy.business;

import com.mysms.core.id.UuidV7;
import com.mysms.privacy.model.EvidenceEntry;
import com.mysms.privacy.repository.ConsentEvidenceRepository;
import com.mysms.privacy.repository.ConsentEvidenceRepository.ChainHead;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Appends to the consent evidence ledger (privacy.consent_evidence) inside the caller's write transaction: locks the
 * tenant's chain head, gives the entry a time strictly after the previous entry's (so time orders the chain), names the
 * previous entry's hash, hashes the entry ({@link EvidenceEntry#entryHash()}) and moves the head to it. The lock is
 * held until the transaction ends, so a tenant's entries are appended one at a time; callers lock the subject first.
 */
@Component
public class ConsentEvidenceLedger {

    /** What one step of a consent record leaves in the ledger, before the ledger places it in the chain. */
    public record Step(UUID tenantId, UUID consentRecordId, String event, UUID actorUserId, UUID actorPersonId,
                       String onBehalfOfCapacity, String channel, String noticeContentSha256, String ipAddress,
                       String userAgentHash, UUID impersonatorUserId) {
    }

    private final ConsentEvidenceRepository evidence;
    private final Clock clock;

    public ConsentEvidenceLedger(ConsentEvidenceRepository evidence, Clock clock) {
        this.evidence = evidence;
        this.clock = clock;
    }

    /** Appends the step as the tenant's next entry and answers the entry as stored. */
    public EvidenceEntry append(Step step) {
        ChainHead head = evidence.lockChain();
        OffsetDateTime now = OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC)
                .truncatedTo(ChronoUnit.MICROS);
        OffsetDateTime last = head.lastOccurredAt();
        OffsetDateTime occurredAt = last != null && !now.isAfter(last)
                ? last.withOffsetSameInstant(ZoneOffset.UTC).plus(1, ChronoUnit.MICROS) : now;
        EvidenceEntry entry = new EvidenceEntry(UuidV7.next(clock), step.tenantId(), step.consentRecordId(),
                step.event(), step.actorUserId(), step.actorPersonId(), step.onBehalfOfCapacity(), step.channel(),
                step.noticeContentSha256(), step.ipAddress(), step.userAgentHash(), step.impersonatorUserId(),
                occurredAt, head.lastEntryHash());
        evidence.append(entry, entry.entryHash());
        return entry;
    }
}
