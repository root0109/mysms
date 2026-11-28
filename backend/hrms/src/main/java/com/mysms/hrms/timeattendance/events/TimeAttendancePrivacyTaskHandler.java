package com.mysms.hrms.timeattendance.events;

import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.jobs.JobSchedule;
import com.mysms.core.outbox.AuditWriter;
import com.mysms.core.outbox.OutboxWriter;
import com.mysms.hrms.timeattendance.config.TimeAttendanceDatabaseConfig;
import com.mysms.hrms.timeattendance.config.TimeAttendanceEventsConfig;
import com.mysms.hrms.timeattendance.repository.PrivacyErasureRepository;
import com.mysms.privacyclient.ErasureStep;
import com.mysms.privacyclient.KeptRows;
import com.mysms.privacyclient.LegalHolds;
import com.mysms.privacyclient.PrivacyTaskHandler;
import java.time.Clock;
import java.time.Duration;
import java.util.List;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import tools.jackson.databind.json.JsonMapper;

/**
 * time_attendance's part in privacy's erasure and export requests and its retention policy (design
 * PRIVACY-AND-CONSENT-CENTRE §9.1, §10). The register is kept under time_attendance.after_leaving: an erasure
 * changes nothing and answers the subject's staff attendance days as kept, counted with the policy code. The days
 * are never anonymised — as under attendance.after_leaving before the move, whose sweep reached students only: they
 * back payroll's wage records (payroll.after_leaving) and stay the school's evidence of presence. The policy's action
 * is therefore archive, like payroll's, and archive is not built: its retention runs answer failed
 * (RETENTION_POLICY_UNSUPPORTED) exactly as payroll's do until archiving exists.
 */
@Component
public class TimeAttendancePrivacyTaskHandler extends PrivacyTaskHandler {

    static final String RETENTION_POLICY = "time_attendance.after_leaving";

    /** The register kept under {@link #RETENTION_POLICY}. */
    static final List<KeptRows> KEPT = List.of(
            new KeptRows("time_attendance.staff_attendance", PrivacyErasureRepository.STAFF_ATTENDANCE_KEPT,
                    RETENTION_POLICY));

    public TimeAttendancePrivacyTaskHandler(
            @Qualifier(TimeAttendanceDatabaseConfig.DATABASE) ModuleDatabase database,
            @Qualifier(TimeAttendanceDatabaseConfig.TRANSACTIONS) PlatformTransactionManager transactions,
            @Qualifier(TimeAttendanceEventsConfig.OUTBOX) OutboxWriter outbox,
            @Qualifier(TimeAttendanceEventsConfig.AUDIT) AuditWriter audit,
            @Qualifier(TimeAttendanceDatabaseConfig.JOBS) JobSchedule schedule, LegalHolds holds, JsonMapper json,
            Clock clock, @Value("${mysms.privacy-client.task-interval}") Duration interval) {
        super("time_attendance", database, transactions, outbox, audit, schedule, holds, json, clock, interval);
    }

    /** No table is erased: the register is kept under {@link #RETENTION_POLICY}. */
    @Override
    protected List<ErasureStep> erasureSteps() {
        return List.of();
    }

    /** The register, kept and counted under {@link #RETENTION_POLICY}. */
    @Override
    protected List<KeptRows> keptRows() {
        return KEPT;
    }
}
