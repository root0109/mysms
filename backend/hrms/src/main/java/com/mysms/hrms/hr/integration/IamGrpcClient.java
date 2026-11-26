package com.mysms.hrms.hr.integration;

import com.mysms.core.grpc.IntegrationGrpcClientException;
import com.mysms.iam.v1.CreateUserAccountRequest;
import com.mysms.iam.v1.CreateUserAccountResponse;
import com.mysms.iam.v1.ListUserAccountsRequest;
import com.mysms.iam.v1.ListUserAccountsResponse;
import com.mysms.iam.v1.UserAccountServiceGrpc;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;

/** Calls the iam service as the caller; a failed call throws {@link IntegrationGrpcClientException}. */
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

    public ListUserAccountsResponse listUserAccounts(ListUserAccountsRequest request) {
        return IntegrationGrpcClientException.call(SERVICE, () -> userAccounts.listUserAccounts(request));
    }

    public CreateUserAccountResponse createUserAccount(CreateUserAccountRequest request) {
        return IntegrationGrpcClientException.call(SERVICE, () -> userAccounts.createUserAccount(request));
    }
}
