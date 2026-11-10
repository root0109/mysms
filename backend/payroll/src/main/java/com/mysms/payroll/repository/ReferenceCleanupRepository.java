package com.mysms.payroll.repository;

import com.mysms.core.db.ModuleDatabase;
import com.mysms.payroll.config.PayrollDatabaseConfig;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * The statements of payroll's reference cleanup (database/logical-references.md): each changes the rows of one logical
 * reference to a deleted owner, inside the event's tenant, and returns their ids.
 */
@Repository
public class ReferenceCleanupRepository {

    /**
     * payroll.payroll_payment.attachment_file_id, on platform.file_object deletion: null the column.
     */
    public static final String CLEAR_PAYROLL_PAYMENT_ATTACHMENT_FILE_ID = """
            UPDATE payroll.payroll_payment
               SET attachment_file_id = NULL, updated_at = :now, row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND attachment_file_id = :ownerId
            RETURNING id
            """;

    /**
     * payroll.payslip.document_file_id, on platform.file_object deletion: null the column.
     */
    public static final String CLEAR_PAYSLIP_DOCUMENT_FILE_ID = """
            UPDATE payroll.payslip
               SET document_file_id = NULL, updated_at = :now, row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND document_file_id = :ownerId
            RETURNING id
            """;

    /**
     * payroll.staff_bank_account.staff_id, on hr.staff deletion: archive/delete the dependent row.
     */
    public static final String ARCHIVE_STAFF_BANK_ACCOUNT_STAFF_ID = """
            UPDATE payroll.staff_bank_account
               SET deleted_at = :now, updated_at = :now, row_version = row_version + 1
             WHERE tenant_id = :tenantId
               AND staff_id = :ownerId
               AND deleted_at IS NULL
            RETURNING id
            """;

    private final JdbcClient jdbc;

    public ReferenceCleanupRepository(@Qualifier(PayrollDatabaseConfig.DATABASE) ModuleDatabase database) {
        this.jdbc = database.jdbc();
    }

    /** Runs one cleanup statement for the deleted owner; the ids of the rows it changed. */
    public List<UUID> apply(String statement, UUID ownerId, OffsetDateTime now) {
        return jdbc.sql(statement).param("ownerId", ownerId).param("now", now).query(UUID.class).list();
    }
}
