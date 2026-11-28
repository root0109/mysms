package com.mysms.comms.grpc;

import com.mysms.comms.api.model.NotificationCohortSettingUpdate;
import com.mysms.core.grpc.ProtoBridge;

/**
 * UpdateNotificationCohortSetting over gRPC (JAVA-CODING-RULES §7.2): the
 * {@code comms.v1.NotificationCohortSettingUpdate} message as REST's {@link NotificationCohortSettingUpdate} body,
 * one explicit line per field: unset keeps the value, null clears it, a value sets it.
 */
final class NotificationCohortSettingProtoBridge {

    private NotificationCohortSettingProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static NotificationCohortSettingUpdate changes(ProtoBridge bridge,
            com.mysms.comms.v1.NotificationCohortSettingUpdate request) {
        NotificationCohortSettingUpdate changes = new NotificationCohortSettingUpdate();
        changes.setCohortId(bridge.uuid(request.hasCohortId(), request.getCohortId(), "cohort_id"));
        changes.setPopupEnabled(ProtoBridge.flag(request.hasPopupEnabled(), request.getPopupEnabled()));
        return bridge.patch(changes);
    }
}
