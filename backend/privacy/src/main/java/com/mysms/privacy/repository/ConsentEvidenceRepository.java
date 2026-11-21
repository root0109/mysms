package com.mysms.privacy.repository;

import com.mysms.core.db.ModuleDatabase;
import com.mysms.privacy.config.PrivacyDatabaseConfig;
import com.mysms.privacy.model.EvidenceEntry;
import java.time.OffsetDateTime;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * The consent evidence ledger: privacy.consent_evidence, which this class only ever inserts into (append-only by
 * code: there is no update or delete statement), and the head of each tenant's hash chain, privacy.evidence_chain,
 * whose row is locked for the rest of the writing transaction so a tenant's entries are appended one at a time.
 *
 * <p>Append-only by privilege as well — the application role holding INSERT and SELECT only on the ledger — needs a
 * change to core's role grants that this module does not make (ROADMAP SEC-6); until then the hash chain is what makes
 * an altered or removed entry visible.
 */
@Repository
public class ConsentEvidenceRepository {

    /** The head of a tenant's chain: the latest entry's hash and time; both null before the first entry. */
    public record ChainHead(String lastEntryHash, OffsetDateTime lastOccurredAt) {
    }

    /** A tenant's first writer creates the head row; a concurrent one waits on it and then does nothing. */
    private static final String ENSURE_CHAIN = """
            INSERT INTO privacy.evidence_chain (tenant_id) VALUES (:tenantId)
            ON CONFLICT (tenant_id) DO NOTHING
            """;
    private static final String LOCK_CHAIN = """
            SELECT c.last_entry_hash, c.last_occurred_at
              FROM privacy.evidence_chain c
             WHERE c.tenant_id = :tenantId
               FOR UPDATE
            """;
    private static final String ADVANCE_CHAIN = """
            UPDATE privacy.evidence_chain
               SET last_entry_hash = :entryHash,
                   last_occurred_at = :occurredAt,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
            """;
    private static final String APPEND = """
            INSERT INTO privacy.consent_evidence
            (id, tenant_id, consent_record_id, event, actor_user_id, actor_person_id, on_behalf_of_capacity, channel,
             notice_content_sha256, ip_address, user_agent_hash, impersonator_user_id, occurred_at, previous_hash,
             entry_hash)
            VALUES (:id, :tenantId, :consentRecordId, :event, :actorUserId, :actorPersonId, :onBehalfOfCapacity,
                    :channel, :noticeContentSha256, CAST(:ipAddress AS inet), :userAgentHash, :impersonatorUserId,
                    :occurredAt, :previousHash, :entryHash)
            """;

    private final JdbcClient jdbc;

    public ConsentEvidenceRepository(@Qualifier(PrivacyDatabaseConfig.DATABASE) ModuleDatabase database) {
        this.jdbc = database.jdbc();
    }

    /** The head of the caller's tenant's chain, its row locked until the transaction ends. */
    public ChainHead lockChain() {
        jdbc.sql(ENSURE_CHAIN).update();
        return jdbc.sql(LOCK_CHAIN).query((rs, row) -> new ChainHead(rs.getString("last_entry_hash"),
                rs.getObject("last_occurred_at", OffsetDateTime.class))).single();
    }

    /** Appends the entry with its hash and moves the chain's head to it (the chain must be locked). */
    public void append(EvidenceEntry entry, String entryHash) {
        jdbc.sql(APPEND)
                .param("id", entry.id())
                .param("tenantId", entry.tenantId())
                .param("consentRecordId", entry.consentRecordId())
                .param("event", entry.event())
                .param("actorUserId", entry.actorUserId())
                .param("actorPersonId", entry.actorPersonId())
                .param("onBehalfOfCapacity", entry.onBehalfOfCapacity())
                .param("channel", entry.channel())
                .param("noticeContentSha256", entry.noticeContentSha256())
                .param("ipAddress", entry.ipAddress())
                .param("userAgentHash", entry.userAgentHash())
                .param("impersonatorUserId", entry.impersonatorUserId())
                .param("occurredAt", entry.occurredAt())
                .param("previousHash", entry.previousHash())
                .param("entryHash", entryHash)
                .update();
        jdbc.sql(ADVANCE_CHAIN).param("entryHash", entryHash).param("occurredAt", entry.occurredAt()).update();
    }
}
