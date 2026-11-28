package com.mysms.hrms.hr.repository;

import com.mysms.core.db.ModuleDatabase;
import com.mysms.hrms.hr.config.HrDatabaseConfig;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * hr.holiday_record: the holidays of the tenant's institutions, kept from org's holiday events and read by
 * {@link WorkingDays}. A row changes only to a newer source row version, so a late or repeated event changes nothing.
 */
@Repository
public class HolidayRecordRepository {

    private static final String APPLY = """
            INSERT INTO hr.holiday_record (tenant_id, holiday_id, institution_id, title, start_date, end_date,
                                           is_deleted, source_version, synced_at)
            VALUES (:tenantId, :holidayId, :institutionId, :title, :startDate, :endDate, :deleted, :version, :now)
            ON CONFLICT (tenant_id, holiday_id) DO UPDATE
               SET institution_id = EXCLUDED.institution_id, title = EXCLUDED.title,
                   start_date = EXCLUDED.start_date, end_date = EXCLUDED.end_date,
                   is_deleted = EXCLUDED.is_deleted, source_version = EXCLUDED.source_version,
                   synced_at = EXCLUDED.synced_at
             WHERE hr.holiday_record.source_version < EXCLUDED.source_version
            """;

    /** One holiday as an event names it: NULL institution = every institution of the tenant. */
    public record HolidayRecord(UUID holidayId, UUID institutionId, String title, LocalDate startDate,
            LocalDate endDate) {
    }

    private final JdbcClient jdbc;

    public HolidayRecordRepository(@Qualifier(HrDatabaseConfig.DATABASE) ModuleDatabase database) {
        this.jdbc = database.jdbc();
    }

    /** Applies one event of a holiday; whether the stored entry changed (false for an older or repeated one). */
    public boolean apply(HolidayRecord holiday, boolean deleted, int version, OffsetDateTime now) {
        return jdbc.sql(APPLY).param("holidayId", holiday.holidayId()).param("institutionId", holiday.institutionId())
                .param("title", holiday.title()).param("startDate", holiday.startDate())
                .param("endDate", holiday.endDate()).param("deleted", deleted).param("version", version)
                .param("now", now).update() == 1;
    }
}
