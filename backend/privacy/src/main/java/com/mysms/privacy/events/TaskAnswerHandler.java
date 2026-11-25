package com.mysms.privacy.events;

import com.mysms.messaging.events.EventEnvelope;
import com.mysms.messaging.events.EventHandler;
import com.mysms.messaging.events.EventSubscriptions;
import com.mysms.messaging.events.RejectedEventException;
import com.mysms.privacy.business.RightsRequestSaga;
import com.mysms.privacy.model.TaskAnswer;
import com.mysms.privacy.model.TaskResult;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Receives each module's answer to its rights-request task: {@code <module>.privacy_task.completed} of every module of
 * {@code mysms.privacy.erasure.modules}, about the task it names ({@code taskId}, the event's aggregate id), with
 * {@code requestId}, {@code status} (done, partial, failed, not_applicable) and, each optional, {@code counts} (rows per
 * table), {@code heldItems}, {@code policyCodes}, {@code fileId} and, when failed, {@code errorCode} — ids, counts and
 * codes only. The answer is read strictly: a malformed one, or one about no task of the tenant's that module holds, is
 * rejected (never retried); a late or repeated one changes nothing. Runs inside the event's tenant (messaging's
 * InboxProcessor: exactly once per event); the saga records it ({@link RightsRequestSaga#answer}).
 */
@Component
public final class TaskAnswerHandler implements EventSubscriptions {

    static final String COMPLETED = ".privacy_task.completed";
    private static final Set<String> STATUSES = Set.of("done", "partial", "failed", "not_applicable");
    private static final int MAX_CODE = 60;

    private final RightsRequestSaga saga;
    private final JsonMapper json;

    public TaskAnswerHandler(RightsRequestSaga saga, JsonMapper json) {
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

    /** Records one module's answer; whether a task changed (false for a late or repeated answer). */
    public boolean apply(EventEnvelope event) {
        if (!event.type().equals(event.module() + COMPLETED)) {
            throw rejected(event, "is not a module's answer to its own task");
        }
        JsonNode data = json.readTree(event.data());
        UUID taskId = id(event, data, "taskId", true);
        if (!taskId.equals(event.aggregateId())) {
            throw rejected(event, "is not about the task it names");
        }
        String status = text(event, data, "status", true);
        if (!STATUSES.contains(status)) {
            throw rejected(event, "has no valid status");
        }
        boolean failed = "failed".equals(status);
        TaskAnswer answer = new TaskAnswer(taskId, id(event, data, "requestId", true), event.module(), status,
                failed ? null : result(event, data), failed ? text(event, data, "errorCode", false) : null);
        return switch (saga.answer(answer)) {
            case RECORDED -> true;
            case IGNORED -> false;
            case UNKNOWN -> throw rejected(event, "is about no task of this module");
        };
    }

    private static TaskResult result(EventEnvelope event, JsonNode data) {
        JsonNode counts = data.path("counts");
        Map<String, Integer> rows = new HashMap<>();
        if (!absent(counts)) {
            if (!counts.isObject()) {
                throw rejected(event, "carries malformed counts");
            }
            for (Map.Entry<String, JsonNode> table : counts.properties()) {
                rows.put(table.getKey(), count(event, table.getValue()));
            }
        }
        JsonNode codes = data.path("policyCodes");
        List<String> policyCodes = new ArrayList<>();
        if (!absent(codes)) {
            if (!codes.isArray()) {
                throw rejected(event, "carries malformed policyCodes");
            }
            for (JsonNode code : codes) {
                policyCodes.add(code(event, code));
            }
        }
        JsonNode held = data.path("heldItems");
        return new TaskResult(rows, absent(held) ? 0 : count(event, held), policyCodes, id(event, data, "fileId", false));
    }

    /** A field the answer leaves out or sends as null. */
    private static boolean absent(JsonNode value) {
        return value.isMissingNode() || value.isNull();
    }

    private static int count(EventEnvelope event, JsonNode value) {
        if (!value.isInt() || value.intValue() < 0) {
            throw rejected(event, "carries a malformed count");
        }
        return value.intValue();
    }

    private static String code(EventEnvelope event, JsonNode value) {
        if (!value.isString() || value.asString().isEmpty() || value.asString().length() > MAX_CODE) {
            throw rejected(event, "carries a malformed code");
        }
        return value.asString();
    }

    private static String text(EventEnvelope event, JsonNode data, String field, boolean required) {
        JsonNode value = data.path(field);
        if (!required && absent(value)) {
            return null;
        }
        return code(event, value);
    }

    private static UUID id(EventEnvelope event, JsonNode data, String field, boolean required) {
        JsonNode value = data.path(field);
        if (!required && absent(value)) {
            return null;
        }
        try {
            return UUID.fromString(code(event, value));
        } catch (IllegalArgumentException malformed) {
            throw rejected(event, "carries a malformed " + field);
        }
    }

    private static RejectedEventException rejected(EventEnvelope event, String why) {
        return new RejectedEventException(event.type() + " event " + event.id() + " " + why);
    }
}
