package com.mysms.privacy.events;

import com.mysms.messaging.events.EventEnvelope;
import com.mysms.messaging.events.EventHandler;
import com.mysms.messaging.events.EventSubscriptions;
import com.mysms.messaging.events.RejectedEventException;
import com.mysms.privacy.business.RetentionRunService;
import com.mysms.privacy.business.RightsRequestSaga;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Receives each module's report on a retention run: {@code <module>.privacy_retention.completed} of every module of
 * {@code mysms.privacy.erasure.modules} (the modules holding personal data), about the run it names ({@code runId},
 * the event's aggregate id), with {@code status} (done, partial, failed), {@code affected} and {@code held} — counts
 * only. A malformed report, or one about no run the tenant sent that module, is rejected (never retried); a late or
 * repeated one changes nothing. Runs inside the event's tenant; {@link RetentionRunService#complete} records it.
 */
@Component
public final class RetentionAnswerHandler implements EventSubscriptions {

    static final String COMPLETED = ".privacy_retention.completed";
    private static final Set<String> STATUSES = Set.of("done", "partial", "failed");

    private final RetentionRunService runs;
    private final RightsRequestSaga saga;
    private final JsonMapper json;

    public RetentionAnswerHandler(RetentionRunService runs, RightsRequestSaga saga, JsonMapper json) {
        this.runs = runs;
        this.saga = saga;
        this.json = json;
    }

    @Override
    public String module() {
        return "privacy";
    }

    @Override
    public Map<String, EventHandler> handlers() {
        return saga.modules().stream().collect(Collectors.toUnmodifiableMap(m -> m + COMPLETED,
                m -> (EventHandler) this::apply));
    }

    /** Records one module's report; whether a run changed (false for a late or repeated report). */
    public boolean apply(EventEnvelope event) {
        if (!event.type().equals(event.module() + COMPLETED)) {
            throw rejected(event, "is not a module's report on its own run");
        }
        JsonNode data = json.readTree(event.data());
        UUID runId;
        try {
            runId = UUID.fromString(data.path("runId").asString(""));
        } catch (IllegalArgumentException malformed) {
            throw rejected(event, "carries a malformed runId");
        }
        if (!runId.equals(event.aggregateId())) {
            throw rejected(event, "is not about the run it names");
        }
        String status = data.path("status").asString("");
        if (!STATUSES.contains(status)) {
            throw rejected(event, "has no valid status");
        }
        return switch (runs.complete(runId, event.module(), status, count(event, data, "affected"),
                count(event, data, "held"))) {
            case RECORDED -> true;
            case IGNORED -> false;
            case UNKNOWN -> throw rejected(event, "is about no run of this module");
        };
    }

    private static int count(EventEnvelope event, JsonNode data, String field) {
        JsonNode value = data.path(field);
        if (!value.isInt() || value.intValue() < 0) {
            throw rejected(event, "carries a malformed " + field);
        }
        return value.intValue();
    }

    private static RejectedEventException rejected(EventEnvelope event, String why) {
        return new RejectedEventException(event.type() + " event " + event.id() + " " + why);
    }
}
