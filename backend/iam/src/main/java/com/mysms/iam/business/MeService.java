package com.mysms.iam.business;

import com.mysms.authclient.AuthContext;
import com.mysms.contracts.iam.model.Me;
import com.mysms.contracts.iam.model.Person;
import com.mysms.contracts.iam.model.RoleGrant;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.error.ResourceNotFoundException;
import com.mysms.iam.config.IamDatabaseConfig;
import com.mysms.iam.config.IamSecurityConfig;
import com.mysms.iam.domain.MeAccountEntity;
import com.mysms.iam.mapper.PersonMapper;
import com.mysms.iam.mapper.MeAccountMapper;
import com.mysms.iam.repository.GrantRepository;
import com.mysms.iam.repository.AccountRepository;
import com.mysms.iam.repository.RoleGrantRow;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;

/**
 * iam.GetMe: the caller's account, role grants (with scope) and effective permissions, all as of now, and — when
 * the people service is configured — the linked person record, read with the caller's own token through
 * people.GetMyPerson. The person is optional in the contract: if people cannot answer, Me is returned without it
 * rather than failing the whole call.
 */
@Service
public class MeService {

    private static final Logger LOG = LoggerFactory.getLogger(MeService.class);

    private final ModuleDatabase database;
    private final AccountRepository users;
    private final GrantRepository grants;
    private final MeAccountMapper userMapper;
    private final PersonMapper personMapper;
    private final IamSecurityConfig.PeopleClient people;
    private final Clock clock;

    public MeService(@Qualifier(IamDatabaseConfig.DATABASE) ModuleDatabase database, AccountRepository users,
                     GrantRepository grants, MeAccountMapper userMapper, PersonMapper personMapper,
                     IamSecurityConfig.PeopleClient people, Clock clock) {
        this.database = database;
        this.users = users;
        this.grants = grants;
        this.userMapper = userMapper;
        this.personMapper = personMapper;
        this.people = people;
        this.clock = clock;
    }

    public Me getMe() {
        AuthContext caller = AuthContext.require();
        LocalDate today = LocalDate.ofInstant(clock.instant(), ZoneOffset.UTC);
        Me me = database.tx().read(() -> {
            MeAccountEntity account = users.findById(caller.userId())
                    .orElseThrow(() -> new ResourceNotFoundException(MeAccountEntity.TABLE, caller.userId()));
            List<RoleGrantRow> rows = grants.activeGrants(caller.userId(), today);
            Me result = new Me(userMapper.toDto(account));
            result.setRoles(rows.stream().map(MeService::grant).toList());
            result.setPermissions(grants.effectivePermissions(caller.userId(), today));
            return result;
        });
        if (me.getUser().getPersonId() != null) {
            person().ifPresent(me::setPerson);
        }
        return me;
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
