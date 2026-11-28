package com.mysms.students.admission.business;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mysms.authclient.SiblingAccess;
import com.mysms.core.db.InProcessIdempotency;
import com.mysms.core.error.ApiException;
import com.mysms.core.error.DuplicateResourceException;
import com.mysms.core.error.FieldError;
import com.mysms.core.error.NotImplementedException;
import com.mysms.core.error.PermissionDeniedException;
import com.mysms.core.error.ResourceNotFoundException;
import com.mysms.core.error.UnavailableException;
import com.mysms.core.error.ValidationFailedException;
import com.mysms.core.paging.Page;
import com.mysms.core.testkit.GrpcRefusals;
import com.mysms.people.v1.CreatePersonRequest;
import com.mysms.people.v1.CreatePersonResponse;
import com.mysms.people.v1.Person;
import com.mysms.students.admission.domain.ApplicationEntity;
import com.mysms.students.admission.integration.PeopleGrpcClient;
import com.mysms.students.sis.api.model.CreatePortalAccountRequest;
import com.mysms.students.sis.api.model.CreateStudentPortalAccountRequest;
import com.mysms.students.sis.api.model.Guardian;
import com.mysms.students.sis.api.model.RelationshipType;
import com.mysms.students.sis.api.model.Student;
import com.mysms.students.sis.business.GuardianService;
import com.mysms.students.sis.business.PortalAccountService;
import com.mysms.students.sis.business.ProgramEnrollmentService;
import com.mysms.students.sis.business.RelationshipTypeService;
import com.mysms.students.sis.business.StudentEnrollmentService;
import com.mysms.students.sis.business.StudentService;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;

/**
 * How the people (gRPC) and sis (in process) answers become admission.EnrollApplicant's, over stand-ins: what is sent
 * for the applicant, that sis is asked only with the caller's permission and under the step's key, and that a refusal
 * keeps its meaning — 403, 409, 422 with the refused fields — while anything else is 503 and no message of another
 * service reaches the caller.
 */
class EnrollmentWriterTest {

    private final PeopleGrpcClient people = mock(PeopleGrpcClient.class);
    private final StudentService students = mock(StudentService.class);
    private final GuardianService guardians = mock(GuardianService.class);
    private final RelationshipTypeService relationshipTypes = mock(RelationshipTypeService.class);
    private final PortalAccountService portalAccounts = mock(PortalAccountService.class);
    private final SiblingAccess sibling = mock(SiblingAccess.class);
    private final InProcessIdempotency idempotency = mock(InProcessIdempotency.class);
    private final EnrollmentWriter writer = new EnrollmentWriter(people, students,
            mock(StudentEnrollmentService.class), mock(ProgramEnrollmentService.class), guardians, relationshipTypes,
            portalAccounts, sibling, idempotency);
    private final UUID key = UUID.randomUUID();

    @BeforeEach
    void callerMayAndKeysRunOnce() {
        when(sibling.permits(any())).thenReturn(true);
        when(idempotency.call(any(), any(), any(), any(), any()))
                .thenAnswer(call -> ((Supplier<?>) call.getArgument(4)).get());
    }

    private static ApplicationEntity applicant() {
        return ApplicationEntity.builder().institutionId(UUID.randomUUID()).applicationNumber("A-1")
                .fullName("Asha Rao").firstName("Asha").lastName("Rao").dateOfBirth(LocalDate.of(2019, 1, 1))
                .gender("female").bloodGroup("O+").mobile("+919800000001").phone("0802000000").build();
    }

