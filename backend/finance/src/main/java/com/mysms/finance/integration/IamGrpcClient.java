package com.mysms.finance.integration;

import com.mysms.core.grpc.IntegrationGrpcClientException;
import com.mysms.iam.v1.GetMeRequest;
import com.mysms.iam.v1.GetMeResponse;
import com.mysms.iam.v1.UserAccountServiceGrpc;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;

/**
 * Calls the iam service with the caller's own identity; a failed call throws {@link IntegrationGrpcClientException}.
 */
@Service
public class IamGrpcClient {

    /** The starter's client of the iam service. */
    public static final String IAM_SERVICE_KEY = "iam-service";
    private static final String SERVICE = "iam";

    private UserAccountServiceGrpc.UserAccountServiceBlockingStub userAccounts;

    @GrpcClient(IAM_SERVICE_KEY)
    public void initUserAccountStub(UserAccountServiceGrpc.UserAccountServiceBlockingStub stub) {
        this.userAccounts = stub;
    }

    public GetMeResponse getMe(GetMeRequest request) {
        return IntegrationGrpcClientException.call(SERVICE, () -> userAccounts.getMe(request));
    }
}
