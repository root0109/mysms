package com.mysms.academics.assessment.grpc;

import com.mysms.academics.assessment.api.model.HomeworkSubmissionUpdate;
import com.mysms.core.grpc.ProtoBridge;

/**
 * UpdateHomeworkSubmission over gRPC (JAVA-CODING-RULES §7.2): the {@code assessment.v1.HomeworkSubmissionUpdate}
 * message as REST's {@link HomeworkSubmissionUpdate} body, one explicit line per field: unset keeps the value, null
 * clears it, a value sets it.
 */
final class HomeworkSubmissionProtoBridge {

    private HomeworkSubmissionProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static HomeworkSubmissionUpdate changes(
            ProtoBridge bridge, com.mysms.assessment.v1.HomeworkSubmissionUpdate request) {
        HomeworkSubmissionUpdate changes = new HomeworkSubmissionUpdate();
        changes.setHomeworkId(bridge.uuid(request.hasHomeworkId(), request.getHomeworkId(), "homework_id"));
        changes.setSubmittedOn(bridge.date(request.hasSubmittedOn(), request.getSubmittedOn(), "submitted_on"));
        return bridge.patch(changes);
    }
}
