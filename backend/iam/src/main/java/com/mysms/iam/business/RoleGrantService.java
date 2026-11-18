package com.mysms.iam.business;

import com.mysms.authclient.DataScope;
import com.mysms.core.context.Actor;
import com.mysms.core.context.CallContextHolder;
import com.mysms.core.db.AppTransactionManager;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.error.FieldError;
import com.mysms.core.error.PreconditionFailedException;
import com.mysms.core.error.ResourceNotFoundException;
import com.mysms.core.error.ValidationFailedException;
import com.mysms.core.outbox.AuditWriter;
import com.mysms.core.outbox.OutboxWriter;
import com.mysms.core.validation.DtoValidator;
import com.mysms.iam.api.model.RoleGrant;
import com.mysms.iam.api.model.SetUserRolesRequest;
import com.mysms.iam.api.model.UserRoleList;
import com.mysms.iam.config.IamDatabaseConfig;
import com.mysms.iam.config.IamEventsConfig;
import com.mysms.iam.domain.UserRoleEntity;
import com.mysms.iam.events.IamEvents;
import com.mysms.iam.mapper.UserRoleMapper;
import com.mysms.iam.repository.AccountContactRepository;
import com.mysms.iam.repository.UserAccountRepository;
import com.mysms.iam.repository.UserRoleRepository;
import java.time.Clock;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * iam.SetUserRoles: the user's role grants become exactly the requested set, in one transaction. Grants already in
 * the set are kept as they are (dates, active flag, primary unit); the others are removed and the missing ones
 * created. Every grant removed or created must be one the caller could make (AccessRules#requireGrantable: the
 * caller holds each of the role's permissions at the grant's reach), never on the caller's own account and never
 * while an administrator acts as someone; the user's tokens change at their next refresh.
 */
@Service
public class RoleGrantService extends AppTransactionManager {

    private static final String PERMISSION_SET_ROLES = "iam.user_account.set_user_roles";
    private static final String TENANT = "tenant";
    private static final int MAX_GRANTS = 100;

    private final UserRoleRepository userRoles;
    private final UserRoleMapper mapper;
    private final UserAccountRepository accounts;
    private final AccountContactRepository roles;
    private final AccessRules rules;
    private final OutboxWriter outbox;
    private final AuditWriter audit;
    private final DtoValidator validator;
    private final Clock clock;

    public RoleGrantService(@Qualifier(IamDatabaseConfig.DATABASE) ModuleDatabase database,
            @Qualifier(IamDatabaseConfig.TRANSACTIONS) PlatformTransactionManager transactions,
            UserRoleRepository userRoles, UserRoleMapper mapper, UserAccountRepository accounts,
            AccountContactRepository roles, AccessRules rules, @Qualifier(IamEventsConfig.OUTBOX) OutboxWriter outbox,
            @Qualifier(IamEventsConfig.AUDIT) AuditWriter audit, DtoValidator validator, Clock clock) {
        super(database, transactions);
        this.userRoles = userRoles;
        this.mapper = mapper;
        this.accounts = accounts;
        this.roles = roles;
        this.rules = rules;
        this.outbox = outbox;
        this.audit = audit;
        this.validator = validator;
        this.clock = clock;
    }

    public UserRoleList setUserRoles(UUID userId, SetUserRolesRequest request) {
        validator.validate(request);
        AccessRules.requireOwnIdentity();
        rules.requireNotSelf(userId);
        DataScope scope = DataScope.current(PERMISSION_SET_ROLES);
        List<RoleGrant> wanted = request.getGrants();
        if (wanted.size() > MAX_GRANTS) {
            throw new ValidationFailedException(List.of(new FieldError("grants", "at most " + MAX_GRANTS + " grants",
                    "Size")));
        }
        return doInWriteTransaction(() -> {
            accounts.findById(userId).orElseThrow(() -> new ResourceNotFoundException(IamEvents.USER_ACCOUNT, userId));
            // role grants are tenant-level rows, as for a single grant (iam.CreateUserRole)
            scope.requireWritable(Optional.empty());
            Set<Key> desired = resolve(wanted);
            List<UserRoleEntity> current = userRoles.findByUser(userId);
            List<UserRoleEntity> result = new ArrayList<>();
            for (UserRoleEntity grant : current) {
                Key key = new Key(grant.getRoleId(), grant.getScopeType(), grant.getScopeId());
                if (desired.remove(key)) {
                    result.add(grant);
                    continue;
                }
                rules.requireGrantable(grant.getRoleId(), grant.getScopeType(), grant.getScopeId());
                int expected = grant.getRowVersion();
                grant.onDeletingData(Actor.current(), clock);
                if (userRoles.softDelete(grant, expected) == 0) {
                    throw new PreconditionFailedException("A role grant changed meanwhile; read the grants again.");
                }
                audit.recordDelete(UserAccountService.USER_ROLE_AGGREGATE, grant.getId());
                outbox.publish(UserAccountService.USER_ROLE_AGGREGATE + IamEvents.DELETED,
                        UserAccountService.USER_ROLE_AGGREGATE, grant.getId(), IamEvents.payload(grant.getId()));
            }
            for (Key key : desired) {
                rules.requireGrantable(key.roleId(), key.scopeType(), key.scopeId());
                UserRoleEntity grant = UserRoleEntity.builder().userId(userId).roleId(key.roleId())
                        .scopeType(key.scopeType()).scopeId(key.scopeId()).isActive(true).build();
                grant.onCreatingData(Actor.current(), clock);
                grant.setTenantId(CallContextHolder.require().tenantId().orElseThrow());
                UserRoleEntity stored = userRoles.insert(grant);
                audit.recordCreate(UserAccountService.USER_ROLE_AGGREGATE, stored.getId());
                outbox.publish(UserAccountService.USER_ROLE_AGGREGATE + IamEvents.CREATED,
                        UserAccountService.USER_ROLE_AGGREGATE, stored.getId(), IamEvents.payload(stored.getId()));
                result.add(stored);
            }
            outbox.publish(IamEvents.ROLES_SET, IamEvents.USER_ACCOUNT, userId, IamEvents.payload(userId));
            return new UserRoleList(result.stream().map(mapper::toDto).toList());
        });
    }

    /** The requested grants with their roles resolved; every problem at once (422). */
    private Set<Key> resolve(List<RoleGrant> wanted) {
        List<FieldError> errors = new ArrayList<>();
        Set<Key> keys = new LinkedHashSet<>();
        for (int i = 0; i < wanted.size(); i++) {
            RoleGrant grant = wanted.get(i);
            String at = "grants[" + i + "].";
            String scopeType = grant.getScopeType().getValue();
            if (TENANT.equals(scopeType) != (grant.getScopeId() == null)) {
                errors.add(new FieldError(at + "scopeId", "is required for every scope but tenant, and only then",
                        "INVALID_SCOPE"));
                continue;
            }
            Optional<AccountContactRepository.RoleRef> role = roles.roleByCode(grant.getRoleCode().trim());
            if (role.isEmpty()) {
                errors.add(new FieldError(at + "roleCode", "no active role has this code", "NOT_FOUND"));
                continue;
            }
            Key key = new Key(role.get().id(), scopeType, grant.getScopeId());
            if (!keys.add(key)) {
                errors.add(new FieldError(at + "roleCode", "the same grant is listed twice", "DUPLICATE"));
            }
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
        return keys;
    }

    /** What makes two grants the same (uq_user_role_assignment). */
    private record Key(UUID roleId, String scopeType, UUID scopeId) {
    }
}
