package com.mysms.platform.org.events;

import com.mysms.core.db.AppTransactionManager;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.messaging.events.EventEnvelope;
import com.mysms.messaging.events.EventHandler;
import com.mysms.messaging.events.EventSubscriptions;
import com.mysms.messaging.events.RejectedEventException;
import com.mysms.platform.org.config.OrgDatabaseConfig;
import com.mysms.platform.org.repository.DirectoryRepository;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Set;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Keeps org.tenant_directory — the tenant codes and status the public school finder shows and filters by — in step
 * with platform.tenant. Tenant events carry the tenant's code, status and the row version they produced; an entry
 * changes only to a newer version, so out-of-order and repeated deliveries are harmless. A deleted tenant is closed.
 */
@Component
public class TenantDirectorySync extends AppTransactionManager implements EventSubscriptions {

    static final String CREATED = "platform.tenant.created";
    static final String UPDATED = "platform.tenant.updated";
    static final String DELETED = "platform.tenant.deleted";
    private static final Set<String> STATUSES = Set.of("active", "suspended", "closed");

    private final DirectoryRepository directory;
    private final JsonMapper json;
    private final Clock clock;

    public TenantDirectorySync(DirectoryRepository directory,
            @Qualifier(OrgDatabaseConfig.DATABASE) ModuleDatabase database,
            @Qualifier(OrgDatabaseConfig.TRANSACTIONS) PlatformTransactionManager transactions, JsonMapper json,
            Clock clock) {
        super(database, transactions);
        this.directory = directory;
        this.json = json;
        this.clock = clock;
    }

    @Override
    public String module() {
        return "org";
    }

    @Override
    public Map<String, EventHandler> handlers() {
        return Map.of(CREATED, this::apply, UPDATED, this::apply, DELETED, this::apply);
    }

    /**
     * Applies one tenant event; whether the directory changed. Tenant events have no tenant of their own (the table is
     * global), so this runs its own transaction, binding no tenant, idempotent by version.
     */
    public boolean apply(EventEnvelope event) {
        JsonNode data = json.readTree(event.data());
        String code = data.path("code").asString(null);
        String status = DELETED.equals(event.type()) ? "closed" : data.path("status").asString(null);
        int version = data.path("rowVersion").asInt(0);
        if (code == null || status == null || version < 1 || !STATUSES.contains(status)
                || !data.path("id").asString("").equals(event.aggregateId().toString())) {
            throw new RejectedEventException("tenant event " + event.id() + " lacks its id, code, status or version");
        }
        OffsetDateTime now = OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC);
        return doInGlobalWriteTransaction(() -> directory.applyTenant(event.aggregateId(), code, status, version, now));
    }
}
