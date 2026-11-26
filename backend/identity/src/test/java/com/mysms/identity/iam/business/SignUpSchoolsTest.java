package com.mysms.identity.iam.business;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mysms.common.v1.Pagination;
import com.mysms.core.error.UnavailableException;
import com.mysms.core.testkit.GrpcRefusals;
import com.mysms.identity.iam.api.model.SignUpInstitution;
import com.mysms.identity.iam.integration.OrgGrpcClient;
import com.mysms.org.v1.Institution;
import com.mysms.org.v1.ListActiveInstitutionsRequest;
import com.mysms.org.v1.ListActiveInstitutionsResponse;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;

/** The schools sign-up offers and accepts, against a stand-in of the org client: paging, the id check, failures. */
class SignUpSchoolsTest {

    private final OrgGrpcClient org = mock(OrgGrpcClient.class);
    private final SignUpSchools schools = new SignUpSchools(org);

    private static ListActiveInstitutionsResponse page(int next, String cursor, Institution... items) {
        return ListActiveInstitutionsResponse.newBuilder().addAllItems(List.of(items))
                .setPagination(Pagination.newBuilder().setNext(next).setNextCursor(cursor)).build();
    }

    private static Institution school(UUID id, String name) {
        return Institution.newBuilder().setId(id.toString()).setName(name).build();
    }

    @Test
    void everyPageOfActiveSchoolsIsReadAsIdAndName() {
        UUID alpha = UUID.randomUUID();
        UUID beta = UUID.randomUUID();
        when(org.listActiveInstitutions(any())).thenReturn(page(2, "next-cursor", school(alpha, "Alpha")),
                page(0, "", school(beta, "Beta")));

        assertThat(schools.all()).containsExactly(new SignUpInstitution(alpha, "Alpha"),
                new SignUpInstitution(beta, "Beta"));
        ArgumentCaptor<ListActiveInstitutionsRequest> asked = ArgumentCaptor.forClass(
                ListActiveInstitutionsRequest.class);
        verify(org, times(2)).listActiveInstitutions(asked.capture());
        assertThat(asked.getAllValues().get(0).getPageable().getSize()).isEqualTo(SignUpSchools.PAGE_SIZE);
        assertThat(asked.getAllValues().get(1).getPageable().getPage()).isEqualTo(2);
        assertThat(asked.getAllValues().get(1).getPageable().getCursor()).isEqualTo("next-cursor");
    }

    @Test
    void aSchoolIsActiveOnlyWhenOrgListsItForTheTenant() {
        UUID school = UUID.randomUUID();
        when(org.listActiveInstitutions(any())).thenReturn(page(0, "", school(school, "Alpha")), page(0, ""));

        assertThat(schools.isActive(school)).isTrue();
        assertThat(schools.isActive(school)).as("unknown, inactive or another tenant's").isFalse();
        ArgumentCaptor<ListActiveInstitutionsRequest> asked = ArgumentCaptor.forClass(
                ListActiveInstitutionsRequest.class);
        verify(org, times(2)).listActiveInstitutions(asked.capture());
        assertThat(asked.getValue().getFilter()).isEqualTo("id = \"" + school + "\"");
    }

    @Test
    void anOrgThatCannotAnswerOrRefusesIsUnavailable() {
        when(org.listActiveInstitutions(any())).thenThrow(GrpcRefusals.unreachable("org"))
                .thenThrow(GrpcRefusals.refused("org", HttpStatus.FORBIDDEN, "PERMISSION_DENIED"));

        assertThatThrownBy(schools::all).isInstanceOf(UnavailableException.class);
        assertThatThrownBy(() -> schools.isActive(UUID.randomUUID())).isInstanceOf(UnavailableException.class)
                .hasMessageNotContaining("org service's own sentence");
    }
}