    @Test
    void theApplicantsPersonIsMadeFromTheApplicationWithTheGivenKey() {
        UUID stored = UUID.randomUUID();
        when(people.createPerson(any())).thenReturn(CreatePersonResponse.newBuilder()
                .setPerson(Person.newBuilder().setId(stored.toString())).build());

        assertThat(writer.createApplicantPerson(applicant(), key)).isEqualTo(stored);

        ArgumentCaptor<CreatePersonRequest> sent = ArgumentCaptor.forClass(CreatePersonRequest.class);
        verify(people).createPerson(sent.capture());
        Person person = sent.getValue().getPerson();
        assertThat(sent.getValue().getRequestId()).isEqualTo(key.toString());
        assertThat(person.getDisplayName()).isEqualTo("Asha Rao");
        assertThat(person.getDateOfBirth()).isEqualTo("2019-01-01");
        assertThat(person.getGender()).isEqualTo(Person.Gender.GENDER_FEMALE);
        assertThat(person.getBloodGroup()).isEqualTo(Person.BloodGroup.BLOOD_GROUP_O_POSITIVE);
        assertThat(person.getPrimaryPhone()).as("the mobile first").isEqualTo("+919800000001");
        assertThat(person.getAlternatePhone()).isEqualTo("0802000000");
        assertThat(person.hasMiddleName()).as("an absent value is not sent").isFalse();
    }

    @Test
    void theStudentIsCreatedInSisUnderTheStepsKey() {
        UUID person = UUID.randomUUID();
        UUID created = UUID.randomUUID();
        ApplicationEntity application = applicant();
        when(students.createStudent(any(), eq(false))).thenAnswer(call -> ((Student) call.getArgument(0)).id(created));

        EnrollmentWriter.StudentRef student = writer.createStudent(person, application, LocalDate.of(2026, 4, 1), key);

        assertThat(student).isEqualTo(new EnrollmentWriter.StudentRef(created, application.getInstitutionId(), person));
        ArgumentCaptor<Student> sent = ArgumentCaptor.forClass(Student.class);
        verify(idempotency).call(eq("sis.CreateStudent"), eq(key), sent.capture(), eq(Student.class), any());
        assertThat(sent.getValue().getAdmissionNumber()).isEqualTo("A-1");
        assertThat(sent.getValue().getAdmissionDate()).isEqualTo(LocalDate.of(2026, 4, 1));
    }

    @Test
    void aStudentThatIsNotThereIsEmptyAndAReadWithoutThePermissionIsRefused() {
        UUID id = UUID.randomUUID();
        when(students.getStudent(id)).thenThrow(new ResourceNotFoundException("sis.student", id));
        assertThat(writer.findStudent(id)).isEmpty();

        UUID hidden = UUID.randomUUID();
        when(sibling.permits("sis.student.read")).thenReturn(false);
        assertThatThrownBy(() -> writer.findStudent(hidden)).isInstanceOf(PermissionDeniedException.class)
                .hasMessageContaining("sis.student.read");
        verify(students, never()).getStudent(hidden);
    }

    @Test
    void aRefusedCreateKeepsItsMeaningAndNamesTheRefusedFields() {
        when(people.createPerson(any())).thenThrow(GrpcRefusals.refused("people", HttpStatus.UNPROCESSABLE_CONTENT,
                "VALIDATION_FAILED", new FieldError("dateOfBirth", "secret detail", "Past")));
        assertThatThrownBy(() -> writer.createApplicantPerson(applicant(), key))
                .isInstanceOfSatisfying(ValidationFailedException.class, refused -> {
                    assertThat(refused.errors()).singleElement()
                            .satisfies(error -> assertThat(error.field()).isEqualTo("dateOfBirth"));
                    assertThat(refused.errors().get(0).message()).doesNotContain("secret");
                });

        UUID person = UUID.randomUUID();
        when(students.createStudent(any(), anyBoolean())).thenThrow(new DuplicateResourceException("secret"));
        assertThatThrownBy(() -> writer.createStudent(person, applicant(), LocalDate.now(), key))
                .isInstanceOf(DuplicateResourceException.class).hasMessageNotContaining("secret");

        doThrow(new ValidationFailedException(List.of(new FieldError("admissionNumber", "secret detail", "Size"))))
                .when(students).createStudent(any(), anyBoolean());
        assertThatThrownBy(() -> writer.createStudent(person, applicant(), LocalDate.now(), key))
                .isInstanceOfSatisfying(ValidationFailedException.class, refused -> assertThat(refused.errors())
                        .singleElement().satisfies(error -> {
                            assertThat(error.field()).isEqualTo("admissionNumber");
                            assertThat(error.message()).doesNotContain("secret");
                        }));
    }

