package com.mysms.learning.lms.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.learning.lms.api.model.DriveItemShareUpdate;

/**
 * UpdateDriveItemShare over gRPC (JAVA-CODING-RULES §7.2): the {@code lms.v1.DriveItemShareUpdate} message as REST's
 * {@link DriveItemShareUpdate} body, one explicit line per field: unset keeps the value, null clears it, a value sets
 * it.
 */
final class DriveItemShareProtoBridge {

    private DriveItemShareProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static DriveItemShareUpdate changes(ProtoBridge bridge, com.mysms.lms.v1.DriveItemShareUpdate request) {
        DriveItemShareUpdate changes = new DriveItemShareUpdate();
        changes.setUserId(bridge.uuid(request.hasUserId(), request.getUserId(), "user_id"));
        changes.setReaction(bridge.choice(
                request.hasReaction(), request.getReaction(), DriveItemShareUpdate.ReactionEnum::fromValue));
        return bridge.patch(changes);
    }
}
