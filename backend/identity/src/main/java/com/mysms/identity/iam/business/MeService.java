package com.mysms.identity.iam.business;

import com.mysms.authclient.AuthContext;
import com.mysms.authclient.SiblingAccess;
import com.mysms.core.error.ApiException;
import com.mysms.identity.iam.api.model.Me;
import com.mysms.identity.iam.api.model.Person;
import com.mysms.identity.iam.api.model.RoleGrant;
import com.mysms.core.db.AppTransactionManager;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.error.ResourceNotFoundException;
import com.mysms.identity.iam.config.IamDatabaseConfig;
import com.mysms.identity.iam.domain.MeAccountEntity;
import com.mysms.identity.iam.mapper.MePersonMapper;
import com.mysms.identity.people.business.MyPersonService;
import com.mysms.identity.iam.mapper.MeAccountMapper;
import com.mysms.identity.iam.repository.AccountRepository;
import com.mysms.identity.iam.repository.ActiveRoleRepository;
import com.mysms.identity.iam.repository.GrantRepository;
import com.mysms.identity.iam.repository.MemberRecordRepository;
import com.mysms.identity.iam.repository.MemberRecordRepository.Kind;
import com.mysms.identity.iam.repository.SessionStateRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * iam.GetMe: the caller's account, role grants (with scope), effective permissions and own staff and student records
 * (kept from hr's and sis's events, {@code MemberRecordSync}), all as of now, and the linked person record, read
 * as the caller from the sibling people area (people.GetMyPerson in process: {@link MyPersonService}). The person is
 * optional in the contract: if people cannot answer, Me is returned without it rather than failing the whole call.
 */
@Service
public class MeService extends AppTransactionManager {

    /** people.GetMyPerson's permission. */
    static final String PERMISSION_READ_MY_PERSON = "people.person.read_mine";
    private static final Logger LOG = LoggerFactory.getLogger(MeService.class);
    private final AccountRepository users;
    private final GrantRepository grants;
    private final ActiveRoleRepository roles;
    private final SessionStateRepository sessions;
    private final MemberRecordRepository members;
    private final MeAccountMapper userMapper;
    private final MyPersonService people;
    private final SiblingAccess sibling;
    private final MePersonMapper personMapper;
    private final Clock clock;

    public MeService(@Qualifier(IamDatabaseConfig.DATABASE) ModuleDatabase database,
            @Qualifier(IamDatabaseConfig.TRANSACTIONS) PlatformTransactionManager transactions, AccountRepository users,
                     GrantRepository grants, ActiveRoleRepository roles,
                     SessionStateRepository sessions, MemberRecordRepository members, MeAccountMapper userMapper,
                     MyPersonService people, SiblingAccess sibling, MePersonMapper personMapper, Clock clock) {

        super(database, transactions);
        this.users = users;
        this.grants = grants;
        this.roles = roles;
        this.sessions = sessions;
        this.members = members;
        this.userMapper = userMapper;
        this.people = people;
        this.sibling = sibling;
        this.personMapper = personMapper;
        this.clock = clock;
    }

    public Me getMe() {
        AuthContext caller = AuthContext.require();
        LocalDate today = LocalDate.ofInstant(clock.instant(), ZoneOffset.UTC);
        Me me = account(caller.userId(), caller.sessionId(), today);
        if (me.getUser().getPersonId() != null) {
            person().ifPresent(me::setPerson);
        }
        return me;
    }

    /**
     * The caller's account, every active role grant (with its role id, for the persona switcher), the role the
     * session works in after iam.SwitchRole and the permissions in force — that role's alone once switched — and own
     * staff and student records (of the account or of its person, iam.member_record), read in one transaction.
     */
    private Me account(UUID userId, Optional<UUID> session, LocalDate today) {
        return doInReadTransaction(() -> {
            MeAccountEntity account = users.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException(MeAccountEntity.TABLE, userId));
            List<ActiveRoleRepository.Grant> rows = roles.grants(userId, today);
            Me result = new Me(userMapper.toDto(account));
            result.setRoles(rows.stream().map(MeService::grant).toList());
            Optional<SessionStateRepository.State> state = session.flatMap(sessions::find);
            Optional<UUID> activeRole = state.flatMap(SessionStateRepository.State::activeRoleId);
            if (activeRole.isPresent()) {
                Optional<UUID> scope = state.get().activeScopeId();
                List<ActiveRoleRepository.Grant> selected = rows.stream()
                        .filter(g -> g.roleId().equals(activeRole.get()) && (scope.isEmpty() || scope.equals(g.scopeId())))
                        .toList();
                selected.stream().findFirst().map(MeService::grant).ifPresent(first -> {
                    first.setScopeId(scope.orElse(selected.size() == 1 ? first.getScopeId() : null));
                    result.setActiveRole(first);
                });
                result.setPermissions(roles.rolePermissions(userId, selected.isEmpty() ? null : activeRole.get()));
            } else {
                result.setPermissions(grants.effectivePermissions(userId, today));
            }
            result.setStaffIds(members.recordsOf(Kind.STAFF, userId, account.getPersonId()));
            result.setStudentIds(members.recordsOf(Kind.STUDENT, userId, account.getPersonId()));
            return result;
        });
    }

    /**
     * people.GetMyPerson as the caller: without its permission, or when people refuses or fails in any way, Me is
     * returned without the person, as when the call went over gRPC.
     */
    private Optional<Person> person() {
        if (!sibling.permits(PERMISSION_READ_MY_PERSON)) {
            LOG.debug("the caller may not read their person; Me is returned without it");
            return Optional.empty();
        }
        try {
            return Optional.of(personMapper.toMe(people.get()));
        } catch (ApiException e) {
            LOG.warn("people.GetMyPerson failed ({}); Me is returned without the person", e.code());
            return Optional.empty();
        } catch (RuntimeException e) {
            LOG.warn("people.GetMyPerson failed ({}); Me is returned without the person", e.getClass().getSimpleName());
            return Optional.empty();
        }
    }

    private static RoleGrant grant(ActiveRoleRepository.Grant row) {
        RoleGrant grant = new RoleGrant(row.roleCode(), RoleGrant.ScopeTypeEnum.fromValue(row.scopeType()));
        grant.setRoleId(row.roleId());
        grant.setScopeId(row.scopeId().orElse(null));
        return grant;
    }
}
