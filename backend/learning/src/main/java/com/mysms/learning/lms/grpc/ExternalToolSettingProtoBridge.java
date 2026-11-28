package com.mysms.learning.lms.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.learning.lms.api.model.ExternalToolSettingUpdate;

/**
 * UpdateExternalToolSetting over gRPC (JAVA-CODING-RULES §7.2): the {@code lms.v1.ExternalToolSettingUpdate} message as
 * REST's {@link ExternalToolSettingUpdate} body, one explicit line per field: unset keeps the value, null clears it, a
 * value sets it.
 */
final class ExternalToolSettingProtoBridge {

    private ExternalToolSettingProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static ExternalToolSettingUpdate changes(ProtoBridge bridge, com.mysms.lms.v1.ExternalToolSettingUpdate request) {
        ExternalToolSettingUpdate changes = new ExternalToolSettingUpdate();
        changes.setSettingKey(ProtoBridge.text(request.hasSettingKey(), request.getSettingKey()));
        changes.setSettingValue(ProtoBridge.text(request.hasSettingValue(), request.getSettingValue()));
        changes.setSecretValue(ProtoBridge.text(request.hasSecretValue(), request.getSecretValue()));
        return bridge.patch(changes);
    }
}
