package com.mysms.identity.iam.business;

import com.mysms.common.v1.PageableRequest;
import com.mysms.core.error.UnavailableException;
import com.mysms.core.grpc.IntegrationGrpcClientException;
import com.mysms.identity.iam.api.model.SignUpInstitution;
import com.mysms.identity.iam.integration.OrgGrpcClient;
import com.mysms.org.v1.Institution;
import com.mysms.org.v1.ListActiveInstitutionsRequest;
import com.mysms.org.v1.ListActiveInstitutionsResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * The schools self sign-up offers and accepts: the active institutions of the current call's tenant, read from org.
 * Called outside any transaction; an org that cannot answer is 503.
 */
@Component
public class SignUpSchools {

    static final int PAGE_SIZE = 500;
    private static final String UNAVAILABLE = "The school list could not be read; try again later.";

    private final OrgGrpcClient org;

    public SignUpSchools(OrgGrpcClient org) {
        this.org = org;
    }

    /** Every active institution of the tenant, by name. */
    public List<SignUpInstitution> all() {
        List<SignUpInstitution> schools = new ArrayList<>();
        PageableRequest.Builder page = PageableRequest.newBuilder().setPage(1).setSize(PAGE_SIZE);
        while (true) {
            ListActiveInstitutionsResponse answer = read(ListActiveInstitutionsRequest.newBuilder().setPageable(page)
                    .build());
            answer.getItemsList().forEach(i -> schools.add(new SignUpInstitution(UUID.fromString(i.getId()),
                    i.getName())));
            if (answer.getPagination().getNext() == 0) {
                return schools;
            }
            page.setPage(answer.getPagination().getNext()).setCursor(answer.getPagination().getNextCursor());
        }
    }

    /** Whether the institution is an active institution of the tenant (another tenant's is not). */
    public boolean isActive(UUID institution) {
        List<Institution> found = read(ListActiveInstitutionsRequest.newBuilder()
                .setFilter("id = \"" + institution + "\"")
                .setPageable(PageableRequest.newBuilder().setPage(1).setSize(1)).build()).getItemsList();
        return !found.isEmpty();
    }

    private ListActiveInstitutionsResponse read(ListActiveInstitutionsRequest request) {
        try {
            return org.listActiveInstitutions(request);
        } catch (IntegrationGrpcClientException e) {
            throw new UnavailableException(UNAVAILABLE);
        }
    }
}
