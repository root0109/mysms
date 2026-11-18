package com.mysms.finance.repository;

import com.mysms.core.db.ModuleDatabase;
import com.mysms.finance.config.FinanceDatabaseConfig;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * The document numbers finance issues from its own finance.number_sequence (invoice and receipt numbers): one series
 * per institution (or the tenant, for tenant-level documents) and code, opened on first use. A number is taken under
 * the series row's lock, held until the issuing transaction ends, so two transactions never get the same number and a
 * rollback gives it back.
 */
@Repository
public class NumberSequenceRepository {

    private static final String OPEN = """
            INSERT INTO finance.number_sequence
            (id, tenant_id, institution_id, sequence_code, period_key, prefix, padding, next_value, created_at,
             created_by, updated_at, updated_by, row_version)
            VALUES (:id, :tenantId, :institutionId, :sequenceCode, '', :prefix, 6, 1, :now, :actor, :now, :actor, 1)
            ON CONFLICT ON CONSTRAINT uq_finance_number_sequence_1 DO NOTHING
            """;
    private static final String TAKE = """
            UPDATE finance.number_sequence
               SET next_value = next_value + 1,
                   updated_at = :now,
                   updated_by = :actor,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND institution_id IS NOT DISTINCT FROM CAST(:institutionId AS uuid)
               AND sequence_code = :sequenceCode AND period_key = '' AND deleted_at IS NULL
            RETURNING COALESCE(prefix, '') || lpad((next_value - 1)::text, padding, '0')
            """;

    private final JdbcClient jdbc;

    public NumberSequenceRepository(@Qualifier(FinanceDatabaseConfig.DATABASE) ModuleDatabase database) {
        this.jdbc = database.jdbc();
    }

    /** The next number of the series (e.g. {@code INV-000042}); opens the series with this prefix on first use. */
    public String next(UUID institutionId, String sequenceCode, String prefix, UUID newId, OffsetDateTime now,
            UUID actor) {
        jdbc.sql(OPEN).param("id", newId).param("institutionId", institutionId).param("sequenceCode", sequenceCode)
                .param("prefix", prefix).param("now", now).param("actor", actor).update();
        return jdbc.sql(TAKE).param("institutionId", institutionId).param("sequenceCode", sequenceCode)
                .param("now", now).param("actor", actor).query(String.class).single();
    }
}
