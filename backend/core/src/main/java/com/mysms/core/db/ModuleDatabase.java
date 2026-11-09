package com.mysms.core.db;

import com.mysms.core.tx.TenantTransactionExecutor;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.util.Optional;
import java.util.regex.Pattern;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.configuration.FluentConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.flywaydb.core.api.output.MigrateResult;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Everything one module needs to reach its own schema, and nothing that could reach another's
 * (JAVA-CODING-RULES §2): its own connection pool, transaction manager, {@link JdbcClient}, transaction executor and
 * Flyway history. Created by the module's database configuration from `mysms.<module>.datasource.*`.
 */
public final class ModuleDatabase implements AutoCloseable {

    private static final Pattern MODULE = Pattern.compile("^[a-z]+$");
    private static final Pattern ROLE = Pattern.compile("^[a-z_][a-z0-9_]{0,62}$");
    private static final Logger LOG = LoggerFactory.getLogger(ModuleDatabase.class);

    private final String module;
    private final HikariDataSource dataSource;
    private final PlatformTransactionManager transactionManager;
    private final JdbcClient jdbc;
    private final JdbcClient tenantJdbc;
    private final TenantTransactionExecutor tx;
    private final ModuleDatabaseSettings settings;
    private HikariDataSource relayPool;
    private final Object lock = new Object();

    private ModuleDatabase(String module, HikariDataSource dataSource, boolean tenantScoped,
                           ModuleDatabaseSettings settings) {
        this.module = module;
        this.settings = settings;
        this.dataSource = dataSource;
        this.transactionManager = new DataSourceTransactionManager(dataSource);
        this.jdbc = JdbcClient.create(dataSource);
        this.tenantJdbc = TenantJdbcClient.wrap(jdbc);
        this.tx = new TenantTransactionExecutor(transactionManager, jdbc, tenantScoped, settings.transactionTimeout());
    }

    /** Binds `mysms.<module>.datasource.*` and opens the module's database (migrating it unless disabled). */
    public static ModuleDatabase open(String module, Environment environment, boolean tenantScoped) {
        ModuleDatabaseSettings settings = Binder.get(environment)
                .bind("mysms." + module + ".datasource", Bindable.of(ModuleDatabaseSettings.class))
                .orElseThrow(() -> new IllegalStateException("mysms." + module + ".datasource is not configured"));
        return open(module, settings, tenantScoped);
    }

    public static ModuleDatabase open(String module, ModuleDatabaseSettings settings, boolean tenantScoped) {
        if (!MODULE.matcher(module).matches()) {
            throw new IllegalArgumentException("invalid module name " + module);
        }
        HikariConfig config = new HikariConfig();
        config.setPoolName(module);
        config.setJdbcUrl(settings.url());
        config.setUsername(settings.username());
        config.setPassword(settings.password());
        config.setMaximumPoolSize(settings.maximumPoolSize());
        HikariDataSource dataSource = new HikariDataSource(config);
        try {
            if (settings.migrate()) {
                migrate(module, dataSource, settings);
            }
            return new ModuleDatabase(module, dataSource, tenantScoped, settings);
        } catch (RuntimeException e) {
            dataSource.close();
            throw e;
        }
    }

    /**
     * Applies the module's own migrations (classpath:db/migration/&lt;module&gt;) to its own schema, with its own
     * history table in that schema. Never cleans. With a separate migration role, Flyway connects as the schema owner
     * and then grants the application role exactly USAGE on the schema, DML on its tables and use of its sequences.
     */
    static MigrateResult migrate(String module, DataSource applicationDataSource, ModuleDatabaseSettings settings) {
        FluentConfiguration flyway = Flyway.configure()
                .schemas(module)
                .defaultSchema(module)
                .createSchemas(true)
                .locations("classpath:db/migration/" + module)
                .cleanDisabled(true)
                .validateMigrationNaming(true)
                .failOnMissingLocations(true);
        if (settings.separateMigrationRole()) {
            if (!ROLE.matcher(settings.username()).matches()) {
                throw new IllegalArgumentException("invalid application role name for module " + module);
            }
            flyway.dataSource(settings.url(), settings.migrateUsername(), settings.migratePassword())
                    .callbacks(new ApplicationRoleGrants(module, settings.username(), relayRole(module, settings)));
        } else {
            LOG.warn("module {} migrates as its application role: development only, use a separate migrate-username",
                    module);
            flyway.dataSource(applicationDataSource);
        }
        return flyway.load().migrate();
    }

    private static String relayRole(String module, ModuleDatabaseSettings settings) {
        if (settings.relayUsername() == null) {
            return null;
        }
        if (!ROLE.matcher(settings.relayUsername()).matches()) {
            throw new IllegalArgumentException("invalid relay role name for module " + module);
        }
        if (settings.relayUsername().equals(settings.username())
                || settings.relayUsername().equals(settings.migrateUsername())) {
            throw new IllegalArgumentException("module " + module + ": the relay role must be a role of its own "
                    + "(mysms_relay), not the application or migration role");
        }
        return settings.relayUsername();
    }

    /**
     * Connections for the outbox relay, which reads every tenant's pending events: as the relay role (BYPASSRLS) when
     * one is configured, otherwise empty. A small pool of its own, never used for business queries.
     */
    public Optional<DataSource> relayDataSource() {
        synchronized (lock) {
            if (settings.relayUsername() == null) {
                return Optional.empty();
            }
            if (relayPool == null) {
                HikariConfig config = new HikariConfig();
                config.setPoolName(module + "-relay");
                config.setJdbcUrl(settings.url());
                config.setUsername(settings.relayUsername());
                config.setPassword(settings.relayPassword());
                config.setMaximumPoolSize(2);
                relayPool = new HikariDataSource(config);
            }
            return Optional.of(relayPool);
        }
    }

    public String module() {
        return module;
    }

    public DataSource dataSource() {
        return dataSource;
    }

    public PlatformTransactionManager transactionManager() {
        return transactionManager;
    }

    /** The module's client; it binds the call's tenant to every statement that filters on {@code :tenantId}. */
    public JdbcClient jdbc() {
        return tenantJdbc;
    }

    /**
     * Connections as the schema owner (the migrate role), for the DDL the application role may never run: partition
     * maintenance. Without a separate migrate role (local development) this is the application pool.
     */
    public DataSource ownerDataSource() {
        if (!settings.separateMigrationRole()) {
            return dataSource;
        }
        return new DriverManagerDataSource(settings.url(), settings.migrateUsername(), settings.migratePassword());
    }

    public TenantTransactionExecutor tx() {
        return tx;
    }

    /** True when the database answers; used by the module's health indicator. */
    public boolean isReachable() {
        try {
            Integer one = jdbc.sql("SELECT 1").query(Integer.class).single();
            return one == 1;
        } catch (RuntimeException e) {
            return false;
        }
    }

    @Override
    public void close() {
        synchronized (lock) {
            if (relayPool != null) {
                relayPool.close();
            }
            dataSource.close();
        }
    }
}
