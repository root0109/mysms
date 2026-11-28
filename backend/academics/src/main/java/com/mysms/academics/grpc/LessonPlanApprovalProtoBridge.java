package com.mysms.academics.grpc;

import com.mysms.academics.api.model.LessonPlanApprovalUpdate;
import com.mysms.core.grpc.ProtoBridge;

/**
 * UpdateLessonPlanApproval over gRPC (JAVA-CODING-RULES §7.2): the {@code academics.v1.LessonPlanApprovalUpdate}
 * message as REST's {@link LessonPlanApprovalUpdate} body, one explicit line per field: unset keeps the value, null
 * clears it, a value sets it.
 */
final class LessonPlanApprovalProtoBridge {

    private LessonPlanApprovalProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static LessonPlanApprovalUpdate changes(
            ProtoBridge bridge, com.mysms.academics.v1.LessonPlanApprovalUpdate request) {
        LessonPlanApprovalUpdate changes = new LessonPlanApprovalUpdate();
        changes.setDecision(bridge.choice(
                request.hasDecision(), request.getDecision(), LessonPlanApprovalUpdate.DecisionEnum::fromValue));
        changes.setComments(ProtoBridge.text(request.hasComments(), request.getComments()));
        changes.setDecidedByUserId(bridge.uuid(
                request.hasDecidedByUserId(), request.getDecidedByUserId(), "decided_by_user_id"));
        changes.setDecidedAt(ProtoBridge.time(request.hasDecidedAt(), request.getDecidedAt()));
        return bridge.patch(changes);
    }
}
