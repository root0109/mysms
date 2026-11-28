package com.mysms.learning.lms.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.learning.lms.api.model.CohortFeedbackUpdate;

/**
 * UpdateCohortFeedback over gRPC (JAVA-CODING-RULES §7.2): the {@code lms.v1.CohortFeedbackUpdate} message as REST's
 * {@link CohortFeedbackUpdate} body, one explicit line per field: unset keeps the value, null clears it, a value sets
 * it.
 */
final class CohortFeedbackProtoBridge {

    private CohortFeedbackProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static CohortFeedbackUpdate changes(ProtoBridge bridge, com.mysms.lms.v1.CohortFeedbackUpdate request) {
        CohortFeedbackUpdate changes = new CohortFeedbackUpdate();
        changes.setFeedback(ProtoBridge.text(request.hasFeedback(), request.getFeedback()));
        changes.setContentRating(ProtoBridge.text(request.hasContentRating(), request.getContentRating()));
        changes.setTeacherRating(ProtoBridge.text(request.hasTeacherRating(), request.getTeacherRating()));
        changes.setValueRating(ProtoBridge.text(request.hasValueRating(), request.getValueRating()));
        return bridge.patch(changes);
    }
}
