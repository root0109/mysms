package com.mysms.students.careers.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.students.careers.api.model.JobApplicationUpdate;

/**
 * UpdateJobApplication over gRPC (JAVA-CODING-RULES §7.2): the {@code careers.v1.JobApplicationUpdate} message as
 * REST's {@link JobApplicationUpdate} body, one explicit line per field: unset keeps the value, null clears it, a value
 * sets it.
 */
final class JobApplicationProtoBridge {

    private JobApplicationProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static JobApplicationUpdate changes(ProtoBridge bridge, com.mysms.careers.v1.JobApplicationUpdate request) {
        JobApplicationUpdate changes = new JobApplicationUpdate();
        changes.setResumeFileId(bridge.uuid(request.hasResumeFileId(), request.getResumeFileId(), "resume_file_id"));
        return bridge.patch(changes);
    }
}
