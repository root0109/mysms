package com.mysms.core.db;

import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;

/**
 * Readiness of one module's database; each module registers one for its own {@link ModuleDatabase}. The module is
 * ready while its primary answers; the replica is reported as a detail only, because reads fall back to the primary
 * when it does not answer ({@link RoutingDataSource}).
 */
public final class ModuleDatabaseHealthIndicator implements HealthIndicator {

    private final ModuleDatabase database;

    public ModuleDatabaseHealthIndicator(ModuleDatabase database) {
        this.database = database;
    }

    @Override
    public Health health() {
        boolean primary = database.isReachable(DatabaseRole.PRIMARY);
        Health.Builder builder = primary ? Health.up() : Health.down();
        builder.withDetail("module", database.module());
        if (database.hasReplica()) {
            builder.withDetail("replica", database.isReachable(DatabaseRole.REPLICA) ? "UP" : "DOWN");
        }
        return builder.build();
    }
}
