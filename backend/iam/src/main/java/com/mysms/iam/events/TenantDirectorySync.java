package com.mysms.iam.events;

import com.mysms.core.db.AppTransactionManager;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.iam.config.IamDatabaseConfig;
import com.mysms.iam.repository.TenantDirectoryRepository;
import com.mysms.messaging.events.EventEnvelope;
import com.mysms.messaging.events.EventHandler;
import com.mysms.messaging.events.EventSubscriptions;
import com.mysms.messaging.events.RejectedEventException;
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
 * Keeps iam.tenant_directory — the tenant codes and status sign-in resolves before a tenant is known — in step with
 * platform.tenant. Tenant events carry the tenant's code, status and the row version
 * they produced; an entry changes only to a newer version, so out-of-order and repeated deliveries are harmless. A
 * deleted tenant is closed: nobody signs in to it.
 */
@Component
public class TenantDirectorySync extends AppTransactionManager implements EventSubscriptions {

    static final String CREATED = "platform.tenant.created";
    static final String UPDATED = "platform.tenant.updated";
    static final String DELETED = "platform.tenant.deleted";
    private static final Set<String> STATUSES = Set.of("active", "suspended", "closed");

    private final TenantDirectoryRepository directory;
    private final JsonMapper json;
    private final Clock clock;

    public TenantDirectorySync(TenantDirectoryRepository directory,
                               @Qualifier(IamDatabaseConfig.DATABASE) ModuleDatabase database,
            @Qualifier(IamDatabaseConfig.TRANSACTIONS) PlatformTransactionManager transactions, JsonMapper json,
                               Clock clock) {

        super(database, transactions);
        this.directory = directory;
        this.json = json;
        this.clock = clock;
    }

    @Override
    public String module() {
        return "iam";
    }

    @Override
    public Map<String, EventHandler> handlers() {
        return Map.of(CREATED, this::apply, UPDATED, this::apply, DELETED, this::apply);
    }

    /**
     * Applies one tenant event; returns whether the directory changed. Tenant events have no tenant of their own (the
     * table is global), so they bypass the inbox and this runs its own transaction, idempotent by version.
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
        // the directory is global: its transaction binds no tenant
        return doInGlobalWriteTransaction(() -> {
            return directory.apply(event.aggregateId(), code, status, version, now);
        });
    }
}
