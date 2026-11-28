package com.mysms.learning.lms.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.learning.lms.api.model.LearningProgramMemberUpdate;

/**
 * UpdateLearningProgramMember over gRPC (JAVA-CODING-RULES §7.2): the {@code lms.v1.LearningProgramMemberUpdate}
 * message as REST's {@link LearningProgramMemberUpdate} body, one explicit line per field: unset keeps the value, null
 * clears it, a value sets it.
 */
final class LearningProgramMemberProtoBridge {

    private LearningProgramMemberProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static LearningProgramMemberUpdate changes(
            ProtoBridge bridge, com.mysms.lms.v1.LearningProgramMemberUpdate request) {
        LearningProgramMemberUpdate changes = new LearningProgramMemberUpdate();
        changes.setUserId(bridge.uuid(request.hasUserId(), request.getUserId(), "user_id"));
        changes.setProgressPercent(ProtoBridge.text(request.hasProgressPercent(), request.getProgressPercent()));
        return bridge.patch(changes);
    }
}
