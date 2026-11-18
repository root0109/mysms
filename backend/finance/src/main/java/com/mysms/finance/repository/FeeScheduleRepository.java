package com.mysms.finance.repository;

import com.mysms.core.db.ModuleDatabase;
import com.mysms.finance.config.FinanceDatabaseConfig;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * What a fee run reads to price dues (finance.GenerateFeeDues, finance.PublishFeeStructure): the plan with its
 * installments, the fee heads each installment shares out, and the concessions of the students charged. Read-only
 * projections, constant SQL, the tenant filtered on every table.
 */
@Repository
public class FeeScheduleRepository {

    /** A fee plan's payment terms. */
    public record PlanRow(UUID id, UUID institutionId, String scheduleBasis, Integer offsetDays,
            String offsetDirection, BigDecimal discountPercent, boolean active) {
    }

    /** An installment of the plan: its share of the total and when it falls due. */
    public record InstallmentRow(UUID id, int sequence, Integer dueDays, LocalDate dueDate, BigDecimal sharePercent) {
    }

    /** A fee head an installment includes, its share of the installment, and the head's own pricing. */
    public record ComponentRow(UUID installmentId, UUID feeHeadId, Integer sequence, BigDecimal sharePercent,
            BigDecimal defaultAmount, UUID currencyId) {
    }

    /** An active concession of a student for the year (fee head null: every head). */
    public record ConcessionRow(UUID id, UUID studentId, UUID feeHeadId, String type, BigDecimal value,
            LocalDate validFrom, LocalDate validUntil) {
    }

    private static final String PLAN = """
            SELECT p.id, p.institution_id, p.schedule_basis, p.offset_days, p.offset_direction, p.discount_percent,
                   p.is_active
              FROM finance.fee_plan p
             WHERE p.tenant_id = :tenantId
               AND p.id = :id AND p.deleted_at IS NULL
            """;
    private static final String INSTALLMENTS = """
            SELECT i.id, i.sequence, i.due_days, i.due_date, i.share_percent
              FROM finance.fee_plan_installment i
             WHERE i.tenant_id = :tenantId
               AND i.fee_plan_id = :planId AND i.deleted_at IS NULL
             ORDER BY i.sequence, i.id
            """;
    private static final String COMPONENTS = """
            SELECT c.installment_id, c.fee_head_id, c.sequence, c.share_percent, h.default_amount, h.currency_id
              FROM finance.fee_plan_installment_component c
              JOIN finance.fee_head h ON h.tenant_id = :tenantId AND h.id = c.fee_head_id AND h.deleted_at IS NULL
             WHERE c.tenant_id = :tenantId
               AND c.installment_id = ANY(:installmentIds) AND c.deleted_at IS NULL
             ORDER BY c.installment_id, c.sequence NULLS LAST, c.fee_head_id
            """;
    private static final String CONCESSIONS = """
            SELECT c.id, c.student_id, c.fee_head_id, c.concession_type, c.concession_value, c.valid_from,
                   c.valid_until
              FROM finance.fee_concession c
             WHERE c.tenant_id = :tenantId
               AND c.student_id = ANY(:studentIds) AND c.academic_year_id = :academicYearId
               AND c.status = 'active' AND c.deleted_at IS NULL
             ORDER BY c.created_at, c.id
            """;

    private final JdbcClient jdbc;

    public FeeScheduleRepository(@Qualifier(FinanceDatabaseConfig.DATABASE) ModuleDatabase database) {
        this.jdbc = database.jdbc();
    }

    public Optional<PlanRow> plan(UUID id) {
        return jdbc.sql(PLAN).param("id", id)
                .query((rs, row) -> new PlanRow(rs.getObject("id", UUID.class),
                        rs.getObject("institution_id", UUID.class), rs.getString("schedule_basis"),
                        rs.getObject("offset_days", Integer.class), rs.getString("offset_direction"),
                        rs.getBigDecimal("discount_percent"), rs.getBoolean("is_active")))
                .optional();
    }

    public List<InstallmentRow> installments(UUID planId) {
        return jdbc.sql(INSTALLMENTS).param("planId", planId)
                .query((rs, row) -> new InstallmentRow(rs.getObject("id", UUID.class), rs.getInt("sequence"),
                        rs.getObject("due_days", Integer.class), rs.getObject("due_date", LocalDate.class),
                        rs.getBigDecimal("share_percent")))
                .list();
    }

    public List<ComponentRow> components(Collection<UUID> installmentIds) {
        return jdbc.sql(COMPONENTS).param("installmentIds", installmentIds.toArray(UUID[]::new))
                .query((rs, row) -> new ComponentRow(rs.getObject("installment_id", UUID.class),
                        rs.getObject("fee_head_id", UUID.class), rs.getObject("sequence", Integer.class),
                        rs.getBigDecimal("share_percent"), rs.getBigDecimal("default_amount"),
                        rs.getObject("currency_id", UUID.class)))
                .list();
    }

    public List<ConcessionRow> concessions(Collection<UUID> studentIds, UUID academicYearId) {
        return jdbc.sql(CONCESSIONS).param("studentIds", studentIds.toArray(UUID[]::new))
                .param("academicYearId", academicYearId)
                .query((rs, row) -> new ConcessionRow(rs.getObject("id", UUID.class),
                        rs.getObject("student_id", UUID.class), rs.getObject("fee_head_id", UUID.class),
                        rs.getString("concession_type"), rs.getBigDecimal("concession_value"),
                        rs.getObject("valid_from", LocalDate.class), rs.getObject("valid_until", LocalDate.class)))
                .list();
    }
}
