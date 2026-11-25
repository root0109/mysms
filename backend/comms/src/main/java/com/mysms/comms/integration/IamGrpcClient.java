package com.mysms.comms.integration;

import com.mysms.core.grpc.IntegrationGrpcClientException;
import com.mysms.iam.v1.GetMeRequest;
import com.mysms.iam.v1.GetMeResponse;
import com.mysms.iam.v1.ListUserAccountsRequest;
import com.mysms.iam.v1.ListUserAccountsResponse;
import com.mysms.iam.v1.UserAccountServiceGrpc;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;

/**
 * comms's client of the iam service: the accounts whose addresses messages go to. Every call carries the caller's
 * own identity; a failed call is an {@link IntegrationGrpcClientException}.
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

    public ListUserAccountsResponse listUserAccounts(ListUserAccountsRequest request) {
        return IntegrationGrpcClientException.call(SERVICE, () -> userAccounts.listUserAccounts(request));
    }
}
