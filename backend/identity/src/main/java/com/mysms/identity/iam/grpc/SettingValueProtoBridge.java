package com.mysms.identity.iam.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.identity.iam.api.model.SettingValueUpdate;

/**
 * UpdateSettingValue over gRPC (JAVA-CODING-RULES §7.2): the {@code iam.v1.SettingValueUpdate} message
 * as REST's {@link SettingValueUpdate} body, one explicit line per field: unset keeps the value, null
 * clears it, a value sets it.
 */
final class SettingValueProtoBridge {

    private SettingValueProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static SettingValueUpdate changes(ProtoBridge bridge, com.mysms.iam.v1.SettingValueUpdate request) {
        SettingValueUpdate changes = new SettingValueUpdate();
        changes.setValue(ProtoBridge.text(request.hasValue(), request.getValue()));
        changes.setIsLocked(ProtoBridge.flag(request.hasIsLocked(), request.getIsLocked()));
        return bridge.patch(changes);
    }
}
