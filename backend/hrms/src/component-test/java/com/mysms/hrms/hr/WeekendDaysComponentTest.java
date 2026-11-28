package com.mysms.hrms.hr;

import static org.assertj.core.api.Assertions.assertThat;

import com.mysms.authclient.testing.TestTokens;
import com.mysms.core.context.CallContext;
import com.mysms.core.context.CallContextHolder;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.testkit.TestDatabase;
import com.mysms.core.testkit.TestTransactions;
import com.mysms.hrms.HrmsApplication;
import com.mysms.hrms.hr.config.HrDatabaseConfig;
import com.mysms.hrms.hr.repository.LeaveLedgerRepository;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;

/**
 * The weekend of {@code mysms.hr.leave.weekend-days} as leave counts it, against real PostgreSQL: another weekend than
 * the default, and a seven-day week (blank). The week counted is Monday 2 to Sunday 8 March 2026, with a holiday of the
 * whole tenant on Wednesday 4.
 */
class WeekendDaysComponentTest {

    private static final TestDatabase DATABASE = TestDatabase.createDropOnExit();
    private static final TestTokens TOKENS = new TestTokens();
    private static final String TRACE = "4bf92f3577b34da6a3ce929d0e0e4736";

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        DATABASE.register(registry, "hr", "payroll", "time_attendance");
        registry.add("mysms.auth.issuer", () -> TestTokens.ISSUER);
        registry.add("mysms.auth.public-key", TOKENS::publicKeyPem);
    }

    /** The days of a full-day application for the week, in a tenant of its own. */
    BigDecimal daysOfTheWeek(ModuleDatabase database, LeaveLedgerRepository ledger) {
        UUID tenant = UUID.randomUUID();
        UUID institution = UUID.randomUUID();
        UUID staff = UUID.randomUUID();
        UUID application = UUID.randomUUID();
        return CallContextHolder.callAs(CallContext.anonymous(TRACE).withIdentity(tenant, null),
                () -> TestTransactions.of(database).write(() -> {
                    database.jdbc().sql("INSERT INTO hr.staff (id, tenant_id, institution_id, person_id, employee_code)"
                            + " VALUES ('" + staff + "', :tenantId, '" + institution + "', '" + UUID.randomUUID()
                            + "', 'W-1')").update();
                    database.jdbc().sql("INSERT INTO hr.holiday_record (tenant_id, holiday_id, title, start_date,"
                            + " end_date, source_version, synced_at) VALUES (:tenantId, '" + UUID.randomUUID()
                            + "', 'Closed', '2026-03-04', '2026-03-04', 1, now())").update();
                    database.jdbc().sql("INSERT INTO hr.leave_application (id, tenant_id, institution_id,"
                            + " academic_year_id, staff_id, from_at, to_at) VALUES ('" + application
                            + "', :tenantId, '" + institution + "', '" + UUID.randomUUID() + "', '" + staff
                            + "', '2026-03-02T00:00:00Z', '2026-03-08T00:00:00Z')").update();
                    return ledger.daysOf(application);
                }));
    }

    @Nested
    @SpringBootTest(classes = HrmsApplication.class)
    @TestPropertySource(properties = "mysms.hr.leave.weekend-days=FRIDAY")
    class AFridayWeekend {

        @Autowired
        @Qualifier(HrDatabaseConfig.DATABASE)
        ModuleDatabase database;

        @Autowired
        LeaveLedgerRepository ledger;

        @Test
        void countsSaturdayAndSundayAndLeavesOutFridayAndTheHoliday() {
            // seven days, less Friday 6 and the holiday on the 4th
            assertThat(daysOfTheWeek(database, ledger)).isEqualByComparingTo("5");
        }
    }

    @Nested
    @SpringBootTest(classes = HrmsApplication.class)
    @TestPropertySource(properties = "mysms.hr.leave.weekend-days=")
    class ASevenDayWeek {

        @Autowired
        @Qualifier(HrDatabaseConfig.DATABASE)
        ModuleDatabase database;

        @Autowired
        LeaveLedgerRepository ledger;

        @Test
        void countsEveryDayButTheHoliday() {
            // seven days, less the holiday on the 4th
            assertThat(daysOfTheWeek(database, ledger)).isEqualByComparingTo("6");
        }
    }
}
