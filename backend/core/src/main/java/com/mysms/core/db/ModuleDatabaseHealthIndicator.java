package com.mysms.core.db;

import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;

/** Readiness of one module's database; each module registers one for its own {@link ModuleDatabase}. */
public final class ModuleDatabaseHealthIndicator implements HealthIndicator {

    private final ModuleDatabase database;

    public ModuleDatabaseHealthIndicator(ModuleDatabase database) {
        this.database = database;
    }

    @Override
    public Health health() {
        Health.Builder builder = database.isReachable() ? Health.up() : Health.down();
        return builder.withDetail("module", database.module()).build();
    }
}
