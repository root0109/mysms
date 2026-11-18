package com.mysms.finance.business;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mysms.core.outbox.AuditWriter;
import com.mysms.core.outbox.OutboxWriter;
import com.mysms.core.validation.DtoValidator;
import com.mysms.finance.mapper.StudentFeeDueMapper;
import com.mysms.finance.model.DueTarget;
import com.mysms.finance.repository.FeeScheduleRepository;
import com.mysms.finance.repository.FeeScheduleRepository.ComponentRow;
import com.mysms.finance.repository.FeeScheduleRepository.InstallmentRow;
import com.mysms.finance.repository.FeeScheduleRepository.PlanRow;
import com.mysms.finance.repository.StudentFeeDueRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataIntegrityViolationException;

/**
 * One student who cannot be charged never holds back the others (stand-ins for the repositories; the fee run over
 * HTTP is FeeWorkflowComponentTest's): the failing student's transaction rolls back alone, the run goes on, and the
 * operation's outcome names them as FAILED.
 */
class FeeRunFailureTest {

    private final StandInTransactions tx = new StandInTransactions();
    private final StudentFeeDueRepository repository = mock(StudentFeeDueRepository.class);
    private final FeeScheduleRepository schedules = mock(FeeScheduleRepository.class);
    private final OperationService operations = mock(OperationService.class);
    private final StudentFeeDueService service = new StudentFeeDueService(tx.database, tx.transactions, repository,
            schedules, mock(StudentFeeDueMapper.class), mock(StudentOwnershipLookup.class), mock(RosterLookup.class),
            operations, mock(OutboxWriter.class), mock(AuditWriter.class), mock(DtoValidator.class),
            Clock.systemUTC());

    @Test
    void aStudentWhoseDuesCannotBeWrittenIsListedAndTheRunGoesOn() {
        UUID plan = UUID.randomUUID();
        UUID installment = UUID.randomUUID();
        UUID first = UUID.randomUUID();
        UUID failing = UUID.randomUUID();
        UUID last = UUID.randomUUID();
        when(schedules.plan(plan)).thenReturn(Optional.of(new PlanRow(plan, UUID.randomUUID(), "fixed_days", null,
                null, null, true)));
        when(schedules.installments(plan)).thenReturn(List.of(new InstallmentRow(installment, 1, 0, null,
                new BigDecimal("100"))));
        when(schedules.components(List.of(installment))).thenReturn(List.of(new ComponentRow(installment,
                UUID.randomUUID(), 1, new BigDecimal("100"), new BigDecimal("1000.00"), UUID.randomUUID())));
        when(schedules.concessions(any(), any())).thenReturn(List.of());
        UUID operation = UUID.randomUUID();
        when(operations.start(any(), any(), any())).thenReturn(operation);
        when(operations.progress(eq(operation), anyInt())).thenReturn(true);
        when(repository.insertRun(argThat(run -> run != null && !failing.equals(run.studentId())), any(), any(),
                any())).thenReturn(List.of(UUID.randomUUID()));
        when(repository.insertRun(argThat(run -> run != null && failing.equals(run.studentId())), any(), any(),
                any())).thenThrow(new DataIntegrityViolationException("a constraint of the database"));

        service.generate(new StudentFeeDueService.FeeRun(plan, UUID.randomUUID(), null, LocalDate.of(2026, 4, 1),
                null, List.of(DueTarget.student(first), DueTarget.student(failing), DueTarget.student(last))),
                StudentFeeDueService.KIND_GENERATE, "finance.fee_plan", plan);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> outcome = ArgumentCaptor.forClass(Map.class);
        verify(operations).succeed(eq(operation), outcome.capture());
        verify(operations, never()).fail(any(), any(), any());
        assertThat(outcome.getValue()).containsEntry("students", 2).containsEntry("dues", 2)
                .containsEntry("failed", 1).containsEntry("skippedCount", 1)
                .containsEntry("skipped", List.of(Map.of("studentId", failing.toString(), "reason", "FAILED")));
        verify(repository).insertRun(argThat(run -> run != null && last.equals(run.studentId())), any(), any(),
                any());
    }
}
