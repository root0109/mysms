package com.mysms.core.db;

import java.time.Duration;

/**
 * `mysms.<module>.datasource.*`: where a module's schema lives, so moving a schema to its own PostgreSQL instance is a
 * configuration change.
 *
 * @param password         from the environment or a secret store; never committed
 * @param migrate          whether this process applies the module's Flyway migrations at startup
 * @param migrateUsername  the schema owner Flyway runs as (`mysms_migrate`); the application role
 *                         then only receives DML grants and can never alter tables or switch off row-level security.
 *                         Blank = migrate as the application role (local development only; logged as a warning)
 * @param relayUsername    the role the outbox relay reads every tenant's pending events as (`mysms_relay`,
 *                         BYPASSRLS). Blank = the application role, which works only when it bypasses row-level
 *                         security itself (local development as a superuser)
 * @param slowStatement    how long a statement may take before it is named in the log (default 200 ms; zero or less
 *                         switches the report off)
 * @param replicaUrl       JDBC URL of a streaming replica that read-only transactions use; blank = none, every read
 *                         goes to the primary. Never migrated: it follows the primary
 * @param replicaUsername  role on the replica; blank = the application role
 * @param replicaPassword  blank = the application role's
 */
public record ModuleDatabaseSettings(String url, String username, String password, Integer maximumPoolSize,
                                     Duration transactionTimeout, Boolean migrate, String migrateUsername,
                                     String migratePassword, String relayUsername, String relayPassword,
                                     Duration slowStatement, String replicaUrl, String replicaUsername,
                                     String replicaPassword) {

    public ModuleDatabaseSettings {
        if (url == null || url.isBlank()) {
            throw new IllegalArgumentException("datasource url is required");
        }
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("datasource username is required");
        }
        maximumPoolSize = maximumPoolSize == null ? Integer.valueOf(10) : maximumPoolSize;
        transactionTimeout = transactionTimeout == null ? Duration.ofSeconds(30) : transactionTimeout;
        migrate = migrate == null ? Boolean.TRUE : migrate;
        slowStatement = slowStatement == null ? Duration.ofMillis(200) : slowStatement;
        migrateUsername = migrateUsername == null || migrateUsername.isBlank() ? null : migrateUsername;
        relayUsername = relayUsername == null || relayUsername.isBlank() ? null : relayUsername;
        replicaUrl = replicaUrl == null || replicaUrl.isBlank() ? null : replicaUrl;
        replicaUsername = replicaUsername == null || replicaUsername.isBlank() ? username : replicaUsername;
        replicaPassword = replicaPassword == null || replicaPassword.isBlank() ? password : replicaPassword;
    }

    /** True when read-only transactions have a replica of their own. */
    public boolean hasReplica() {
        return replicaUrl != null;
    }

    /** True when migrations run as a separate schema-owner role. */
    public boolean separateMigrationRole() {
        return migrateUsername != null && !migrateUsername.equals(username);
    }
}
