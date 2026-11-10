package com.mysms.iam.business;

import com.mysms.authclient.AuthContext;
import com.mysms.iam.api.model.Me;
import com.mysms.iam.api.model.Person;
import com.mysms.iam.api.model.RoleGrant;
import com.mysms.core.db.AppTransactionManager;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.error.ResourceNotFoundException;
import com.mysms.iam.config.IamDatabaseConfig;
import com.mysms.iam.config.IamSecurityConfig;
import com.mysms.iam.domain.MeAccountEntity;
import com.mysms.iam.mapper.MeAccountMapper;
import com.mysms.iam.mapper.PersonMapper;
import com.mysms.iam.repository.AccountRepository;
import com.mysms.iam.repository.GrantRepository;
import com.mysms.iam.repository.MemberRecordRepository;
import com.mysms.iam.repository.MemberRecordRepository.Kind;
import com.mysms.iam.repository.RoleGrantRow;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.web.client.RestClientException;

/**
 * iam.GetMe: the caller's account, role grants (with scope), effective permissions and own staff and student records
 * (kept from hr's and sis's events, {@code MemberRecordSync}), all as of now, and — when
 * the people service is configured — the linked person record, read with the caller's own token through
 * people.GetMyPerson. The person is optional in the contract: if people cannot answer, Me is returned without it
 * rather than failing the whole call.
 */
@Service
public class MeService extends AppTransactionManager {

    private static final Logger LOG = LoggerFactory.getLogger(MeService.class);
    private final AccountRepository users;
    private final GrantRepository grants;
    private final MemberRecordRepository members;
    private final MeAccountMapper userMapper;
    private final PersonMapper personMapper;
    private final IamSecurityConfig.PeopleClient people;
    private final Clock clock;

    public MeService(@Qualifier(IamDatabaseConfig.DATABASE) ModuleDatabase database,
            @Qualifier(IamDatabaseConfig.TRANSACTIONS) PlatformTransactionManager transactions, AccountRepository users,
                     GrantRepository grants, MemberRecordRepository members, MeAccountMapper userMapper,
                     PersonMapper personMapper, IamSecurityConfig.PeopleClient people, Clock clock) {

        super(database, transactions);
        this.users = users;
        this.grants = grants;
        this.members = members;
        this.userMapper = userMapper;
        this.personMapper = personMapper;
        this.people = people;
        this.clock = clock;
    }

    public Me getMe() {
        AuthContext caller = AuthContext.require();
        LocalDate today = LocalDate.ofInstant(clock.instant(), ZoneOffset.UTC);
        Me me = account(caller.userId(), today);
        if (me.getUser().getPersonId() != null) {
            person().ifPresent(me::setPerson);
        }
        return me;
    }

    /**
     * The caller's account, active role grants, effective permissions and own staff and student records (of the
     * account or of its person, iam.member_record), read in one transaction.
     */
    private Me account(UUID userId, LocalDate today) {
        return doInReadTransaction(() -> {
            MeAccountEntity account = users.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException(MeAccountEntity.TABLE, userId));
            List<RoleGrantRow> rows = grants.activeGrants(userId, today);
            Me result = new Me(userMapper.toDto(account));
            result.setRoles(rows.stream().map(MeService::grant).toList());
            result.setPermissions(grants.effectivePermissions(userId, today));
            result.setStaffIds(members.recordsOf(Kind.STAFF, userId, account.getPersonId()));
            result.setStudentIds(members.recordsOf(Kind.STUDENT, userId, account.getPersonId()));
            return result;
        });
    }

    private Optional<Person> person() {
        if (people.api().isEmpty()) {
            return Optional.empty();
        }
        try {
            ResponseEntity<com.mysms.sdk.people.model.Person> response = people.api().get().getMyPerson(null);
            return Optional.ofNullable(response.getBody()).map(personMapper::toIam);
        } catch (RestClientException e) {
            LOG.warn("people.GetMyPerson failed ({}); Me is returned without the person", e.getClass().getSimpleName());
            return Optional.empty();
        }
    }

    private static RoleGrant grant(RoleGrantRow row) {
        RoleGrant grant = new RoleGrant(row.roleCode(), RoleGrant.ScopeTypeEnum.fromValue(row.scopeType()));
        grant.setScopeId(row.scopeId().orElse(null));
        return grant;
    }
}
