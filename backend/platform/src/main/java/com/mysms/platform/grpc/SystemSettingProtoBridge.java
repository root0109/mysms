package com.mysms.platform.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.platform.api.model.SystemSettingUpdate;

/**
 * UpdateSystemSetting over gRPC (JAVA-CODING-RULES §7.2): the {@code platform.v1.SystemSettingUpdate} message
 * as REST's {@link SystemSettingUpdate} body, one explicit line per field: unset keeps the value, null
 * clears it, a value sets it.
 */
final class SystemSettingProtoBridge {

    private SystemSettingProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static SystemSettingUpdate changes(ProtoBridge bridge, com.mysms.platform.v1.SystemSettingUpdate request) {
        SystemSettingUpdate changes = new SystemSettingUpdate();
        changes.setSettingKey(ProtoBridge.text(request.hasSettingKey(), request.getSettingKey()));
        changes.setName(ProtoBridge.text(request.hasName(), request.getName()));
        changes.setDescription(ProtoBridge.text(request.hasDescription(), request.getDescription()));
        changes.setValue(ProtoBridge.text(request.hasValue(), request.getValue()));
        changes.setInputType(ProtoBridge.text(request.hasInputType(), request.getInputType()));
        changes.setIsActive(ProtoBridge.flag(request.hasIsActive(), request.getIsActive()));
        return bridge.patch(changes);
    }
}
