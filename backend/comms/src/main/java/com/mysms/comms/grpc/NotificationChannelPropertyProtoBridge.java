package com.mysms.comms.grpc;

import com.mysms.comms.api.model.NotificationChannelPropertyUpdate;
import com.mysms.core.grpc.ProtoBridge;

/**
 * UpdateNotificationChannelProperty over gRPC (JAVA-CODING-RULES §7.2): the
 * {@code comms.v1.NotificationChannelPropertyUpdate} message as REST's {@link NotificationChannelPropertyUpdate} body,
 * one explicit line per field: unset keeps the value, null clears it, a value sets it.
 */
final class NotificationChannelPropertyProtoBridge {

    private NotificationChannelPropertyProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static NotificationChannelPropertyUpdate changes(ProtoBridge bridge,
            com.mysms.comms.v1.NotificationChannelPropertyUpdate request) {
        NotificationChannelPropertyUpdate changes = new NotificationChannelPropertyUpdate();
        changes.setPropertyKey(ProtoBridge.text(request.hasPropertyKey(), request.getPropertyKey()));
        changes.setPropertyValue(ProtoBridge.text(request.hasPropertyValue(), request.getPropertyValue()));
        changes.setSecretValue(ProtoBridge.text(request.hasSecretValue(), request.getSecretValue()));
        changes.setSortOrder(ProtoBridge.number(request.hasSortOrder(), request.getSortOrder()));
        changes.setIsMandatory(ProtoBridge.flag(request.hasIsMandatory(), request.getIsMandatory()));
        return bridge.patch(changes);
    }
}
