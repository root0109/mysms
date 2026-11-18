package com.mysms.finance.repository;

import com.mysms.core.db.ModuleDatabase;
import com.mysms.finance.config.FinanceDatabaseConfig;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * What the payment workflows read and write about the gateway: the merchant account a payment is taken on (secrets as
 * stored, encrypted) and the gateway's notifications (finance.gateway_event, kept once per gateway event id for
 * idempotent processing and as dispute evidence). Constant SQL, the tenant filtered on every statement.
 */
@Repository
public class GatewayRepository {

    /** A merchant account; the secrets stay encrypted envelopes until the service opens them. */
    public record AccountRow(UUID id, UUID institutionId, String gatewayName, String keyId, String keySecretEnc,
            String webhookSecretEnc, boolean active) {
    }

    /** A notification as kept: whether it was handled, and why not when it was refused. */
    public record EventRow(UUID id, OffsetDateTime processedAt, String processingError) {
    }

    private static final String ACCOUNT_COLUMNS = """
            SELECT a.id, a.institution_id, a.gateway_name, a.key_id, a.key_secret_enc, a.webhook_secret_enc,
                   a.is_active
              FROM finance.payment_gateway_account a
             WHERE a.tenant_id = :tenantId
               AND a.deleted_at IS NULL
            """;
    private static final String ACCOUNT = ACCOUNT_COLUMNS + " AND a.id = :id";
    /** The account an institution takes money on: its own before the tenant's, the default one first. */
    private static final String ACCOUNT_FOR = ACCOUNT_COLUMNS + """
               AND a.is_active AND (a.institution_id = :institutionId OR a.institution_id IS NULL)
             ORDER BY a.institution_id IS NULL, a.is_default DESC, a.created_at, a.id
             LIMIT 1
            """;
    private static final String CLAIM_EVENT = """
            INSERT INTO finance.gateway_event
            (id, tenant_id, gateway_account_id, gateway_name, gateway_event_id, event_type, raw_payload, received_at,
             created_at, updated_at, row_version)
            VALUES (:id, :tenantId, :accountId, :gatewayName, :eventId, :eventType, :payload, :now, :now, :now, 1)
            ON CONFLICT (tenant_id, gateway_name, gateway_event_id) WHERE gateway_event_id IS NOT NULL DO NOTHING
            """;
    private static final String LOCK_EVENT = """
            SELECT e.id, e.processed_at, e.processing_error
              FROM finance.gateway_event e
             WHERE e.tenant_id = :tenantId
               AND e.gateway_name = :gatewayName AND e.gateway_event_id = :eventId
               FOR UPDATE
            """;
    private static final String FINISH_EVENT = """
            UPDATE finance.gateway_event
               SET payment_id = :paymentId, processed_at = :processedAt, processing_error = :error,
                   updated_at = :now, row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id
            """;

    private final JdbcClient jdbc;

    public GatewayRepository(@Qualifier(FinanceDatabaseConfig.DATABASE) ModuleDatabase database) {
        this.jdbc = database.jdbc();
    }

    public Optional<AccountRow> account(UUID id) {
        return jdbc.sql(ACCOUNT).param("id", id).query((rs, row) -> account(rs)).optional();
    }

    /** The active account money for the institution is taken on (the tenant's when it has none of its own). */
    public Optional<AccountRow> accountFor(UUID institutionId) {
        return jdbc.sql(ACCOUNT_FOR).param("institutionId", institutionId).query((rs, row) -> account(rs)).optional();
    }

    /**
     * Keeps the notification once per gateway event id and locks it for this transaction; answers it as kept (the
     * earlier delivery's row on a redelivery).
     */
    public EventRow claimEvent(UUID newId, UUID accountId, String gatewayName, String eventId, String eventType,
            String payload, OffsetDateTime now) {
        jdbc.sql(CLAIM_EVENT).param("id", newId).param("accountId", accountId).param("gatewayName", gatewayName)
                .param("eventId", eventId).param("eventType", eventType).param("payload", payload).param("now", now)
                .update();
        return jdbc.sql(LOCK_EVENT).param("gatewayName", gatewayName).param("eventId", eventId)
                .query((rs, row) -> new EventRow(rs.getObject("id", UUID.class),
                        rs.getObject("processed_at", OffsetDateTime.class), rs.getString("processing_error")))
                .single();
    }

    /** Records how the notification was handled: the payment it concerned, and the refusal reason if any. */
    public void finishEvent(UUID id, UUID paymentId, String error, OffsetDateTime now) {
        jdbc.sql(FINISH_EVENT).param("id", id).param("paymentId", paymentId)
                .param("processedAt", error == null ? now : null).param("error", error).param("now", now).update();
    }

    private static AccountRow account(java.sql.ResultSet rs) throws java.sql.SQLException {
        return new AccountRow(rs.getObject("id", UUID.class), rs.getObject("institution_id", UUID.class),
                rs.getString("gateway_name"), rs.getString("key_id"), rs.getString("key_secret_enc"),
                rs.getString("webhook_secret_enc"), rs.getBoolean("is_active"));
    }
}
