package com.mysms.learning.lms.integration;

import com.mysms.assessment.v1.GradingBandServiceGrpc;
import com.mysms.assessment.v1.ListGradingBandsRequest;
import com.mysms.assessment.v1.ListGradingBandsResponse;
import com.mysms.core.grpc.IntegrationGrpcClientException;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;

/**
 * lms's client of the assessment service: a cohort grades on one of
 * assessment's grading schemes, whose bands turn a percentage into a grade. The starter injects the blocking stub of
 * the client {@value #ASSESSMENT_SERVICE_KEY}; every call carries the caller's own identity.
 */
@Service
public class AssessmentGrpcClient {

    /** The starter's client of the assessment service. */
    public static final String ASSESSMENT_SERVICE_KEY = "assessment-service";
    private static final String SERVICE = "assessment";

    private GradingBandServiceGrpc.GradingBandServiceBlockingStub bands;

    @GrpcClient(ASSESSMENT_SERVICE_KEY)
    public void initGradingBandStub(GradingBandServiceGrpc.GradingBandServiceBlockingStub stub) {
        this.bands = stub;
    }

    public ListGradingBandsResponse listGradingBands(ListGradingBandsRequest request) {
        return IntegrationGrpcClientException.call(SERVICE, () -> bands.listGradingBands(request));
    }
}
