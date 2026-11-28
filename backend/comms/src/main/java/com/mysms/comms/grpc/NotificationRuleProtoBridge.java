package com.mysms.comms.grpc;

import com.mysms.comms.api.model.NotificationRuleUpdate;
import com.mysms.core.grpc.ProtoBridge;

/**
 * UpdateNotificationRule over gRPC (JAVA-CODING-RULES §7.2): the {@code comms.v1.NotificationRuleUpdate} message
 * as REST's {@link NotificationRuleUpdate} body, one explicit line per field: unset keeps the value, null
 * clears it, a value sets it.
 */
final class NotificationRuleProtoBridge {

    private NotificationRuleProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static NotificationRuleUpdate changes(ProtoBridge bridge, com.mysms.comms.v1.NotificationRuleUpdate request) {
        NotificationRuleUpdate changes = new NotificationRuleUpdate();
        changes.setRoleId(bridge.uuid(request.hasRoleId(), request.getRoleId(), "role_id"));
        changes.setPermissionId(bridge.uuid(request.hasPermissionId(), request.getPermissionId(), "permission_id"));
        changes.setIsEnabled(ProtoBridge.flag(request.hasIsEnabled(), request.getIsEnabled()));
        changes.setEmailSubjectTemplate(ProtoBridge.text(
                request.hasEmailSubjectTemplate(), request.getEmailSubjectTemplate()));
        changes.setEmailBodyTemplate(ProtoBridge.text(request.hasEmailBodyTemplate(), request.getEmailBodyTemplate()));
        changes.setSmsTemplate(ProtoBridge.text(request.hasSmsTemplate(), request.getSmsTemplate()));
        return bridge.patch(changes);
    }
}
