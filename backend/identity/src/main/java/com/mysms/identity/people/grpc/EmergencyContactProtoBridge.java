package com.mysms.identity.people.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.identity.people.api.model.EmergencyContactUpdate;

/**
 * UpdateEmergencyContact over gRPC (JAVA-CODING-RULES §7.2): the {@code people.v1.EmergencyContactUpdate} message
 * as REST's {@link EmergencyContactUpdate} body, one explicit line per field: unset keeps the value, null
 * clears it, a value sets it.
 */
final class EmergencyContactProtoBridge {

    private EmergencyContactProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static EmergencyContactUpdate changes(ProtoBridge bridge, com.mysms.people.v1.EmergencyContactUpdate request) {
        EmergencyContactUpdate changes = new EmergencyContactUpdate();
        changes.setContactPersonId(bridge.uuid(
                request.hasContactPersonId(), request.getContactPersonId(), "contact_person_id"));
        changes.setContactName(ProtoBridge.text(request.hasContactName(), request.getContactName()));
        changes.setRelationship(ProtoBridge.text(request.hasRelationship(), request.getRelationship()));
        changes.setPhone(ProtoBridge.text(request.hasPhone(), request.getPhone()));
        changes.setPriority(ProtoBridge.number(request.hasPriority(), request.getPriority()));
        return bridge.patch(changes);
    }
}
