package com.mysms.finance.repository;

import com.mysms.core.db.ModuleDatabase;
import com.mysms.finance.config.FinanceDatabaseConfig;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * The statements of finance's reference cleanup: each changes the rows of one logical reference to a deleted owner,
 * inside the event's tenant, and returns their ids.
 */
@Repository
public class ReferenceCleanupRepository {

    /**
     * finance.course_fee_default.course_id, on academics.course deletion: archive/delete the dependent row.
     */
    public static final String ARCHIVE_COURSE_FEE_DEFAULT_COURSE_ID = """
            UPDATE finance.course_fee_default
               SET deleted_at = :now, updated_at = :now, row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND course_id = :ownerId
               AND deleted_at IS NULL
            RETURNING id
            """;

    /**
     * finance.invoice.document_file_id, on platform.file_object deletion: null the column.
     */
    public static final String CLEAR_INVOICE_DOCUMENT_FILE_ID = """
            UPDATE finance.invoice
               SET document_file_id = NULL, updated_at = :now, row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND document_file_id = :ownerId
            RETURNING id
            """;

    /**
     * finance.payment.billing_address_id, on people.person_address deletion: null the column.
     */
    public static final String CLEAR_PAYMENT_BILLING_ADDRESS_ID = """
            UPDATE finance.payment
               SET billing_address_id = NULL, updated_at = :now, row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND billing_address_id = :ownerId
            RETURNING id
            """;

    /**
     * finance.program_enrollment_fee.program_enrollment_id, on sis.program_enrollment deletion: archive/delete the dependent row.
     */
    public static final String ARCHIVE_PROGRAM_ENROLLMENT_FEE_PROGRAM_ENROLLMENT_ID = """
            UPDATE finance.program_enrollment_fee
               SET deleted_at = :now, updated_at = :now, row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND program_enrollment_id = :ownerId
               AND deleted_at IS NULL
            RETURNING id
            """;

    /**
     * finance.student_fee_due.program_enrollment_id, on sis.program_enrollment deletion: null the column.
     */
    public static final String CLEAR_STUDENT_FEE_DUE_PROGRAM_ENROLLMENT_ID = """
            UPDATE finance.student_fee_due
               SET program_enrollment_id = NULL, updated_at = :now, row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND program_enrollment_id = :ownerId
            RETURNING id
            """;

    /**
     * finance.student_fee_due.student_enrollment_id, on sis.student_enrollment deletion: null the column.
     */
    public static final String CLEAR_STUDENT_FEE_DUE_STUDENT_ENROLLMENT_ID = """
            UPDATE finance.student_fee_due
               SET student_enrollment_id = NULL, updated_at = :now, row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND student_enrollment_id = :ownerId
            RETURNING id
            """;

    /**
     * finance.coupon_scope.lms_course_id, on lms.course deletion: archive/delete the dependent row.
     */
    public static final String ARCHIVE_COUPON_SCOPE_LMS_COURSE_ID = """
            UPDATE finance.coupon_scope
               SET deleted_at = :now, updated_at = :now, row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND lms_course_id = :ownerId
               AND deleted_at IS NULL
            RETURNING id
            """;

    /**
     * finance.coupon_scope.lms_cohort_id, on lms.cohort deletion: archive/delete the dependent row.
     */
    public static final String ARCHIVE_COUPON_SCOPE_LMS_COHORT_ID = """
            UPDATE finance.coupon_scope
               SET deleted_at = :now, updated_at = :now, row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND lms_cohort_id = :ownerId
               AND deleted_at IS NULL
            RETURNING id
            """;

    private final JdbcClient jdbc;

    public ReferenceCleanupRepository(@Qualifier(FinanceDatabaseConfig.DATABASE) ModuleDatabase database) {
        this.jdbc = database.jdbc();
    }

    /** Runs one cleanup statement for the deleted owner; the ids of the rows it changed. */
    public List<UUID> apply(String statement, UUID ownerId, OffsetDateTime now) {
        return jdbc.sql(statement).param("ownerId", ownerId).param("now", now).query(UUID.class).list();
    }
}
