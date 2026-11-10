package com.mysms.iam.business;

import com.mysms.core.context.CallContext;
import com.mysms.core.context.CallContextHolder;
import com.mysms.core.db.AppTransactionManager;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.id.UuidV7;
import com.mysms.core.outbox.AuditWriter;
import com.mysms.iam.config.IamDatabaseConfig;
import com.mysms.iam.config.IamEventsConfig;
import com.mysms.iam.config.IamProperties;
import com.mysms.iam.domain.MeAccountEntity;
import com.mysms.iam.repository.AccountRepository;
import com.mysms.iam.repository.RoleSeedRepository;
import com.mysms.iam.repository.TenantDirectoryRepository;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * First start of a new installation: when mysms.iam.bootstrap.* are all set, makes sure the tenant code exists in the
 * tenant directory, the tenant has a system role "administrator" holding every permission, and an administrator
 * account with that role exists. Idempotent — nothing is changed when they already exist (an existing account's
 * password is never overwritten). The password comes from the environment and must be at least 12 characters.
 */
@Component
public class IamBootstrap extends AppTransactionManager implements ApplicationRunner {

    public static final String ADMIN_ROLE = "administrator";
    static final int MIN_PASSWORD = 12;
    private static final Pattern CODE = Pattern.compile("[a-z0-9][a-z0-9-]{1,59}");
    private static final Logger LOG = LoggerFactory.getLogger(IamBootstrap.class);

    private final IamProperties.Bootstrap settings;
    private final TenantDirectoryRepository tenants;
    private final RoleSeedRepository roles;
    private final AccountRepository users;
    private final PasswordHasher passwords;
    private final AuditWriter audit;
    private final Clock clock;

    public IamBootstrap(IamProperties properties, @Qualifier(IamDatabaseConfig.DATABASE) ModuleDatabase database,
            @Qualifier(IamDatabaseConfig.TRANSACTIONS) PlatformTransactionManager transactions,
                        TenantDirectoryRepository tenants, RoleSeedRepository roles, AccountRepository users,
                        PasswordHasher passwords, @Qualifier(IamEventsConfig.AUDIT) AuditWriter audit, Clock clock) {

        super(database, transactions);
        this.settings = properties.bootstrap();
        this.tenants = tenants;
        this.roles = roles;
        this.users = users;
        this.passwords = passwords;
        this.audit = audit;
        this.clock = clock;
    }

    @Override
    public void run(ApplicationArguments arguments) {
        if (settings.enabled()) {
            bootstrap();
        }
    }

    /** Visible for tests. */
    void bootstrap() {
        String code = settings.tenantCode().trim().toLowerCase(java.util.Locale.ROOT);
        if (!CODE.matcher(code).matches()) {
            throw new IllegalStateException("mysms.iam.bootstrap.tenant-code must be 2-60 of a-z, 0-9 and '-'");
        }
        if (settings.adminPassword().length() < MIN_PASSWORD) {
            throw new IllegalStateException("mysms.iam.bootstrap.admin-password must be at least " + MIN_PASSWORD
                    + " characters");
        }
        CallContext system = CallContext.anonymous(UUID.randomUUID().toString().replace("-", ""));
        UUID tenant = CallContextHolder.callAs(system, () -> existingTenant(code)).orElseGet(() -> UuidV7.next(clock));
        String username = settings.adminUsername().trim();
        String hash = passwords.hash(settings.adminPassword());
        CallContextHolder.callAs(system.withIdentity(tenant, null), () -> {
            provision(tenant, code, username, hash);
            return null;
        });
    }

    /** The tenant's id in the global directory, read before any tenant is bound. */
    private Optional<UUID> existingTenant(String code) {
        return doInGlobalPrimaryReadTransaction(() -> {
            return tenants.findTenant(code);
        });
    }

    /** The tenant, its administrator role with every permission and the administrator, in one tenant transaction. */
    private void provision(UUID tenant, String code, String username, String hash) {
        runInWriteTransaction(() -> {
            OffsetDateTime now = OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC)
                    .truncatedTo(ChronoUnit.MICROS);
            if (tenants.findTenant(code).isEmpty()) {
                tenants.insert(tenant, code);
            }
            Optional<UUID> existing = roles.findByCode(ADMIN_ROLE);
            UUID role = existing.orElseGet(() -> UuidV7.next(clock));
            if (existing.isEmpty()) {
                roles.insertSystemRole(role, tenant, ADMIN_ROLE, "Administrator",
                        "Every permission of the tenant (created at installation).", now, null);
                audit.recordCreate("iam.role", role);
            }
            int granted = roles.grantEveryPermission(tenant, role, now, null, clock);
            if (!users.usernameTaken(username)) {
                UUID admin = UuidV7.next(clock);
                users.insert(admin, tenant, username, hash, "admin", now, null);
                roles.assignTenantWide(UuidV7.next(clock), tenant, admin, role, now, null);
                audit.recordCreate(MeAccountEntity.TABLE, admin);
                LOG.info("bootstrap: administrator account created in tenant {}", tenant);
            }
            LOG.info("bootstrap: tenant {} ready ({} permissions added to the administrator role)", tenant, granted);
        });
    }
}