    @Test
    void anythingElseIsTheOtherServiceUnavailable() {
        when(people.createPerson(any())).thenThrow(GrpcRefusals.refused("people",
                HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL"));
        assertThatThrownBy(() -> writer.createApplicantPerson(applicant(), key))
                .isInstanceOf(UnavailableException.class).hasMessageNotContaining("own sentence");

        UUID id = UUID.randomUUID();
        when(students.getStudent(id)).thenThrow(new UnavailableException("sis's own sentence"));
        assertThatThrownBy(() -> writer.findStudent(id)).isInstanceOf(UnavailableException.class)
                .hasMessage("The sis service could not answer.");

        UUID student = UUID.randomUUID();
        when(portalAccounts.createStudentPortalAccount(eq(student), any())).thenThrow(new NotImplementedException());
        assertThatThrownBy(() -> writer.createStudentPortalAccount(student, key))
                .isInstanceOf(EnrollmentWriter.NotServedException.class)
                .satisfies(e -> assertThat(((ApiException) e).status()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE));
    }

    @Test
    void aParentSisKnowsIsFoundByEmailThenByMobile() {
        UUID known = UUID.randomUUID();
        when(guardians.listGuardians(eq(1), eq(1), isNull(), any(), isNull())).thenAnswer(call ->
                new Page<>("mobile = \"+919800000001\"".equals(call.getArgument(3))
                        ? List.of(new Guardian(UUID.randomUUID()).id(known)) : List.of(), 1, 1, ""));
        assertThat(writer.findGuardian(" meera@example.com ", "+919800000001")).contains(known);
        assertThat(writer.findGuardian(null, " ")).as("nothing to look a parent up by").isEmpty();
    }

    @Test
    void relationshipTypesAreReadOnceAndNamedByCodeAndName() {
        UUID mother = UUID.randomUUID();
        when(relationshipTypes.listRelationshipTypes(1, EnrollmentWriter.PAGE_SIZE, null, null, null)).thenReturn(
                new Page<>(List.of(new RelationshipType("Mother").id(mother).code("MOTHER")), 1, 1, ""));
        assertThat(writer.relationshipTypes()).containsEntry("mother", mother);
        verify(relationshipTypes, times(1)).listRelationshipTypes(any(), any(), any(), any(), any());
    }

    @Test
    void portalAccountsAreAskedForWithoutAnInvitationUnderTheStepsKey() {
        UUID student = UUID.randomUUID();
        writer.createStudentPortalAccount(student, key);
        ArgumentCaptor<CreateStudentPortalAccountRequest> sent =
                ArgumentCaptor.forClass(CreateStudentPortalAccountRequest.class);
        verify(portalAccounts).createStudentPortalAccount(eq(student), sent.capture());
        assertThat(sent.getValue().getSendInvite()).isFalse();
        verify(idempotency).call(eq("sis.CreateStudentPortalAccount"), eq(key), any(), eq(Student.class), any());

        UUID guardian = UUID.randomUUID();
        writer.createGuardianPortalAccount(guardian, key);
        ArgumentCaptor<CreatePortalAccountRequest> asked = ArgumentCaptor.forClass(CreatePortalAccountRequest.class);
        verify(portalAccounts).createGuardianPortalAccount(eq(guardian), asked.capture());
        assertThat(asked.getValue().getSendInvite()).isFalse();
        verify(idempotency).call(eq("sis.CreatePortalAccount"), eq(key), any(), eq(Guardian.class), any());
    }
}
