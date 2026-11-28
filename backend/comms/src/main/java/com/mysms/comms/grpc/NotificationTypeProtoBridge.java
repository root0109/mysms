package com.mysms.comms.grpc;

import com.mysms.comms.api.model.NotificationTypeUpdate;
import com.mysms.core.grpc.ProtoBridge;

/**
 * UpdateNotificationType over gRPC (JAVA-CODING-RULES §7.2): the {@code comms.v1.NotificationTypeUpdate} message
 * as REST's {@link NotificationTypeUpdate} body, one explicit line per field: unset keeps the value, null
 * clears it, a value sets it.
 */
final class NotificationTypeProtoBridge {

    private NotificationTypeProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static NotificationTypeUpdate changes(ProtoBridge bridge, com.mysms.comms.v1.NotificationTypeUpdate request) {
        NotificationTypeUpdate changes = new NotificationTypeUpdate();
        changes.setCode(ProtoBridge.text(request.hasCode(), request.getCode()));
        changes.setDescription(ProtoBridge.text(request.hasDescription(), request.getDescription()));
        changes.setModuleCode(ProtoBridge.text(request.hasModuleCode(), request.getModuleCode()));
        changes.setRoleCode(ProtoBridge.text(request.hasRoleCode(), request.getRoleCode()));
        changes.setIsEnabled(ProtoBridge.flag(request.hasIsEnabled(), request.getIsEnabled()));
        changes.setPopupEnabled(ProtoBridge.flag(request.hasPopupEnabled(), request.getPopupEnabled()));
        changes.setTeacherPopupEnabled(ProtoBridge.flag(
                request.hasTeacherPopupEnabled(), request.getTeacherPopupEnabled()));
        changes.setValidParameters(ProtoBridge.text(request.hasValidParameters(), request.getValidParameters()));
        return bridge.patch(changes);
    }
}
