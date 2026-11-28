package com.mysms.students.admission.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.students.admission.api.model.ApplicationEmergencyContactUpdate;

/**
 * UpdateApplicationEmergencyContact over gRPC (JAVA-CODING-RULES §7.2): the {@code
 * admission.v1.ApplicationEmergencyContactUpdate} message as REST's {@link ApplicationEmergencyContactUpdate} body, one
 * explicit line per field: unset keeps the value, null clears it, a value sets it.
 */
final class ApplicationEmergencyContactProtoBridge {

    private ApplicationEmergencyContactProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static ApplicationEmergencyContactUpdate changes(
            ProtoBridge bridge, com.mysms.admission.v1.ApplicationEmergencyContactUpdate request) {
        ApplicationEmergencyContactUpdate changes = new ApplicationEmergencyContactUpdate();
        changes.setPriority(ProtoBridge.number(request.hasPriority(), request.getPriority()));
        changes.setContactNumber(ProtoBridge.text(request.hasContactNumber(), request.getContactNumber()));
        changes.setRelationship(ProtoBridge.text(request.hasRelationship(), request.getRelationship()));
        return bridge.patch(changes);
    }
}
