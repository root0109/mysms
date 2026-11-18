package com.mysms.assessment.business;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mysms.assessment.business.StudentOwnershipLookup.Relation;
import com.mysms.assessment.integration.IamGrpcClient;
import com.mysms.assessment.integration.SisGrpcClient;
import com.mysms.authclient.AuthContext;
import com.mysms.core.error.UnavailableException;
import com.mysms.core.testkit.GrpcRefusals;
import com.mysms.iam.v1.GetMeResponse;
import com.mysms.iam.v1.Me;
import com.mysms.sis.v1.ChildCard;
import com.mysms.sis.v1.GetStudentSummaryRequest;
import com.mysms.sis.v1.GetStudentSummaryResponse;
import com.mysms.sis.v1.ListMyChildrenResponse;
import com.mysms.sis.v1.Student;
import com.mysms.sis.v1.StudentSummary;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

/** How sis's and iam's answers become the caller's relation to a student (mocks stand in for the two clients). */
class StudentOwnershipLookupTest {

    private final UUID user = UUID.randomUUID();
    private final UUID person = UUID.randomUUID();
    private final UUID studentId = UUID.randomUUID();
    private SisGrpcClient sis;
    private IamGrpcClient iam;
    private StudentOwnershipLookup ownership;
    private AuthContext caller;

    @BeforeEach
    void services() {
        sis = mock(SisGrpcClient.class);
        iam = mock(IamGrpcClient.class);
        ownership = new StudentOwnershipLookup(sis, iam);
        caller = new AuthContext(user, Optional.of(UUID.randomUUID()), Set.of(), Optional.empty(),
                Set.of("assessment.marksheet_line.get_student_results"), "jti", Optional.empty(), false,
                Optional.of(person), Optional.empty(), false);
        doThrow(GrpcRefusals.refused("sis", HttpStatus.FORBIDDEN, "PERMISSION_DENIED")).when(sis).listMyChildren(any());
    }

    @Test
    void theStudentsOwnAccountOrPersonMakesThemTheStudent() {
        summary(student(studentId, UUID.randomUUID()).setUserId(user.toString()));
        assertThat(ownership.relationTo(caller, studentId)).contains(Relation.SELF);

        summary(student(studentId, person));
        assertThat(ownership.relationTo(caller, studentId)).as("matched by the linked person").contains(Relation.SELF);
    }

    @Test
    void aStudentSisShowsToStaffOnlyIsNotTheCallersOwn() {
        summary(student(studentId, UUID.randomUUID()).setUserId(UUID.randomUUID().toString()));
        assertThat(ownership.relationTo(caller, studentId)).isEmpty();

        summary(student(UUID.randomUUID(), UUID.randomUUID()).setUserId(user.toString()));
        assertThat(ownership.relationTo(caller, studentId)).as("another student's record").isEmpty();
    }

    @Test
    void aChildOfTheCallerMakesThemAGuardian() {
        doThrow(GrpcRefusals.refused("sis", HttpStatus.NOT_FOUND, "NOT_FOUND")).when(sis).getStudentSummary(any());
        doReturn(children(studentId)).when(sis).listMyChildren(any());
        assertThat(ownership.relationTo(caller, studentId)).contains(Relation.GUARDIAN);

        doReturn(children(UUID.randomUUID())).when(sis).listMyChildren(any());
        assertThat(ownership.relationTo(caller, studentId)).as("someone else's child").isEmpty();
    }

    @Test
    void whatSisCannotAnswerIsUnavailableNeverAGuess() {
        doThrow(GrpcRefusals.refused("sis", HttpStatus.BAD_GATEWAY, "INTERNAL")).when(sis).getStudentSummary(any());
        assertThatThrownBy(() -> ownership.relationTo(caller, studentId)).isInstanceOf(UnavailableException.class);

        doThrow(GrpcRefusals.unreachable("sis")).when(sis).getStudentSummary(any());
        assertThatThrownBy(() -> ownership.relationTo(caller, studentId)).isInstanceOf(UnavailableException.class);

        doReturn(children()).when(sis).listMyChildren(any());
        doThrow(GrpcRefusals.unreachable("iam")).when(iam).getMe(any());
        assertThatThrownBy(() -> ownership.ownStudents(caller))
                .as("the caller's own records need iam").isInstanceOf(UnavailableException.class);
    }

    @Test
    void ownStudentsAreTheChildrenAndTheRecordsIamNamesThatSisConfirms() {
        UUID child = UUID.randomUUID();
        UUID unconfirmed = UUID.randomUUID();
        doReturn(children(child)).when(sis).listMyChildren(any());
        when(iam.getMe(any())).thenReturn(GetMeResponse.newBuilder().setResult(Me.newBuilder()
                .addStudentIds(studentId.toString()).addStudentIds(unconfirmed.toString())
                .addStudentIds(child.toString())).build());
        summary(student(studentId, person));
        doThrow(GrpcRefusals.refused("sis", HttpStatus.NOT_FOUND, "NOT_FOUND")).when(sis).getStudentSummary(
                GetStudentSummaryRequest.newBuilder().setStudentId(unconfirmed.toString()).build());

        assertThat(ownership.ownStudents(caller)).containsExactlyInAnyOrder(child, studentId);
        verify(sis, never()).getStudentSummary(
                GetStudentSummaryRequest.newBuilder().setStudentId(child.toString()).build());
    }

    private static Student.Builder student(UUID id, UUID personId) {
        return Student.newBuilder().setId(id.toString()).setPersonId(personId.toString())
                .setInstitutionId(UUID.randomUUID().toString());
    }

    private void summary(Student.Builder student) {
        doReturn(GetStudentSummaryResponse.newBuilder().setResult(StudentSummary.newBuilder().setStudent(student))
                .build()).when(sis).getStudentSummary(
                        GetStudentSummaryRequest.newBuilder().setStudentId(studentId.toString()).build());
    }

    private static ListMyChildrenResponse children(UUID... ids) {
        ListMyChildrenResponse.Builder page = ListMyChildrenResponse.newBuilder();
        for (UUID id : ids) {
            page.addItems(ChildCard.newBuilder().setStudent(student(id, UUID.randomUUID())));
        }
        return page.build();
    }
}
