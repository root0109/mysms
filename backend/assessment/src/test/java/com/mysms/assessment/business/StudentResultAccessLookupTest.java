package com.mysms.assessment.business;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.mysms.authclient.AuthContext;
import com.mysms.authclient.AuthFilter;
import com.mysms.authclient.OperationTable;
import com.mysms.authclient.VerifiedToken;
import com.mysms.core.error.ProblemResponseWriter;
import com.mysms.sdk.sis.api.GuardianApi;
import com.mysms.sdk.sis.api.StudentApi;
import com.mysms.sdk.sis.model.ChildCard;
import com.mysms.sdk.sis.model.NavigationInfo;
import com.mysms.sdk.sis.model.PagingInfoChildCard;
import com.mysms.sdk.sis.model.Student;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import tools.jackson.databind.json.JsonMapper;

class StudentResultAccessLookupTest {

    @Test
    void guardianMayReadAChildWithoutStudentReadPermission() throws Exception {
        UUID studentId = UUID.randomUUID();
        Student child = new Student(UUID.randomUUID(), UUID.randomUUID()).id(studentId);
        ChildCard childCard = new ChildCard();
        childCard.setStudent(child);
        PagingInfoChildCard children = new PagingInfoChildCard(List.of(childCard),
                new NavigationInfo(1, 1, "/v1/sis/guardians/me/children?page=1&size=500"));
        GuardianApi guardians = mock(GuardianApi.class);
        StudentApi students = mock(StudentApi.class);
        when(guardians.listMyChildren(1, 500, null)).thenReturn(ResponseEntity.ok(children));
        StudentResultAccessLookup lookup = new StudentResultAccessLookup(students, guardians);
        UUID userId = UUID.randomUUID();
        AuthContext caller = new AuthContext(userId, Optional.of(UUID.randomUUID()), Set.of(), Optional.empty(),
                Set.of("assessment.marksheet_line.get_student_results", "sis.guardian.list_my_children"),
                "test-jti", Optional.empty(), false, Optional.of(UUID.randomUUID()), Optional.empty(), false);
        VerifiedToken token = new VerifiedToken(caller, Instant.now().plusSeconds(60), Set.of("mysms"), Set.of("pwd"));
        AuthFilter filter = new AuthFilter(OperationTable.load(List.of("assessment")), ignored -> token,
                (context, permission) -> context.hasPermission(permission),
                new ProblemResponseWriter(JsonMapper.builder().build()), List.of());
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod("GET");
        request.setRequestURI("/v1/assessment/students/" + studentId + "/results");
        request.addHeader("Authorization", "Bearer unit-test-token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (servletRequest, servletResponse) ->
                assertThat(lookup.isStudentOrGuardian(studentId)).isTrue());

        assertThat(response.getStatus()).isEqualTo(200);
        verify(guardians).listMyChildren(1, 500, null);
        verifyNoInteractions(students);
    }
}