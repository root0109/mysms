package com.mysms.platform.events;

import com.mysms.core.error.DuplicateResourceException;
import com.mysms.messaging.events.EventEnvelope;
import com.mysms.messaging.events.EventHandler;
import com.mysms.messaging.events.EventSubscriptions;
import com.mysms.messaging.events.RejectedEventException;
import com.mysms.platform.business.TenantService;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Registers the tenant iam's installation bootstrap created, so the tenant's audit trail and other platform rows can
 * refer to it. An event naming a tenant other than its own, or a code that is not a tenant code or that another tenant
 * holds, can never succeed and is rejected.
 */
@Component
public class PlatformTenantSync implements EventSubscriptions {

    static final String BOOTSTRAPPED = "iam.tenant_directory.bootstrapped";
    private static final Pattern CODE = Pattern.compile("[a-z0-9][a-z0-9-]{1,49}");

    private final TenantService tenants;
    private final JsonMapper json;

    public PlatformTenantSync(TenantService tenants, JsonMapper json) {
        this.tenants = tenants;
        this.json = json;
    }

    @Override
    public String module() {
        return "platform";
    }

    @Override
    public Map<String, EventHandler> handlers() {
        return Map.of(BOOTSTRAPPED, this::register);
    }

    /** Registers the event's tenant unless the platform has it already. */
    public void register(EventEnvelope event) {
        JsonNode data = json.readTree(event.data());
        String code = data.path("code").asString("");
        if (!event.tenantId().equals(Optional.of(event.aggregateId()))
                || !data.path("id").asString("").equals(event.aggregateId().toString())
                || !CODE.matcher(code).matches()) {
            throw new RejectedEventException("bootstrap event " + event.id() + " lacks its own tenant or a valid code");
        }
        try {
            tenants.registerBootstrapTenant(event.aggregateId(), code);
        } catch (DuplicateResourceException e) {
            throw new RejectedEventException("bootstrap event " + event.id() + " names a code another tenant holds");
        }
    }
}
