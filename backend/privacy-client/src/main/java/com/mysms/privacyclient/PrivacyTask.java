package com.mysms.privacyclient;

import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.UUID;
import tools.jackson.databind.JsonNode;

/**
 * A module's task of an erasure or export request, read from privacy.rights_request.task_assigned (design §6.3):
 * ids, codes and the request's deadline only. It carries no hold: the module asks privacy which holds apply
 * ({@link LegalHolds}) when it runs the task.
 *
 * @param taskId          the task, the id the module answers with
 * @param requestId       the rights request the task fulfils
 * @param type            the request's type (erasure or export)
 * @param action          what the module does
 * @param subjectPersonId the data principal
 * @param module          the module the task is for
 * @param wave            1 or 2: comms and iam stop messages and sign-in in wave 1
 * @param deadline        when the request is due
 */
public record PrivacyTask(UUID taskId, UUID requestId, String type, PrivacyTaskAction action, UUID subjectPersonId,
                          String module, int wave, OffsetDateTime deadline) {

    /** The task an event's data names; {@link IllegalArgumentException} when a field is missing or malformed. */
    public static PrivacyTask from(JsonNode data) {
        try {
            return new PrivacyTask(UUID.fromString(text(data, "taskId")), UUID.fromString(text(data, "requestId")),
                    text(data, "type"), PrivacyTaskAction.of(text(data, "action")),
                    UUID.fromString(text(data, "subjectPersonId")), text(data, "module"), wave(data),
                    OffsetDateTime.parse(text(data, "deadline")));
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("a malformed deadline", e);
        }
    }

    static String text(JsonNode data, String field) {
        JsonNode value = data.path(field);
        if (!value.isString() || value.asString().isEmpty()) {
            throw new IllegalArgumentException("no " + field);
        }
        return value.asString();
    }

    private static int wave(JsonNode data) {
        JsonNode value = data.path("wave");
        if (!value.isInt() || value.intValue() < 1) {
            throw new IllegalArgumentException("no wave");
        }
        return value.intValue();
    }
}
