package com.mysms.academics.timetable.repository;

import com.mysms.core.db.ModuleDatabase;
import com.mysms.academics.timetable.config.TimetableDatabaseConfig;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * timetable.institution_zone: each school's own time zone, kept from org's institution events. A row changes only to a
 * newer source row version, so a late or repeated event changes nothing.
 */
@Repository
public class InstitutionZoneRepository {

    private static final String APPLY = """
            INSERT INTO timetable.institution_zone (tenant_id, institution_id, time_zone, is_deleted, source_version,
                                                    synced_at)
            VALUES (:tenantId, :institutionId, :timeZone, :deleted, :version, :now)
            ON CONFLICT (tenant_id, institution_id) DO UPDATE
               SET time_zone = EXCLUDED.time_zone, is_deleted = EXCLUDED.is_deleted,
                   source_version = EXCLUDED.source_version, synced_at = EXCLUDED.synced_at
             WHERE timetable.institution_zone.source_version < EXCLUDED.source_version
            """;
    private static final String ZONE_OF = """
            SELECT z.time_zone
              FROM timetable.institution_zone z
             WHERE z.tenant_id = :tenantId
               AND z.institution_id = :institutionId
               AND NOT z.is_deleted
               AND z.time_zone IS NOT NULL
            """;

    private final JdbcClient jdbc;

    public InstitutionZoneRepository(@Qualifier(TimetableDatabaseConfig.DATABASE) ModuleDatabase database) {
        this.jdbc = database.jdbc();
    }

    /** Applies one institution event ({@code timeZone} null = the tenant's); whether the stored entry changed. */
    public boolean apply(UUID institutionId, String timeZone, boolean deleted, int version, OffsetDateTime now) {
        return jdbc.sql(APPLY).param("institutionId", institutionId).param("timeZone", timeZone)
                .param("deleted", deleted).param("version", version).param("now", now).update() == 1;
    }

    /** The school's own time zone, when org gave it one. */
    public Optional<String> zoneOf(UUID institutionId) {
        return jdbc.sql(ZONE_OF).param("institutionId", institutionId).query(String.class).optional();
    }
}
