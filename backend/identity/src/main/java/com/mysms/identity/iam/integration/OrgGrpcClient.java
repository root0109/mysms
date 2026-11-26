package com.mysms.identity.iam.integration;

import com.mysms.authclient.ServiceTokenProvider;
import com.mysms.core.grpc.IntegrationGrpcClientException;
import com.mysms.org.v1.InstitutionServiceGrpc;
import com.mysms.org.v1.ListActiveInstitutionsRequest;
import com.mysms.org.v1.ListActiveInstitutionsResponse;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;

/**
 * Calls the org service as iam's service principal, in the tenant of the current call context: sign-up has no person
 * behind it. A failed call throws {@link IntegrationGrpcClientException}.
 */
@Service
public class OrgGrpcClient {

    /** The starter's client of the org service. */
    public static final String ORG_SERVICE_KEY = "org-service";
    private static final String SERVICE = "org";
    private static final String MODULE = "iam";

    private final ServiceTokenProvider serviceTokens;
    private InstitutionServiceGrpc.InstitutionServiceBlockingStub institutions;

    public OrgGrpcClient(ServiceTokenProvider serviceTokens) {
        this.serviceTokens = serviceTokens;
    }

    @GrpcClient(ORG_SERVICE_KEY)
    public void initInstitutionStub(InstitutionServiceGrpc.InstitutionServiceBlockingStub stub) {
        this.institutions = stub;
    }

    public ListActiveInstitutionsResponse listActiveInstitutions(ListActiveInstitutionsRequest request) {
        return IntegrationGrpcClientException.call(SERVICE, () -> institutions
                .withCallCredentials(serviceTokens.callCredentials(MODULE)).listActiveInstitutions(request));
    }
}
