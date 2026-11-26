package com.mysms.platform.org.events;

import com.mysms.messaging.events.EventEnvelope;
import com.mysms.messaging.events.EventHandler;
import com.mysms.messaging.events.EventSubscriptions;
import com.mysms.messaging.events.RejectedEventException;
import com.mysms.platform.org.repository.DirectoryRepository;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Keeps org.finder_listing — which schools opted in to the public school finder — in step with the institution values
 * of the iam setting {@code directory.listedInSchoolFinder}, which only administrators of the school (or of the
 * tenant) may set. org may not call iam (iam depends on org), so it learns them from iam's setting-value events; an
 * entry changes only to a newer version, so out-of-order and repeated deliveries are harmless. Values of other
 * settings, and events without the setting's key (a user's own settings), change nothing. Runs inside the event's
 * tenant (messaging's InboxProcessor: exactly once per event).
 */
@Component
public class FinderListingSync implements EventSubscriptions {

    /** The iam setting a school's administrators opt in with. */
    public static final String SETTING = "directory.listedInSchoolFinder";
    static final String CREATED = "iam.setting_value.created";
    static final String UPDATED = "iam.setting_value.updated";
    static final String DELETED = "iam.setting_value.deleted";

    private final DirectoryRepository directory;
    private final JsonMapper json;
    private final Clock clock;

    public FinderListingSync(DirectoryRepository directory, JsonMapper json, Clock clock) {
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

    /** Applies one setting-value event; whether the stored entry changed. */
    public boolean apply(EventEnvelope event) {
        JsonNode data = json.readTree(event.data());
        if (!SETTING.equals(data.path("settingKey").asString(""))) {
            return false;
        }
        int version = data.path("rowVersion").asInt(0);
        String institution = data.path("scopeId").asString("");
        UUID tenant = event.tenantId().orElse(null);
        if (version < 1 || tenant == null || !"institution".equals(data.path("scopeType").asString(""))
                || institution.isEmpty() || !data.path("id").asString("").equals(event.aggregateId().toString())) {
            throw new RejectedEventException("setting event " + event.id() + " lacks its id, institution or version");
        }
        try {
            return directory.applyListing(event.aggregateId(), tenant, UUID.fromString(institution),
                    "true".equals(data.path("value").asString("")), DELETED.equals(event.type()), version,
                    OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC));
        } catch (IllegalArgumentException malformed) {
            throw new RejectedEventException("setting event " + event.id() + " carries a malformed institution");
        }
    }
}
