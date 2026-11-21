package com.mysms.iam.events;

import com.mysms.iam.business.ApiPermissionService;
import com.mysms.messaging.events.EventEnvelope;
import com.mysms.messaging.events.EventHandler;
import com.mysms.messaging.events.EventSubscriptions;
import com.mysms.messaging.events.RejectedEventException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Receives every module's API-permission catalogue, {@code <module>.api_permissions.published}, which auth-client's
 * publisher sends at start-up and every {@code mysms.auth.catalogue-interval} (JAVA-CODING-RULES §16.2), and hands it
 * to {@link ApiPermissionService}. The events carry no tenant, so they bypass the inbox; applying is idempotent. A
 * catalogue that does not name its own module, lacks its hash or holds a malformed operation, or an operation or
 * permission code of another module, is rejected (parked), never half applied.
 */
@Component
public class ApiPermissionSync implements EventSubscriptions {

    /** Every module that serves an operation table and so publishes a catalogue (ARCHITECTURE.md §2, plus iam). */
    static final Set<String> PUBLISHERS = Set.of("master", "platform", "iam", "org", "people", "academics", "sis",
            "hr", "payroll", "admission", "attendance", "facility", "timetable", "assessment", "lms", "finance",
            "library", "transport", "comms", "frontoffice", "careers", "privacy");
    static final String EVENT_SUFFIX = ".api_permissions.published";
    private static final Set<String> AUDIENCES = Set.of("public", "internal", "platform");
    private static final Set<String> METHODS = Set.of("GET", "POST", "PUT", "PATCH", "DELETE");

    private final ApiPermissionService service;
    private final JsonMapper json;

    public ApiPermissionSync(ApiPermissionService service, JsonMapper json) {
        this.service = service;
        this.json = json;
    }

    @Override
    public String module() {
        return "iam";
    }

    @Override
    public Map<String, EventHandler> handlers() {
        EventHandler handler = this::apply;
        return PUBLISHERS.stream().collect(Collectors.toUnmodifiableMap(m -> m + EVENT_SUFFIX, m -> handler));
    }

    /** Applies one published catalogue. */
    public void apply(EventEnvelope event) {
        JsonNode data = json.readTree(event.data());
        String module = data.path("module").asString(null);
        String sha256 = data.path("sha256").asString(null);
        String release = data.path("release").asString(null);
        JsonNode entries = data.path("entries");
        if (!event.module().equals(module) || !event.type().equals(module + EVENT_SUFFIX) || sha256 == null
                || sha256.length() != 64 || release == null || release.isBlank() || release.length() > 60
                || !entries.isArray() || entries.isEmpty()) {
            throw new RejectedEventException("api-permission catalogue " + event.id()
                    + " lacks its own module, hash, release or entries");
        }
        List<ApiPermissionService.Operation> operations = new ArrayList<>();
        for (JsonNode entry : entries) {
            operations.add(operation(event, module, entry));
        }
        service.apply(new ApiPermissionService.Catalogue(module, release, sha256, operations));
    }

    private static ApiPermissionService.Operation operation(EventEnvelope event, String module, JsonNode entry) {
        String operationId = entry.path("operationId").asString(null);
        String method = entry.path("method").asString(null);
        String path = entry.path("path").asString(null);
        String permission = entry.path("permission").asString(null);
        String audience = entry.path("audience").asString(null);
        List<String> schemes = new ArrayList<>();
        entry.path("schemes").forEach(s -> schemes.add(s.asString()));
        if (operationId == null || !operationId.startsWith(module + ".") || !METHODS.contains(method)
                || path == null || !path.startsWith("/") || permission == null || !permission.startsWith(module + ".")
                || !AUDIENCES.contains(audience) || !entry.path("anonymous").isBoolean()) {
            throw new RejectedEventException("api-permission catalogue " + event.id() + " holds a malformed operation "
                    + operationId);
        }
        return new ApiPermissionService.Operation(operationId, method, path, permission, schemes,
                entry.path("anonymous").asBoolean(), audience);
    }
}
