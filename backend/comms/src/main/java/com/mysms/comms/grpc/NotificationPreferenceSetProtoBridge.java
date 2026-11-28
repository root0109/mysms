package com.mysms.comms.grpc;

import com.mysms.comms.api.model.NotificationPreferenceSetUpdate;
import com.mysms.core.grpc.ProtoBridge;

/**
 * UpdateNotificationPreferenceSet over gRPC (JAVA-CODING-RULES §7.2): the
 * {@code comms.v1.NotificationPreferenceSetUpdate} message as REST's {@link NotificationPreferenceSetUpdate} body,
 * one explicit line per field: unset keeps the value, null clears it, a value sets it.
 */
final class NotificationPreferenceSetProtoBridge {

    private NotificationPreferenceSetProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static NotificationPreferenceSetUpdate changes(ProtoBridge bridge,
            com.mysms.comms.v1.NotificationPreferenceSetUpdate request) {
        NotificationPreferenceSetUpdate changes = new NotificationPreferenceSetUpdate();
        changes.setUserId(bridge.uuid(request.hasUserId(), request.getUserId(), "user_id"));
        changes.setRoleId(bridge.uuid(request.hasRoleId(), request.getRoleId(), "role_id"));
        return bridge.patch(changes);
    }
}
