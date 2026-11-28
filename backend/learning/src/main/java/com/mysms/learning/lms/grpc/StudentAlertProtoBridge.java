package com.mysms.learning.lms.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.learning.lms.api.model.StudentAlertUpdate;

/**
 * UpdateStudentAlert over gRPC (JAVA-CODING-RULES §7.2): the {@code lms.v1.StudentAlertUpdate} message as REST's {@link
 * StudentAlertUpdate} body, one explicit line per field: unset keeps the value, null clears it, a value sets it.
 */
final class StudentAlertProtoBridge {

    private StudentAlertProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static StudentAlertUpdate changes(ProtoBridge bridge, com.mysms.lms.v1.StudentAlertUpdate request) {
        StudentAlertUpdate changes = new StudentAlertUpdate();
        changes.setUserId(bridge.uuid(request.hasUserId(), request.getUserId(), "user_id"));
        changes.setCohortId(bridge.uuid(request.hasCohortId(), request.getCohortId(), "cohort_id"));
        changes.setCourseId(bridge.uuid(request.hasCourseId(), request.getCourseId(), "course_id"));
        changes.setMetricCode(ProtoBridge.text(request.hasMetricCode(), request.getMetricCode()));
        changes.setContentItemId(bridge.uuid(
                request.hasContentItemId(), request.getContentItemId(), "content_item_id"));
        changes.setSentAt(ProtoBridge.time(request.hasSentAt(), request.getSentAt()));
        changes.setSubject(ProtoBridge.text(request.hasSubject(), request.getSubject()));
        changes.setMessage(ProtoBridge.text(request.hasMessage(), request.getMessage()));
        changes.setDetails(ProtoBridge.text(request.hasDetails(), request.getDetails()));
        changes.setReadAt(ProtoBridge.time(request.hasReadAt(), request.getReadAt()));
        return bridge.patch(changes);
    }
}
