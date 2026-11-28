package com.mysms.identity.iam.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.identity.iam.api.model.UserPreferenceUpdate;

/**
 * UpdateUserPreference over gRPC (JAVA-CODING-RULES §7.2): the {@code iam.v1.UserPreferenceUpdate} message
 * as REST's {@link UserPreferenceUpdate} body, one explicit line per field: unset keeps the value, null
 * clears it, a value sets it.
 */
final class UserPreferenceProtoBridge {

    private UserPreferenceProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static UserPreferenceUpdate changes(ProtoBridge bridge, com.mysms.iam.v1.UserPreferenceUpdate request) {
        UserPreferenceUpdate changes = new UserPreferenceUpdate();
        changes.setPrefKey(ProtoBridge.text(request.hasPrefKey(), request.getPrefKey()));
        changes.setPrefValue(ProtoBridge.text(request.hasPrefValue(), request.getPrefValue()));
        return bridge.patch(changes);
    }
}
