package com.mysms.finance.integration;

import com.mysms.core.grpc.IntegrationGrpcClientException;
import com.mysms.org.v1.AcademicYearServiceGrpc;
import com.mysms.org.v1.GetAcademicYearRequest;
import com.mysms.org.v1.GetAcademicYearResponse;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;

/**
 * Calls the org service with the caller's own identity; a failed call throws {@link IntegrationGrpcClientException}.
 */
@Service
public class OrgGrpcClient {

    /** The starter's client of the org service. */
    public static final String ORG_SERVICE_KEY = "org-service";
    private static final String SERVICE = "org";

    private AcademicYearServiceGrpc.AcademicYearServiceBlockingStub academicYears;

    @GrpcClient(ORG_SERVICE_KEY)
    public void initAcademicYearStub(AcademicYearServiceGrpc.AcademicYearServiceBlockingStub stub) {
        this.academicYears = stub;
    }

    public GetAcademicYearResponse getAcademicYear(GetAcademicYearRequest request) {
        return IntegrationGrpcClientException.call(SERVICE, () -> academicYears.getAcademicYear(request));
    }
}
