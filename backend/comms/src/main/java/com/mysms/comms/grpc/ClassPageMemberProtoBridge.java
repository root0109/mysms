package com.mysms.comms.grpc;

import com.mysms.comms.api.model.ClassPageMemberUpdate;
import com.mysms.core.grpc.ProtoBridge;

/**
 * UpdateClassPageMember over gRPC (JAVA-CODING-RULES §7.2): the {@code comms.v1.ClassPageMemberUpdate} message
 * as REST's {@link ClassPageMemberUpdate} body, one explicit line per field: unset keeps the value, null
 * clears it, a value sets it.
 */
final class ClassPageMemberProtoBridge {

    private ClassPageMemberProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static ClassPageMemberUpdate changes(ProtoBridge bridge, com.mysms.comms.v1.ClassPageMemberUpdate request) {
        ClassPageMemberUpdate changes = new ClassPageMemberUpdate();
        changes.setUserId(bridge.uuid(request.hasUserId(), request.getUserId(), "user_id"));
        changes.setIsFollowing(ProtoBridge.flag(request.hasIsFollowing(), request.getIsFollowing()));
        changes.setReaction(bridge.choice(
                request.hasReaction(), request.getReaction(), ClassPageMemberUpdate.ReactionEnum::fromValue));
        changes.setIsActive(ProtoBridge.flag(request.hasIsActive(), request.getIsActive()));
        return bridge.patch(changes);
    }
}
