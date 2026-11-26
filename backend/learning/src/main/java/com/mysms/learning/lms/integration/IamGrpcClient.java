package com.mysms.learning.lms.integration;

import com.mysms.authclient.TenantCodeResolver;
import com.mysms.core.error.UnavailableException;
import com.mysms.core.grpc.IntegrationGrpcClientException;
import com.mysms.iam.v1.GetMeRequest;
import com.mysms.iam.v1.GetMeResponse;
import com.mysms.iam.v1.ListUserAccountsRequest;
import com.mysms.iam.v1.ListUserAccountsResponse;
import com.mysms.iam.v1.ResolveTenantCodeRequest;
import com.mysms.iam.v1.UserAccount;
import com.mysms.iam.v1.UserAccountServiceGrpc;
import java.util.Optional;
import java.util.UUID;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;

/**
 * lms's client of the iam service, and the module's {@link TenantCodeResolver} for guests' school codes. Every call
 * carries the caller's own identity; a failed call is an {@link IntegrationGrpcClientException} the member directory
 * maps onward.
 */
@Service
public class IamGrpcClient implements TenantCodeResolver {

    /** The starter's client of the iam service. */
    public static final String IAM_SERVICE_KEY = "iam-service";
    private static final String SERVICE = "iam";
    private static final int NOT_FOUND = 404;

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

    /** iam.ResolveTenantCode: empty for a school iam does not know as active; 503 when iam cannot answer. */
    @Override
    public Optional<UUID> activeTenant(String code) {
        try {
            String tenant = IntegrationGrpcClientException.call(SERVICE, () -> userAccounts.resolveTenantCode(
                    ResolveTenantCodeRequest.newBuilder().setTenantCode(code).build())).getResult().getTenantId();
            return Optional.of(UUID.fromString(tenant));
        } catch (IntegrationGrpcClientException e) {
            if (e.httpStatus() == NOT_FOUND) {
                return Optional.empty();
            }
            throw new UnavailableException("The identity service could not check the school code.");
        }
    }

    /**
     * iam.GetMe with the caller's own token: the caller's e-mail address, for a reply to their message; empty when
     * they have none. 503 when iam cannot answer.
     */
    public Optional<String> callerEmail() {
        try {
            UserAccount me = getMe(GetMeRequest.getDefaultInstance()).getResult().getUser();
            return me.hasEmail() && !me.getEmail().isBlank() ? Optional.of(me.getEmail()) : Optional.empty();
        } catch (IntegrationGrpcClientException e) {
            throw new UnavailableException("The identity service could not answer.");
        }
    }
}
