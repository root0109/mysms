package com.mysms.careers.integration;

import com.mysms.authclient.TenantCodeResolver;
import com.mysms.core.error.PermissionDeniedException;
import com.mysms.core.error.UnavailableException;
import com.mysms.core.grpc.IntegrationGrpcClientException;
import com.mysms.iam.v1.GetUserAccountRequest;
import com.mysms.iam.v1.ResolveTenantCodeRequest;
import com.mysms.iam.v1.UserAccount;
import com.mysms.iam.v1.UserAccountServiceGrpc;
import java.util.Optional;
import java.util.UUID;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;

/**
 * careers's gRPC client of iam ({@code grpc.client.iam-service.*}), and the module's {@link TenantCodeResolver} for
 * guests' school codes.
 */
@Service
public class IamGrpcClient implements TenantCodeResolver {

    /** The starter's client of the iam service. */
    public static final String IAM_SERVICE_KEY = "iam-service";
    private static final String SERVICE = "iam";
    private static final int NOT_FOUND = 404;
    private static final int FORBIDDEN = 403;
    private static final String READ_ACCOUNTS = "iam.user_account.read";

    private UserAccountServiceGrpc.UserAccountServiceBlockingStub userAccounts;

    @GrpcClient(IAM_SERVICE_KEY)
    public void initUserAccountStub(UserAccountServiceGrpc.UserAccountServiceBlockingStub stub) {
        this.userAccounts = stub;
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
     * iam.GetUserAccount with the caller's own token: the account's e-mail address; empty when the account is gone or
     * has none. The caller needs iam's read permission on accounts (403 otherwise); 503 when iam cannot answer.
     */
    public Optional<String> emailOf(UUID userAccount) {
        try {
            UserAccount account = IntegrationGrpcClientException.call(SERVICE, () -> userAccounts.getUserAccount(
                    GetUserAccountRequest.newBuilder().setId(userAccount.toString()).build())).getUserAccount();
            return account.hasEmail() && !account.getEmail().isBlank() ? Optional.of(account.getEmail())
                    : Optional.empty();
        } catch (IntegrationGrpcClientException e) {
            if (e.httpStatus() == NOT_FOUND) {
                return Optional.empty();
            }
            if (e.httpStatus() == FORBIDDEN) {
                throw new PermissionDeniedException("Writing to an applicant also needs " + READ_ACCOUNTS + ".");
            }
            throw new UnavailableException("The identity service could not answer.");
        }
    }
}
