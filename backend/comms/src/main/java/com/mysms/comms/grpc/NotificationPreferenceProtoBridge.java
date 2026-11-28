package com.mysms.comms.grpc;

import com.mysms.comms.api.model.NotificationPreferenceUpdate;
import com.mysms.core.grpc.ProtoBridge;

/**
 * UpdateNotificationPreference over gRPC (JAVA-CODING-RULES §7.2): the {@code comms.v1.NotificationPreferenceUpdate} message
 * as REST's {@link NotificationPreferenceUpdate} body, one explicit line per field: unset keeps the value, null
 * clears it, a value sets it.
 */
final class NotificationPreferenceProtoBridge {

    private NotificationPreferenceProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static NotificationPreferenceUpdate changes(ProtoBridge bridge, com.mysms.comms.v1.NotificationPreferenceUpdate request) {
        NotificationPreferenceUpdate changes = new NotificationPreferenceUpdate();
        changes.setNotificationTypeId(bridge.uuid(
                request.hasNotificationTypeId(), request.getNotificationTypeId(), "notification_type_id"));
        changes.setPropertyName(ProtoBridge.text(request.hasPropertyName(), request.getPropertyName()));
        changes.setPropertyValue(ProtoBridge.text(request.hasPropertyValue(), request.getPropertyValue()));
        return bridge.patch(changes);
    }
}
