package com.mysms.sis.business;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mysms.authclient.DataScope;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.error.InvalidStateException;
import com.mysms.core.error.PermissionDeniedException;
import com.mysms.core.error.ResourceNotFoundException;
import com.mysms.core.error.ValidationFailedException;
import com.mysms.core.outbox.AuditWriter;
import com.mysms.core.outbox.OutboxWriter;
import com.mysms.sis.api.model.CreatePortalAccountRequest;
import com.mysms.sis.api.model.CreateStudentPortalAccountRequest;
import com.mysms.sis.domain.GuardianEntity;
import com.mysms.sis.domain.StudentEntity;
import com.mysms.sis.mapper.GuardianMapper;
import com.mysms.sis.mapper.StudentMapper;
import com.mysms.sis.repository.FamilyLinkRepository;
import com.mysms.sis.repository.GuardianRepository;
import com.mysms.sis.repository.StudentRepository;
import java.time.Clock;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.transaction.support.SimpleTransactionStatus;

/**
 * What the portal-account flow re-checks after iam answered (a unit test over stand-ins of the repositories and of
 * iam; the real iam answers in the app module's component test): a student who left, or a record that moved out of
 * the caller's scope, while the account was opened is not linked to it. And an invitation is refused before
 * anything is asked.
 */
class PortalAccountServiceTest {

    private static final UUID INSTITUTION = UUID.randomUUID();
    private static final DataScope SCHOOL = new DataScope(false, Set.of(INSTITUTION), false);

    private final StudentRepository students = mock(StudentRepository.class);
    private final GuardianRepository guardians = mock(GuardianRepository.class);
    private final FamilyLinkRepository families = mock(FamilyLinkRepository.class);
    private final PortalAccountStore accounts = mock(PortalAccountStore.class);
    private final PlatformTransactionManager transactions = mock(PlatformTransactionManager.class);
    private final ModuleDatabase database = mock(ModuleDatabase.class);
    private final PortalAccountService service = new PortalAccountService(students, guardians, families,
            mock(StudentMapper.class), mock(GuardianMapper.class), accounts, database, transactions,
            mock(OutboxWriter.class), mock(AuditWriter.class), Clock.systemUTC());

    @BeforeEach
    void transactionsRunTheWork() {
        when(database.readDefinition()).thenReturn(new DefaultTransactionDefinition());
        when(database.writeDefinition()).thenReturn(new DefaultTransactionDefinition());
        when(transactions.getTransaction(any())).thenAnswer(invocation -> new SimpleTransactionStatus(true));
    }

    private static StudentEntity student(UUID id, String status, UUID institution) {
        StudentEntity student = new StudentEntity();
        student.setId(id);
        student.setPersonId(UUID.randomUUID());
        student.setInstitutionId(institution);
        student.setAdmissionNumber("A-1");
        student.setStatus(status);
        student.setRowVersion(1);
        return student;
    }

    @Test
    void aStudentWhoLeftWhileIamWasAskedGetsNoAccount() {
        UUID id = UUID.randomUUID();
        when(students.findById(id)).thenReturn(Optional.of(student(id, "active", INSTITUTION)));
        when(accounts.open(any())).thenAnswer(invocation -> {
            when(students.findById(id)).thenReturn(Optional.of(student(id, "withdrawn", INSTITUTION)));
            return UUID.randomUUID();
        });

        assertThatThrownBy(() -> service.linkStudentAccount(SCHOOL, id)).isInstanceOf(InvalidStateException.class);
        verify(students, never()).linkAccount(any(), anyInt(), any(), any(), any());
    }

    @Test
    void aStudentMovedOutOfTheCallersScopeWhileIamWasAskedGetsNoAccount() {
        UUID id = UUID.randomUUID();
        when(students.findById(id)).thenReturn(Optional.of(student(id, "active", INSTITUTION)));
        when(accounts.open(any())).thenAnswer(invocation -> {
            when(students.findById(id)).thenReturn(Optional.of(student(id, "active", UUID.randomUUID())));
            return UUID.randomUUID();
        });

        assertThatThrownBy(() -> service.linkStudentAccount(SCHOOL, id))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(students, never()).linkAccount(any(), anyInt(), any(), any(), any());
    }

    @Test
    void aGuardianWhoseChildMovedOutOfTheCallersScopeGetsNoAccount() {
        UUID id = UUID.randomUUID();
        GuardianEntity guardian = new GuardianEntity();
        guardian.setId(id);
        guardian.setPersonId(UUID.randomUUID());
        guardian.setEmail("parent@example.test");
        guardian.setRowVersion(1);
        when(guardians.findById(id)).thenReturn(Optional.of(guardian));
        when(families.institutionsOfChildren(id)).thenReturn(List.of(INSTITUTION));
        when(accounts.open(any())).thenAnswer(invocation -> {
            when(families.institutionsOfChildren(id)).thenReturn(List.of(UUID.randomUUID()));
            return UUID.randomUUID();
        });

        assertThatThrownBy(() -> service.linkGuardianAccount(SCHOOL, id))
                .isInstanceOf(PermissionDeniedException.class);
        verify(guardians, never()).linkAccount(any(), anyInt(), any(), any(), any());
    }

    @Test
    void anInvitationIsRefusedBeforeAnythingIsAsked() {
        assertThatThrownBy(() -> service.createStudentPortalAccount(UUID.randomUUID(),
                new CreateStudentPortalAccountRequest().sendInvite(true)))
                .isInstanceOf(ValidationFailedException.class);
        assertThatThrownBy(() -> service.createGuardianPortalAccount(UUID.randomUUID(),
                new CreatePortalAccountRequest().sendInvite(true)))
                .isInstanceOf(ValidationFailedException.class);
        verify(accounts, never()).open(any());
    }
}
