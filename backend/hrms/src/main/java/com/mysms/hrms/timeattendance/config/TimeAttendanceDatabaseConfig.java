package com.mysms.hrms.timeattendance.config;

import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.db.ModuleDatabaseHealthIndicator;
import com.mysms.core.jobs.JobSchedule;
import com.mysms.hrms.hr.config.HrDatabaseConfig;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.core.env.Environment;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * The area's own database: pool, transaction manager and Flyway history for schema "time_attendance" only,
 * bound from mysms.time-attendance.datasource.* (every transaction binds the caller's tenant for row-level security).
 */
@Configuration(proxyBeanMethods = false)
public class TimeAttendanceDatabaseConfig {

    /** Bean name; every repository of this area injects the database by this qualifier. */
    public static final String DATABASE = "timeAttendanceDatabase";

    /** Bean name of the area's transaction manager; every service injects it by this qualifier. */
    public static final String TRANSACTIONS = "timeAttendanceTransactions";

    /** Bean name of the area's job schedule; the area's scheduled jobs inject it by this qualifier. */
    public static final String JOBS = "timeAttendanceJobSchedule";

    /**
     * Opened (and migrated) after hr's database: the baseline's foreign key into {@code hr.leave_application} needs
     * hr's schema, and each area runs its own Flyway history, so only the bean order guarantees it.
     */
    @Bean(name = DATABASE, destroyMethod = "close")
    @DependsOn(HrDatabaseConfig.DATABASE)
    public ModuleDatabase timeAttendanceDatabase(Environment environment) {
        return ModuleDatabase.open("time_attendance", environment, true);
    }

    /**
     * The transaction manager of the area's own pool; the database binds the call's tenant inside each transaction.
     */
    @Bean(name = TRANSACTIONS)
    public PlatformTransactionManager timeAttendanceTransactions(@Qualifier(DATABASE) ModuleDatabase database) {
        return database.transactionManager();
    }

    /** time_attendance.job_schedule: the tenants enrolled in the area's scheduled jobs (core JobRuntime runs them). */
    @Bean(name = JOBS)
    public JobSchedule timeAttendanceJobSchedule(@Qualifier(DATABASE) ModuleDatabase database) {
        return new JobSchedule(database);
    }

    @Bean
    public ModuleDatabaseHealthIndicator timeAttendanceDatabaseHealthIndicator(
            @Qualifier(DATABASE) ModuleDatabase database) {
        return new ModuleDatabaseHealthIndicator(database);
    }
}
