package com.mysms.students.admission.repository;

import com.mysms.core.db.ModuleDatabase;
import com.mysms.students.admission.config.AdmissionDatabaseConfig;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * admission's own number source (admission.number_sequence): one counter per institution, sequence code and
 * period. A number is taken inside the issuing transaction with the counter's row locked ({@code SELECT … FOR UPDATE}), so two
 * transactions never receive the same number, and a transaction that rolls back gives its number back.
 */
@Repository
public class NumberSequenceRepository {

    /** A counter as locked: its row, the value the next taker receives, and how numbers are written. */
    private record Counter(UUID id, long nextValue, String prefix, int padding) {
    }

    private static final String LOCK = """
            SELECT id, next_value, prefix, padding
              FROM admission.number_sequence
             WHERE tenant_id = :tenantId
               AND institution_id = :institutionId AND sequence_code = :code AND period_key = :periodKey
               AND deleted_at IS NULL
               FOR UPDATE
            """;
    private static final String OPEN = """
            INSERT INTO admission.number_sequence
            (id, tenant_id, institution_id, sequence_code, period_key, prefix, created_at, created_by, updated_at,
             updated_by, row_version)
            VALUES (:id, :tenantId, :institutionId, :code, :periodKey, :prefix, :now, :actor, :now, :actor, 1)
            ON CONFLICT DO NOTHING
            """;
    private static final String ADVANCE = """
            UPDATE admission.number_sequence
               SET next_value = next_value + 1,
                   updated_at = :now,
                   updated_by = :actor,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND id = :id
            """;

    private final JdbcClient jdbc;

    public NumberSequenceRepository(@Qualifier(AdmissionDatabaseConfig.DATABASE) ModuleDatabase database) {
        this.jdbc = database.jdbc();
    }

    /**
     * Takes the next number of the institution's sequence for the period, written with the counter's prefix and
     * zero padding. The counter is opened on first use with the given prefix; an administrator's later change of the
     * prefix or padding in the table is respected.
     *
     * @param newId  the id of the counter row, should this call open it
     * @param prefix the prefix a counter opened by this call writes its numbers with
     */
    public String next(UUID newId, UUID institutionId, String code, String periodKey, String prefix,
            OffsetDateTime now, UUID actor) {
        Optional<Counter> counter = lock(institutionId, code, periodKey);
        if (counter.isEmpty()) {
            jdbc.sql(OPEN).param("id", newId).param("institutionId", institutionId).param("code", code)
                    .param("periodKey", periodKey).param("prefix", prefix).param("now", now).param("actor", actor)
                    .update();
            counter = lock(institutionId, code, periodKey);
        }
        Counter locked = counter.orElseThrow(() -> new IllegalStateException("number sequence could not be opened"));
        jdbc.sql(ADVANCE).param("id", locked.id()).param("now", now).param("actor", actor).update();
        String digits = String.valueOf(locked.nextValue());
        return (locked.prefix() == null ? "" : locked.prefix())
                + "0".repeat(Math.max(0, locked.padding() - digits.length())) + digits;
    }

    private Optional<Counter> lock(UUID institutionId, String code, String periodKey) {
        return jdbc.sql(LOCK).param("institutionId", institutionId).param("code", code).param("periodKey", periodKey)
                .query((rs, row) -> new Counter(rs.getObject("id", UUID.class), rs.getLong("next_value"),
                        rs.getString("prefix"), rs.getInt("padding")))
                .optional();
    }
}
