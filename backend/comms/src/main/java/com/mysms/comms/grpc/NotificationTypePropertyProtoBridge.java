package com.mysms.comms.grpc;

import com.mysms.comms.api.model.NotificationTypePropertyUpdate;
import com.mysms.core.grpc.ProtoBridge;

/**
 * UpdateNotificationTypeProperty over gRPC (JAVA-CODING-RULES §7.2): the {@code comms.v1.NotificationTypePropertyUpdate} message
 * as REST's {@link NotificationTypePropertyUpdate} body, one explicit line per field: unset keeps the value, null
 * clears it, a value sets it.
 */
final class NotificationTypePropertyProtoBridge {

    private NotificationTypePropertyProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static NotificationTypePropertyUpdate changes(ProtoBridge bridge, com.mysms.comms.v1.NotificationTypePropertyUpdate request) {
        NotificationTypePropertyUpdate changes = new NotificationTypePropertyUpdate();
        changes.setModuleCode(ProtoBridge.text(request.hasModuleCode(), request.getModuleCode()));
        changes.setPropertyValue(ProtoBridge.text(request.hasPropertyValue(), request.getPropertyValue()));
        return bridge.patch(changes);
    }
}
