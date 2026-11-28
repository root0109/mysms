package com.mysms.comms.grpc;

import com.mysms.comms.api.model.NotificationChannelUpdate;
import com.mysms.core.grpc.ProtoBridge;

/**
 * UpdateNotificationChannel over gRPC (JAVA-CODING-RULES §7.2): the {@code comms.v1.NotificationChannelUpdate} message
 * as REST's {@link NotificationChannelUpdate} body, one explicit line per field: unset keeps the value, null
 * clears it, a value sets it.
 */
final class NotificationChannelProtoBridge {

    private NotificationChannelProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static NotificationChannelUpdate changes(ProtoBridge bridge, com.mysms.comms.v1.NotificationChannelUpdate request) {
        NotificationChannelUpdate changes = new NotificationChannelUpdate();
        changes.setCode(ProtoBridge.text(request.hasCode(), request.getCode()));
        changes.setDescription(ProtoBridge.text(request.hasDescription(), request.getDescription()));
        changes.setIsEnabled(ProtoBridge.flag(request.hasIsEnabled(), request.getIsEnabled()));
        changes.setSortOrder(ProtoBridge.number(request.hasSortOrder(), request.getSortOrder()));
        return bridge.patch(changes);
    }
}
