package com.mysms.attendance.integration;

import com.mysms.core.grpc.IntegrationGrpcClientException;
import com.mysms.org.v1.GetInstitutionRequest;
import com.mysms.org.v1.GetInstitutionResponse;
import com.mysms.org.v1.InstitutionServiceGrpc;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;

/** Calls the org service as the caller; a failed call throws {@link IntegrationGrpcClientException}. */
@Service
public class OrgGrpcClient {

    /** The starter's client of the org service. */
    public static final String ORG_SERVICE_KEY = "org-service";
    private static final String SERVICE = "org";

    private InstitutionServiceGrpc.InstitutionServiceBlockingStub institutions;

    @GrpcClient(ORG_SERVICE_KEY)
    public void initInstitutionStub(InstitutionServiceGrpc.InstitutionServiceBlockingStub stub) {
        this.institutions = stub;
    }

    public GetInstitutionResponse getInstitution(GetInstitutionRequest request) {
        return IntegrationGrpcClientException.call(SERVICE, () -> institutions.getInstitution(request));
    }
}
