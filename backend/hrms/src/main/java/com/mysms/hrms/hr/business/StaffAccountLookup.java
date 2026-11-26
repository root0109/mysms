package com.mysms.hrms.hr.business;

import com.mysms.common.v1.PageableRequest;
import com.mysms.core.error.DuplicateResourceException;
import com.mysms.core.error.FieldError;
import com.mysms.core.error.PermissionDeniedException;
import com.mysms.core.error.UnavailableException;
import com.mysms.core.error.ValidationFailedException;
import com.mysms.core.grpc.IntegrationGrpcClientException;
import com.mysms.hrms.hr.integration.IamGrpcClient;
import com.mysms.iam.v1.CreateUserAccountRequest;
import com.mysms.iam.v1.ListUserAccountsRequest;
import com.mysms.iam.v1.UserAccount;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/**
 * Adapter to the login accounts iam owns, called with the caller's own identity and iam permissions. iam keeps one
 * account per person, so a person who already has one (an earlier half-failed attempt, a guardian, another school) is
 * given that account, which makes the operation safe to repeat.
 */
@Component
public class StaffAccountLookup {

    static final String PERMISSION_READ = "iam.user_account.read";
    static final String PERMISSION_CREATE = "iam.user_account.create";
    private static final Logger LOG = LoggerFactory.getLogger(StaffAccountLookup.class);

    private final IamGrpcClient iam;

    public StaffAccountLookup(IamGrpcClient iam) {
        this.iam = iam;
    }

    /**
     * The login account of the person: the one iam already keeps for them, otherwise a new staff account that signs
     * in with the login name. A caller without the iam permissions is refused (403); a login name another account
     * already uses is a conflict (409); a name iam does not accept is the staff member's employee number at fault
     * (422); an unreachable or failing iam is 503.
     */
    public UUID accountFor(UUID personId, String loginName, LocalDate joinedOn) {
        Optional<UUID> existing = accountOf(personId);
        if (existing.isPresent()) {
            return existing.get();
        }
        UserAccount account = UserAccount.newBuilder().setUserType("staff").setPersonId(personId.toString())
                .setUsername(loginName).setJoinedOn(joinedOn.toString()).build();
        try {
            return idOf(iam.createUserAccount(CreateUserAccountRequest.newBuilder().setUserAccount(account).build())
                    .getUserAccount());
        } catch (IntegrationGrpcClientException e) {
            switch (e.httpStatus()) {
                case 403:
                    throw new PermissionDeniedException(
                            "Creating a staff login account also needs " + PERMISSION_CREATE + " for the whole tenant.");
                case 409:
                    // the person's account appeared meanwhile (a concurrent call), or the login name is taken
                    return accountOf(personId).orElseThrow(() -> new DuplicateResourceException(
                            "Another login account already signs in with this staff member's employee number."));
                case 422:
                    throw new ValidationFailedException(List.of(new FieldError("employeeCode",
                            "the employee number is not accepted as a login name", "INVALID")));
                default:
                    throw unavailable(e);
            }
        }
    }

    /** The account iam keeps for the person, if any. */
    private Optional<UUID> accountOf(UUID personId) {
        try {
            return iam.listUserAccounts(ListUserAccountsRequest.newBuilder()
                    .setFilter("personId = \"" + personId + "\"")
                    .setPageable(PageableRequest.newBuilder().setPage(1).setSize(1)).build())
                    .getItemsList().stream().findFirst().map(StaffAccountLookup::idOf);
        } catch (IntegrationGrpcClientException e) {
            if (e.httpStatus() == HttpStatus.FORBIDDEN.value()) {
                throw new PermissionDeniedException("Creating a staff login account also needs " + PERMISSION_READ
                        + " for the whole tenant.");
            }
            throw unavailable(e);
        }
    }

    private static UUID idOf(UserAccount account) {
        if (account.getId().isEmpty()) {
            LOG.warn("the iam service answered an account without its id");
            throw new UnavailableException("The iam service could not answer.");
        }
        return UUID.fromString(account.getId());
    }

    /** The downstream status and message never reach the client. */
    private static UnavailableException unavailable(IntegrationGrpcClientException e) {
        LOG.warn("the iam service answered {} ({}) to the staff account request", e.httpStatus(), e.statusCode());
        return new UnavailableException("The iam service could not answer.");
    }
}
