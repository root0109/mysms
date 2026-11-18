package com.mysms.sis.integration;

import com.mysms.core.grpc.IntegrationGrpcClientException;
import com.mysms.iam.v1.CreateUserAccountRequest;
import com.mysms.iam.v1.CreateUserAccountResponse;
import com.mysms.iam.v1.CreateUserRoleRequest;
import com.mysms.iam.v1.CreateUserRoleResponse;
import com.mysms.iam.v1.ListRolesRequest;
import com.mysms.iam.v1.ListRolesResponse;
import com.mysms.iam.v1.ListUserAccountsRequest;
import com.mysms.iam.v1.ListUserAccountsResponse;
import com.mysms.iam.v1.RoleServiceGrpc;
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
    private RoleServiceGrpc.RoleServiceBlockingStub roles;

    @GrpcClient(IAM_SERVICE_KEY)
    public void initUserAccountStub(UserAccountServiceGrpc.UserAccountServiceBlockingStub stub) {
        this.userAccounts = stub;
    }

    @GrpcClient(IAM_SERVICE_KEY)
    public void initRoleStub(RoleServiceGrpc.RoleServiceBlockingStub stub) {
        this.roles = stub;
    }

    public ListUserAccountsResponse listUserAccounts(ListUserAccountsRequest request) {
        return IntegrationGrpcClientException.call(SERVICE, () -> userAccounts.listUserAccounts(request));
    }

    public CreateUserAccountResponse createUserAccount(CreateUserAccountRequest request) {
        return IntegrationGrpcClientException.call(SERVICE, () -> userAccounts.createUserAccount(request));
    }

    public CreateUserRoleResponse createUserRole(CreateUserRoleRequest request) {
        return IntegrationGrpcClientException.call(SERVICE, () -> userAccounts.createUserRole(request));
    }

    public ListRolesResponse listRoles(ListRolesRequest request) {
        return IntegrationGrpcClientException.call(SERVICE, () -> roles.listRoles(request));
    }
}
