package com.mysms.identity.iam.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.identity.iam.api.model.UserDeviceUpdate;

/**
 * UpdateUserDevice over gRPC (JAVA-CODING-RULES §7.2): the {@code iam.v1.UserDeviceUpdate} message
 * as REST's {@link UserDeviceUpdate} body, one explicit line per field: unset keeps the value, null
 * clears it, a value sets it.
 */
final class UserDeviceProtoBridge {

    private UserDeviceProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static UserDeviceUpdate changes(ProtoBridge bridge, com.mysms.iam.v1.UserDeviceUpdate request) {
        UserDeviceUpdate changes = new UserDeviceUpdate();
        changes.setDeviceIdentifier(ProtoBridge.text(request.hasDeviceIdentifier(), request.getDeviceIdentifier()));
        changes.setPlatform(bridge.choice(
                request.hasPlatform(), request.getPlatform(), UserDeviceUpdate.PlatformEnum::fromValue));
        changes.setPushToken(ProtoBridge.text(request.hasPushToken(), request.getPushToken()));
        changes.setLastSeenAt(ProtoBridge.time(request.hasLastSeenAt(), request.getLastSeenAt()));
        changes.setIsActive(ProtoBridge.flag(request.hasIsActive(), request.getIsActive()));
        return bridge.patch(changes);
    }
}
