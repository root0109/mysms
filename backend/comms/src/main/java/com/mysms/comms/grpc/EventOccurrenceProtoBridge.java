package com.mysms.comms.grpc;

import com.mysms.comms.api.model.EventOccurrenceUpdate;
import com.mysms.core.grpc.ProtoBridge;

/**
 * UpdateEventOccurrence over gRPC (JAVA-CODING-RULES §7.2): the {@code comms.v1.EventOccurrenceUpdate} message
 * as REST's {@link EventOccurrenceUpdate} body, one explicit line per field: unset keeps the value, null
 * clears it, a value sets it.
 */
final class EventOccurrenceProtoBridge {

    private EventOccurrenceProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static EventOccurrenceUpdate changes(ProtoBridge bridge, com.mysms.comms.v1.EventOccurrenceUpdate request) {
        EventOccurrenceUpdate changes = new EventOccurrenceUpdate();
        changes.setUserId(bridge.uuid(request.hasUserId(), request.getUserId(), "user_id"));
        changes.setCohortId(bridge.uuid(request.hasCohortId(), request.getCohortId(), "cohort_id"));
        changes.setTitle(ProtoBridge.text(request.hasTitle(), request.getTitle()));
        changes.setColor(ProtoBridge.text(request.hasColor(), request.getColor()));
        changes.setDescription(ProtoBridge.text(request.hasDescription(), request.getDescription()));
        changes.setStartAt(ProtoBridge.time(request.hasStartAt(), request.getStartAt()));
        changes.setEndAt(ProtoBridge.time(request.hasEndAt(), request.getEndAt()));
        changes.setIsCancelled(ProtoBridge.flag(request.hasIsCancelled(), request.getIsCancelled()));
        changes.setResponseStatus(bridge.choice(
                request.hasResponseStatus(), request.getResponseStatus(), EventOccurrenceUpdate.ResponseStatusEnum::fromValue));
        changes.setImportance(bridge.choice(
                request.hasImportance(), request.getImportance(), EventOccurrenceUpdate.ImportanceEnum::fromValue));
        return bridge.patch(changes);
    }
}
