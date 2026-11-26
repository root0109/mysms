package com.mysms.students.sis.business;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mysms.authclient.DataScope;
import com.mysms.core.context.CallContext;
import com.mysms.core.context.CallContextHolder;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.error.InvalidStateException;
import com.mysms.core.error.PreconditionFailedException;
import com.mysms.core.outbox.AuditWriter;
import com.mysms.core.outbox.OutboxWriter;
import com.mysms.students.sis.business.AcademicsLookup.PlannedSubject;
import com.mysms.students.sis.domain.SubjectRegistrationEntity;
import com.mysms.students.sis.domain.SubjectRegistrationLineEntity;
import com.mysms.students.sis.mapper.SubjectRegistrationMapper;
import com.mysms.students.sis.repository.ProgramEnrollmentSubjectRepository;
import com.mysms.students.sis.repository.SubjectRegistrationLineRepository;
import com.mysms.students.sis.repository.SubjectRegistrationRepository;
import java.time.Clock;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.transaction.support.SimpleTransactionStatus;

/**
 * How sis.LoadCourseSubjects stays consistent with a concurrent submit (a unit test over stand-ins of the
 * repositories and of academics; the SQL of the lock and the version guard runs in the component tests): the
 * registration is locked before its lines are read and written, a draft submitted meanwhile gets no lines, and
 * added lines give the registration a new version, so an etag read before them no longer passes.
 */
class SubjectRegistrationWorkflowServiceTest {

    private static final UUID INSTITUTION = UUID.randomUUID();
    private static final UUID TENANT = UUID.randomUUID();
    private static final DataScope SCHOOL = new DataScope(false, Set.of(INSTITUTION), false);

    private final SubjectRegistrationRepository registrations = mock(SubjectRegistrationRepository.class);
    private final SubjectRegistrationLineRepository lines = mock(SubjectRegistrationLineRepository.class);
    private final AcademicsLookup academics = mock(AcademicsLookup.class);
    private final PlatformTransactionManager transactions = mock(PlatformTransactionManager.class);
    private final ModuleDatabase database = mock(ModuleDatabase.class);
    private final SubjectRegistrationWorkflowService service = new SubjectRegistrationWorkflowService(registrations,
            lines, mock(ProgramEnrollmentSubjectRepository.class), mock(SubjectRegistrationMapper.class), academics,
            database, transactions, mock(OutboxWriter.class), mock(AuditWriter.class), Clock.systemUTC());
    private final UUID id = UUID.randomUUID();

    @BeforeEach
    void transactionsRunTheWork() {
        when(database.readDefinition()).thenReturn(new DefaultTransactionDefinition());
        when(database.writeDefinition()).thenReturn(new DefaultTransactionDefinition());
        when(transactions.getTransaction(any())).thenAnswer(invocation -> new SimpleTransactionStatus(true));
        when(registrations.institutionOf(any())).thenReturn(Optional.of(INSTITUTION));
        when(registrations.findById(id)).thenReturn(Optional.of(registration("draft", 1)));
        when(registrations.lockById(id)).thenAnswer(invocation -> registrations.findById(id));
        when(lines.ofRegistration(id)).thenReturn(List.of());
        when(lines.insert(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    private SubjectRegistrationEntity registration(String status, int version) {
        SubjectRegistrationEntity registration = new SubjectRegistrationEntity();
        registration.setId(id);
        registration.setStudentId(UUID.randomUUID());
        registration.setCourseId(UUID.randomUUID());
        registration.setStatus(status);
        registration.setRowVersion(version);
        return registration;
    }

    private void load() {
        CallContextHolder.callAs(CallContext.anonymous("4bf92f3577b34da6a3ce929d0e0e4736").withIdentity(TENANT, null),
                () -> service.loadCourseSubjects(SCHOOL, id));
    }

    @Test
    void aDraftSubmittedWhileTheCourseWasReadGetsNoLines() {
        when(academics.courseSubjects(any(), any())).thenAnswer(invocation -> {
            when(registrations.findById(id)).thenReturn(Optional.of(registration("submitted", 2)));
            return List.of(new PlannedSubject(UUID.randomUUID(), UUID.randomUUID(), false));
        });

        assertThatThrownBy(this::load).isInstanceOf(InvalidStateException.class);
        verify(lines, never()).insert(any());
        verify(registrations, never()).linesChanged(any(), anyInt(), any(), any());
    }

    @Test
    void addedLinesGiveTheLockedRegistrationANewVersion() {
        when(academics.courseSubjects(any(), any()))
                .thenReturn(List.of(new PlannedSubject(UUID.randomUUID(), UUID.randomUUID(), true)));
        when(registrations.linesChanged(eq(id), eq(1), any(), any())).thenReturn(1);

        load();

        InOrder order = inOrder(registrations, lines);
        order.verify(registrations).lockById(id);
        order.verify(lines).ofRegistration(id);
        order.verify(lines).insert(any(SubjectRegistrationLineEntity.class));
        order.verify(registrations).linesChanged(eq(id), eq(1), any(), any());
    }

    @Test
    void aRegistrationChangedUnderTheLockIsRefused() {
        when(academics.courseSubjects(any(), any()))
                .thenReturn(List.of(new PlannedSubject(UUID.randomUUID(), UUID.randomUUID(), false)));
        when(registrations.linesChanged(any(), anyInt(), any(), any())).thenReturn(0);

        assertThatThrownBy(this::load).isInstanceOf(PreconditionFailedException.class);
    }

    @Test
    void nothingToAddLeavesTheVersionAlone() {
        when(academics.courseSubjects(any(), any())).thenReturn(List.of());

        load();

        verify(registrations).lockById(id);
        verify(registrations, never()).linesChanged(any(), anyInt(), any(), any());
    }
}
