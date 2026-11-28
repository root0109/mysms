package com.mysms.comms.grpc;

import com.mysms.comms.api.model.EventInstitutionUpdate;
import com.mysms.core.grpc.ProtoBridge;

/**
 * UpdateEventInstitution over gRPC (JAVA-CODING-RULES §7.2): the {@code comms.v1.EventInstitutionUpdate} message
 * as REST's {@link EventInstitutionUpdate} body, one explicit line per field: unset keeps the value, null
 * clears it, a value sets it.
 */
final class EventInstitutionProtoBridge {

    private EventInstitutionProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static EventInstitutionUpdate changes(ProtoBridge bridge, com.mysms.comms.v1.EventInstitutionUpdate request) {
        EventInstitutionUpdate changes = new EventInstitutionUpdate();
        changes.setInstitutionId(bridge.uuid(request.hasInstitutionId(), request.getInstitutionId(), "institution_id"));
        return bridge.patch(changes);
    }
}
