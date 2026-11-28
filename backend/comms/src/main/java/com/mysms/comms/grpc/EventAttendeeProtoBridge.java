package com.mysms.comms.grpc;

import com.mysms.comms.api.model.EventAttendeeUpdate;
import com.mysms.core.grpc.ProtoBridge;

/**
 * UpdateEventAttendee over gRPC (JAVA-CODING-RULES §7.2): the {@code comms.v1.EventAttendeeUpdate} message
 * as REST's {@link EventAttendeeUpdate} body, one explicit line per field: unset keeps the value, null
 * clears it, a value sets it.
 */
final class EventAttendeeProtoBridge {

    private EventAttendeeProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static EventAttendeeUpdate changes(ProtoBridge bridge, com.mysms.comms.v1.EventAttendeeUpdate request) {
        EventAttendeeUpdate changes = new EventAttendeeUpdate();
        changes.setUserId(bridge.uuid(request.hasUserId(), request.getUserId(), "user_id"));
        return bridge.patch(changes);
    }
}
