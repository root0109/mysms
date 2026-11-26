package com.mysms.academics.attendance.events;

import com.mysms.academics.attendance.config.AttendanceDatabaseConfig;
import com.mysms.academics.attendance.config.AttendanceEventsConfig;
import com.mysms.academics.attendance.repository.PrivacyErasureRepository;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.jobs.JobSchedule;
import com.mysms.core.outbox.AuditWriter;
import com.mysms.core.outbox.OutboxWriter;
import com.mysms.privacyclient.ErasureStep;
import com.mysms.privacyclient.KeptRows;
import com.mysms.privacyclient.LegalHolds;
import com.mysms.privacyclient.PrivacyTaskHandler;
import com.mysms.privacyclient.RetentionSweep;
import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import tools.jackson.databind.json.JsonMapper;

/**
 * attendance's part in privacy's erasure and export requests and its retention policy (design
 * PRIVACY-AND-CONSENT-CENTRE §9.1, §10). The registers are kept under attendance.after_leaving: an erasure changes
 * nothing and answers the subject's student and staff attendance as kept, counted with the policy code. The
 * attendance.after_leaving sweep anonymises the student attendance of the students not marked since the cutoff (the
 * mark stays for the register's counts, the student, remark and leave request go); rows a legal hold covers are kept
 * and counted. Staff attendance is not swept: payroll's wage records (payroll.after_leaving) outlive it.
 */
@Component
public class AttendancePrivacyTaskHandler extends PrivacyTaskHandler {

    static final String RETENTION_POLICY = "attendance.after_leaving";

    /** The registers kept under {@link #RETENTION_POLICY}. */
    static final List<KeptRows> KEPT = List.of(
            new KeptRows("attendance.attendance_record", PrivacyErasureRepository.ATTENDANCE_RECORD_KEPT,
                    RETENTION_POLICY),
            new KeptRows("attendance.staff_attendance", PrivacyErasureRepository.STAFF_ATTENDANCE_KEPT,
                    RETENTION_POLICY));

    static final Map<String, RetentionSweep> RETENTION = Map.of(RETENTION_POLICY,
            new RetentionSweep(PrivacyErasureRepository.AFTER_LEAVING_SUBJECTS, List.of(
                    new ErasureStep("attendance.attendance_record", "attendance",
                            PrivacyErasureRepository.AFTER_LEAVING_ROWS,
                            PrivacyErasureRepository.ANONYMISE_ATTENDANCE_RECORD, false,
                            List.of("studentId", "remark", "leaveRequestId")))));

    public AttendancePrivacyTaskHandler(@Qualifier(AttendanceDatabaseConfig.DATABASE) ModuleDatabase database,
            @Qualifier(AttendanceDatabaseConfig.TRANSACTIONS) PlatformTransactionManager transactions,
            @Qualifier(AttendanceEventsConfig.OUTBOX) OutboxWriter outbox,
            @Qualifier(AttendanceEventsConfig.AUDIT) AuditWriter audit,
            @Qualifier(AttendanceDatabaseConfig.JOBS) JobSchedule schedule, LegalHolds holds, JsonMapper json,
            Clock clock, @Value("${mysms.privacy-client.task-interval}") Duration interval) {
        super("attendance", database, transactions, outbox, audit, schedule, holds, json, clock, interval);
    }

    /** No table is erased: the registers are kept under {@link #RETENTION_POLICY}. */
    @Override
    protected List<ErasureStep> erasureSteps() {
        return List.of();
    }

    @Override
    protected Map<String, RetentionSweep> retentionSweeps() {
        return RETENTION;
    }

    /** The registers, kept and counted under {@link #RETENTION_POLICY}. */
    @Override
    protected List<KeptRows> keptRows() {
        return KEPT;
    }
}
