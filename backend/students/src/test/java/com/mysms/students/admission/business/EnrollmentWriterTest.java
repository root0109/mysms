package com.mysms.students.admission.business;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mysms.students.admission.domain.ApplicationEntity;
import com.mysms.students.admission.integration.PeopleGrpcClient;
import com.mysms.students.admission.integration.SisGrpcClient;
import com.mysms.core.error.ApiException;
import com.mysms.core.error.DuplicateResourceException;
import com.mysms.core.error.FieldError;
import com.mysms.core.error.PermissionDeniedException;
import com.mysms.core.error.UnavailableException;
import com.mysms.core.error.ValidationFailedException;
import com.mysms.core.testkit.GrpcRefusals;
import com.mysms.people.v1.CreatePersonRequest;
import com.mysms.people.v1.CreatePersonResponse;
import com.mysms.people.v1.Person;
import com.mysms.sis.v1.CreatePortalAccountRequest;
import com.mysms.sis.v1.CreatePortalAccountResponse;
import com.mysms.sis.v1.CreateStudentPortalAccountRequest;
import com.mysms.sis.v1.CreateStudentPortalAccountResponse;
import com.mysms.sis.v1.GetStudentRequest;
import com.mysms.sis.v1.Guardian;
import com.mysms.sis.v1.ListGuardiansRequest;
import com.mysms.sis.v1.ListGuardiansResponse;
import com.mysms.sis.v1.ListRelationshipTypesResponse;
import com.mysms.sis.v1.RelationshipType;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;

/**
 * How the people and sis answers become admission.EnrollApplicant's, over stand-ins of the gRPC clients: what is sent
 * for the applicant, and that a refusal keeps its meaning — 403, 409, 422 with the refused fields — while anything else
 * is 503 and no message of another service reaches the caller.
 */
class EnrollmentWriterTest {

    private final PeopleGrpcClient people = mock(PeopleGrpcClient.class);
    private final SisGrpcClient sis = mock(SisGrpcClient.class);
    private final EnrollmentWriter writer = new EnrollmentWriter(people, sis);
    private final UUID key = UUID.randomUUID();

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
    void aStudentThatIsNotThereIsEmptyAndARefusedReadIsTheCallersMissingPermission() {
        UUID id = UUID.randomUUID();
        when(sis.getStudent(GetStudentRequest.newBuilder().setId(id.toString()).build()))
                .thenThrow(GrpcRefusals.refused("sis", HttpStatus.NOT_FOUND, "NOT_FOUND"));
        assertThat(writer.findStudent(id)).isEmpty();

        UUID hidden = UUID.randomUUID();
        when(sis.getStudent(GetStudentRequest.newBuilder().setId(hidden.toString()).build()))
                .thenThrow(GrpcRefusals.refused("sis", HttpStatus.FORBIDDEN, "PERMISSION_DENIED"));
        assertThatThrownBy(() -> writer.findStudent(hidden)).isInstanceOf(PermissionDeniedException.class)
                .hasMessageContaining("sis.student.read");
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
        when(sis.createStudent(any())).thenThrow(GrpcRefusals.refused("sis", HttpStatus.CONFLICT, "DUPLICATE"));
        assertThatThrownBy(() -> writer.createStudent(person, applicant(), LocalDate.now(), key))
                .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    void anythingElseIsTheOtherServiceUnavailable() {
        when(people.createPerson(any())).thenThrow(GrpcRefusals.refused("people",
                HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL"));
        assertThatThrownBy(() -> writer.createApplicantPerson(applicant(), key))
                .isInstanceOf(UnavailableException.class).hasMessageNotContaining("own sentence");

        UUID id = UUID.randomUUID();
        when(sis.getStudent(any())).thenThrow(GrpcRefusals.unreachable("sis"));
        assertThatThrownBy(() -> writer.findStudent(id)).isInstanceOf(UnavailableException.class);

        UUID student = UUID.randomUUID();
        when(sis.createStudentPortalAccount(any())).thenThrow(GrpcRefusals.refused("sis",
                HttpStatus.NOT_IMPLEMENTED, "NOT_IMPLEMENTED"));
        assertThatThrownBy(() -> writer.createStudentPortalAccount(student, key))
                .isInstanceOf(EnrollmentWriter.NotServedException.class)
                .satisfies(e -> assertThat(((ApiException) e).status()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE));
    }

    @Test
    void aParentSisKnowsIsFoundByEmailThenByMobile() {
        UUID known = UUID.randomUUID();
        when(sis.listGuardians(any())).thenAnswer(call -> {
            ListGuardiansRequest request = call.getArgument(0);
            ListGuardiansResponse.Builder page = ListGuardiansResponse.newBuilder();
            if (request.getFilter().equals("mobile = \"+919800000001\"")) {
                page.addItems(Guardian.newBuilder().setId(known.toString()));
            }
            return page.build();
        });
        assertThat(writer.findGuardian(" meera@example.com ", "+919800000001")).contains(known);
        assertThat(writer.findGuardian(null, " ")).as("nothing to look a parent up by").isEmpty();
    }

    @Test
    void relationshipTypesAreReadOnceAndNamedByCodeAndName() {
        UUID mother = UUID.randomUUID();
        when(sis.listRelationshipTypes(any())).thenReturn(ListRelationshipTypesResponse.newBuilder()
                .addItems(RelationshipType.newBuilder().setId(mother.toString()).setName("Mother").setCode("MOTHER"))
                .build());
        assertThat(writer.relationshipTypes()).containsEntry("mother", mother);
        verify(sis, times(1)).listRelationshipTypes(any());
    }

    @Test
    void portalAccountsAreAskedForWithoutAnInvitation() {
        UUID student = UUID.randomUUID();
        when(sis.createStudentPortalAccount(any())).thenReturn(CreateStudentPortalAccountResponse.getDefaultInstance());
        writer.createStudentPortalAccount(student, key);
        ArgumentCaptor<CreateStudentPortalAccountRequest> sent =
                ArgumentCaptor.forClass(CreateStudentPortalAccountRequest.class);
        verify(sis).createStudentPortalAccount(sent.capture());
        assertThat(sent.getValue().getStudentId()).isEqualTo(student.toString());
        assertThat(sent.getValue().getSendInvite()).isFalse();
        assertThat(sent.getValue().getRequestId()).isEqualTo(key.toString());

        UUID guardian = UUID.randomUUID();
        when(sis.createPortalAccount(any())).thenReturn(CreatePortalAccountResponse.getDefaultInstance());
        writer.createGuardianPortalAccount(guardian, key);
        ArgumentCaptor<CreatePortalAccountRequest> asked = ArgumentCaptor.forClass(CreatePortalAccountRequest.class);
        verify(sis).createPortalAccount(asked.capture());
        assertThat(asked.getValue().getGuardianId()).isEqualTo(guardian.toString());
        assertThat(asked.getValue().hasSendInvite()).isTrue();
        assertThat(asked.getValue().getSendInvite()).isFalse();
    }
}
