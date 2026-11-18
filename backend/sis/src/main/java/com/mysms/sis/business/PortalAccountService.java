package com.mysms.sis.business;

import com.mysms.authclient.DataScope;
import com.mysms.core.context.Actor;
import com.mysms.core.db.AppTransactionManager;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.error.FieldError;
import com.mysms.core.error.InvalidStateException;
import com.mysms.core.error.PreconditionFailedException;
import com.mysms.core.error.ResourceNotFoundException;
import com.mysms.core.error.ValidationFailedException;
import com.mysms.core.outbox.AuditWriter;
import com.mysms.core.outbox.OutboxWriter;
import com.mysms.sis.api.model.CreatePortalAccountRequest;
import com.mysms.sis.api.model.CreateStudentPortalAccountRequest;
import com.mysms.sis.api.model.Guardian;
import com.mysms.sis.api.model.Student;
import com.mysms.sis.config.SisDatabaseConfig;
import com.mysms.sis.config.SisEventsConfig;
import com.mysms.sis.domain.GuardianEntity;
import com.mysms.sis.domain.StudentEntity;
import com.mysms.sis.events.SisEvents;
import com.mysms.sis.mapper.GuardianMapper;
import com.mysms.sis.mapper.StudentMapper;
import com.mysms.sis.repository.FamilyLinkRepository;
import com.mysms.sis.repository.GuardianRepository;
import com.mysms.sis.repository.StudentRepository;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * sis.CreateStudentPortalAccount and sis.CreatePortalAccount (a guardian's): the login account of a student or a
 * guardian, opened in iam with the caller's own token, then linked on the sis row ({@code user_id}).
 *
 * <p>Each runs in three steps so that no database transaction waits on iam: the row is checked (visible 404,
 * writable 403, no account yet and the details an account needs, 409), the account is opened in iam
 * ({@link PortalAccountStore}: the person's existing account is reused, the persona's role is granted in the row's
 * institutions), and the row is linked — only while it still has no account, so two concurrent requests link the
 * same account once, and a retry after a failed link finds the account iam already has. A student's change is
 * announced as {@code sis.student.updated} with the member payload, from which iam keeps the account's records.
 *
 * <p>{@code sendInvite: true} is refused (422), as hr refuses it: iam sends no invitations yet (message delivery is
 * not built), so an administrator sets the first password.
 */
@Service
public class PortalAccountService extends AppTransactionManager {

    static final String PERMISSION_STUDENT = "sis.student.create_student_portal_account";
    static final String PERMISSION_GUARDIAN = "sis.guardian.create_portal_account";

    private final StudentRepository students;
    private final GuardianRepository guardians;
    private final FamilyLinkRepository families;
    private final StudentMapper studentMapper;
    private final GuardianMapper guardianMapper;
    private final PortalAccountStore accounts;
    private final OutboxWriter outbox;
    private final AuditWriter audit;
    private final Clock clock;

    public PortalAccountService(StudentRepository students, GuardianRepository guardians,
            FamilyLinkRepository families, StudentMapper studentMapper, GuardianMapper guardianMapper,
            PortalAccountStore accounts, @Qualifier(SisDatabaseConfig.DATABASE) ModuleDatabase database,
            @Qualifier(SisDatabaseConfig.TRANSACTIONS) PlatformTransactionManager transactions,
            @Qualifier(SisEventsConfig.OUTBOX) OutboxWriter outbox,
            @Qualifier(SisEventsConfig.AUDIT) AuditWriter audit, Clock clock) {
        super(database, transactions);
        this.students = students;
        this.guardians = guardians;
        this.families = families;
        this.studentMapper = studentMapper;
        this.guardianMapper = guardianMapper;
        this.accounts = accounts;
        this.outbox = outbox;
        this.audit = audit;
        this.clock = clock;
    }

    /**
     * The student's account: signs in with the admission number (409 for a student without one), carries the
     * student role in the student's institution. Only an active student without an account gets one (409).
     */
    public Student createStudentPortalAccount(UUID id, CreateStudentPortalAccountRequest request) {
        refuseInvite(request == null ? null : request.getSendInvite());
        return linkStudentAccount(DataScope.current(PERMISSION_STUDENT), id);
    }

    /** The student's account, for a caller of the given scope: checked, opened in iam, checked again, linked. */
    Student linkStudentAccount(DataScope scope, UUID id) {
        PortalAccountStore.Request opening = doInPrimaryReadTransaction(() -> {
            StudentEntity student = eligibleStudent(scope, id);
            if (student.getUserId() != null) {
                throw new InvalidStateException("The student already has a portal account.");
            }
            return new PortalAccountStore.Request(student.getPersonId(), PortalAccountStore.STUDENT,
                    student.getAdmissionNumber().strip(), null, null, PortalAccountStore.STUDENT_ROLE,
                    List.of(student.getInstitutionId()));
        });
        UUID account = accounts.open(opening);
        return doInWriteTransaction(() -> {
            // the student may have left, or moved out of the caller's scope, while iam was asked
            StudentEntity student = eligibleStudent(scope, id);
            if (!account.equals(student.getUserId())) {
                requireUnlinked(student.getUserId());
                if (students.linkAccount(id, student.getRowVersion(), account, now(),
                        Actor.current().orElse(null)) == 0) {
                    throw new PreconditionFailedException("The student changed meanwhile; try again.");
                }
                student = students.findById(id).orElseThrow();
                audit.recordUpdate(StudentService.AGGREGATE, id, List.of("userId"));
                outbox.publish(StudentService.AGGREGATE + SisEvents.UPDATED, StudentService.AGGREGATE, id,
                        StudentService.memberPayload(student));
            }
            return studentMapper.toDto(student);
        });
    }

    /**
     * The student, if an account may be opened or linked for them now: visible (404) and writable (403) for the
     * caller, active, with an admission number to sign in with (409). Checked before iam is asked and again on the
     * row the link is written to.
     */
    private StudentEntity eligibleStudent(DataScope scope, UUID id) {
        StudentEntity student = students.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(StudentService.AGGREGATE, id));
        Optional<UUID> institution = Optional.ofNullable(student.getInstitutionId());
        if (!scope.canRead(institution)) {
            throw new ResourceNotFoundException(StudentService.AGGREGATE, id);
        }
        scope.requireWritable(institution);
        if (!"active".equals(student.getStatus())) {
            throw new InvalidStateException("Only an active student gets a portal account.");
        }
        if (student.getAdmissionNumber() == null || student.getAdmissionNumber().isBlank()) {
            throw new InvalidStateException("The student signs in with the admission number; record it first.");
        }
        return student;
    }

    /**
     * The guardian's account: signs in with the guardian's e-mail address, else the mobile number (409 without
     * either), and carries the guardian role in each institution of the guardian's children (an active link). A
     * guardian is a tenant-level record, so the caller must be able to write in every one of those institutions
     * (403 otherwise), and a guardian linked to no student gets no account (409).
     */
    public Guardian createGuardianPortalAccount(UUID id, CreatePortalAccountRequest request) {
        refuseInvite(request == null ? null : request.getSendInvite());
        return linkGuardianAccount(DataScope.current(PERMISSION_GUARDIAN), id);
    }

    /** The guardian's account, for a caller of the given scope: checked, opened in iam, checked again, linked. */
    Guardian linkGuardianAccount(DataScope scope, UUID id) {
        PortalAccountStore.Request opening = doInPrimaryReadTransaction(() -> {
            GuardianEntity guardian = guardians.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException(GuardianService.AGGREGATE, id));
            if (guardian.getUserId() != null) {
                throw new InvalidStateException("The guardian already has a portal account.");
            }
            Collection<UUID> institutions = childrensInstitutions(scope, id);
            String email = blankToNull(guardian.getEmail());
            String mobile = blankToNull(guardian.getMobile());
            if (email == null && mobile == null) {
                throw new InvalidStateException(
                        "The guardian signs in with an e-mail address or mobile number; record one first.");
            }
            return new PortalAccountStore.Request(guardian.getPersonId(), PortalAccountStore.GUARDIAN,
                    email != null ? email : mobile, email, mobile, PortalAccountStore.GUARDIAN_ROLE, institutions);
        });
        UUID account = accounts.open(opening);
        return doInWriteTransaction(() -> {
            GuardianEntity guardian = guardians.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException(GuardianService.AGGREGATE, id));
            // the guardian's children may have changed, or left the caller's scope, while iam was asked
            childrensInstitutions(scope, id);
            if (!account.equals(guardian.getUserId())) {
                requireUnlinked(guardian.getUserId());
                if (guardians.linkAccount(id, guardian.getRowVersion(), account, now(),
                        Actor.current().orElse(null)) == 0) {
                    throw new PreconditionFailedException("The guardian changed meanwhile; try again.");
                }
                guardian = guardians.findById(id).orElseThrow();
                audit.recordUpdate(GuardianService.AGGREGATE, id, List.of("userId"));
                outbox.publish(GuardianService.AGGREGATE + SisEvents.UPDATED, GuardianService.AGGREGATE, id,
                        SisEvents.payload(id));
            }
            return guardianMapper.toDto(guardian);
        });
    }

    /**
     * The institutions of the guardian's children (an active link), every one of which the caller must be able to
     * write in (403); a guardian of no student gets no account (409). Checked before iam is asked and again when
     * the account is linked.
     */
    private Collection<UUID> childrensInstitutions(DataScope scope, UUID guardianId) {
        Collection<UUID> institutions = families.institutionsOfChildren(guardianId);
        if (institutions.isEmpty()) {
            throw new InvalidStateException("The guardian is not linked to any student yet.");
        }
        institutions.forEach(institution -> scope.requireWritable(Optional.of(institution)));
        return institutions;
    }

    /** iam sends no invitations yet, so asking for one is refused (422) rather than silently dropped, as in hr. */
    private static void refuseInvite(Boolean sendInvite) {
        if (Boolean.TRUE.equals(sendInvite)) {
            throw new ValidationFailedException(List.of(new FieldError("sendInvite",
                    "invitations cannot be sent yet; set the first password in user administration",
                    "NOT_SUPPORTED")));
        }
    }

    /** Another account was linked between the check and the link (a concurrent request with other details). */
    private static void requireUnlinked(UUID current) {
        if (current != null) {
            throw new InvalidStateException("Another portal account was linked meanwhile.");
        }
    }

    private OffsetDateTime now() {
        return OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC).truncatedTo(ChronoUnit.MICROS);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }
}
