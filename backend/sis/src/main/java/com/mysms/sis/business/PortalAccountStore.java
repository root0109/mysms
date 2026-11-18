package com.mysms.sis.business;

import com.mysms.common.v1.PageableRequest;
import com.mysms.core.error.DuplicateResourceException;
import com.mysms.core.error.FieldError;
import com.mysms.core.error.InvalidStateException;
import com.mysms.core.error.PermissionDeniedException;
import com.mysms.core.error.UnavailableException;
import com.mysms.core.error.ValidationFailedException;
import com.mysms.core.grpc.IntegrationGrpcClientException;
import com.mysms.iam.v1.CreateUserAccountRequest;
import com.mysms.iam.v1.CreateUserRoleRequest;
import com.mysms.iam.v1.ListRolesRequest;
import com.mysms.iam.v1.ListUserAccountsRequest;
import com.mysms.iam.v1.Role;
import com.mysms.iam.v1.UserAccount;
import com.mysms.iam.v1.UserRole;
import com.mysms.sis.integration.IamGrpcClient;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/**
 * Gateway to iam for the login accounts of students and guardians (an Adapter over iam's gRPC client, so the sis
 * services never see the transport). Every call carries the caller's own identity: the caller creates the account and
 * grants the role with their own permissions in iam (iam.user_account.read / create, iam.role.read,
 * iam.user_role.create), and iam's own rules apply — nobody hands out a role they could not hold themselves.
 *
 * <p>Opening an account is safe to repeat: a person has one account, so the account iam already has for the person
 * is used instead of a second one, and a role grant that already exists is left as it is. That is what lets sis
 * retry after its own write failed, without leaving an account nobody is linked to.
 */
@Component
public class PortalAccountStore {

    /** iam's user types of the two personas ({@code UserAccount.user_type}). */
    public static final String STUDENT = "student";
    public static final String GUARDIAN = "guardian";
    /** iam's role codes of the two personas; the tenant creates the roles in access management. */
    public static final String STUDENT_ROLE = "student";
    public static final String GUARDIAN_ROLE = "guardian";
    private static final String PURPOSE = "Creating a portal account";
    private static final Logger LOG = LoggerFactory.getLogger(PortalAccountStore.class);

    /**
     * What sis asks iam for.
     *
     * @param personId     the person the account belongs to (people.person)
     * @param userType     {@link #STUDENT} or {@link #GUARDIAN}
     * @param username     the sign-in name of a new account
     * @param email        the account's e-mail address, or null
     * @param mobile       the account's mobile number, or null
     * @param roleCode     the role to grant ({@link #STUDENT_ROLE} or {@link #GUARDIAN_ROLE})
     * @param institutions the institutions the role is granted in (the student's; a guardian's children's)
     */
    public record Request(UUID personId, String userType, String username, String email, String mobile,
                          String roleCode, Collection<UUID> institutions) {

        public Request {
            institutions = List.copyOf(institutions);
        }
    }

    private final IamGrpcClient iam;

    public PortalAccountStore(IamGrpcClient iam) {
        this.iam = iam;
    }

    /**
     * The id of the person's login account: the one iam already has, or a new one. The role is granted in every
     * institution of the request. A tenant without an active role of the code is 409 before anything is created; a
     * sign-in name, e-mail or mobile number another account uses is 409; details iam does not accept are 422; a
     * caller without the iam permissions is refused (403); an unreachable iam is 503.
     */
    public UUID open(Request request) {
        UUID role = role(request.roleCode()).orElseThrow(() -> new InvalidStateException("The tenant has no active "
                + "role with the code " + request.roleCode() + "; create it in access management first."));
        UUID account = existing(request.personId()).orElseGet(() -> create(request));
        request.institutions().forEach(institution -> grant(account, role, institution));
        return account;
    }

    private Optional<UUID> existing(UUID personId) {
        List<UserAccount> found = Downstream.required("iam", "iam.user_account.read", PURPOSE,
                () -> iam.listUserAccounts(ListUserAccountsRequest.newBuilder()
                        .setFilter("personId = \"" + personId + "\"").setPageable(firstOne()).build()).getItemsList())
                .orElseThrow(() -> new UnavailableException("The iam service did not list the accounts."));
        return found.stream().findFirst().map(account -> UUID.fromString(account.getId()));
    }

    private UUID create(Request request) {
        UserAccount.Builder account = UserAccount.newBuilder().setUsername(request.username())
                .setPersonId(request.personId().toString()).setUserType(request.userType());
        if (request.email() != null) {
            account.setEmail(request.email());
        }
        if (request.mobile() != null) {
            account.setMobileNo(request.mobile());
        }
        UserAccount created;
        try {
            created = iam.createUserAccount(CreateUserAccountRequest.newBuilder().setUserAccount(account).build())
                    .getUserAccount();
        } catch (IntegrationGrpcClientException e) {
            throw refused(e, "iam.user_account.create");
        }
        if (created.getId().isEmpty()) {
            throw new UnavailableException("The iam service did not answer the new account.");
        }
        return UUID.fromString(created.getId());
    }

    /** The tenant's active role with the code, if it has one. */
    private Optional<UUID> role(String code) {
        List<Role> found = Downstream.required("iam", "iam.role.read", PURPOSE,
                () -> iam.listRoles(ListRolesRequest.newBuilder().setFilter("code = \"" + code + "\"")
                        .setPageable(firstOne()).build()).getItemsList())
                .orElseThrow(() -> new UnavailableException("The iam service did not list the roles."));
        return found.stream().filter(Role::getIsActive).findFirst().map(role -> UUID.fromString(role.getId()));
    }

    private void grant(UUID account, UUID role, UUID institution) {
        UserRole grant = UserRole.newBuilder().setRoleId(role.toString())
                .setScopeType(UserRole.ScopeType.SCOPE_TYPE_INSTITUTION).setScopeId(institution.toString())
                .setIsActive(true).build();
        try {
            iam.createUserRole(CreateUserRoleRequest.newBuilder().setUserAccountId(account.toString())
                    .setUserRole(grant).build());
        } catch (IntegrationGrpcClientException e) {
            if (e.httpStatus() != HttpStatus.CONFLICT.value()) { // 409: the account already holds the role there
                throw refused(e, "iam.user_role.create");
            }
        }
    }

    private static PageableRequest firstOne() {
        return PageableRequest.newBuilder().setPage(1).setSize(1).build();
    }

    /** iam's refusal of a write as this operation's error; iam's own sentence never reaches the client. */
    private static RuntimeException refused(IntegrationGrpcClientException e, String permission) {
        int status = e.httpStatus();
        if (status == HttpStatus.FORBIDDEN.value()) {
            return new PermissionDeniedException(PURPOSE + " also needs " + permission
                    + ", for a role and an institution the caller may grant.");
        }
        if (status == HttpStatus.CONFLICT.value()) {
            return new DuplicateResourceException(
                    "Another user account already signs in with this name, e-mail address or mobile number.");
        }
        if (status == HttpStatus.UNPROCESSABLE_CONTENT.value()) {
            return new ValidationFailedException(List.of(new FieldError("username",
                    "the sign-in name, e-mail address or mobile number is not accepted for an account", "INVALID")));
        }
        LOG.warn("the iam service answered {} ({})", status, e.statusCode());
        return new UnavailableException("The iam service could not answer.");
    }
}
