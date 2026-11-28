package com.mysms.academics.grpc;

import com.mysms.academics.api.model.CourseSubjectUpdate;
import com.mysms.core.grpc.ProtoBridge;

/**
 * UpdateCourseSubject over gRPC (JAVA-CODING-RULES §7.2): the {@code academics.v1.CourseSubjectUpdate} message as
 * REST's {@link CourseSubjectUpdate} body, one explicit line per field: unset keeps the value, null clears it, a value
 * sets it.
 */
final class CourseSubjectProtoBridge {

    private CourseSubjectProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static CourseSubjectUpdate changes(ProtoBridge bridge, com.mysms.academics.v1.CourseSubjectUpdate request) {
        CourseSubjectUpdate changes = new CourseSubjectUpdate();
        changes.setTermNumber(ProtoBridge.number(request.hasTermNumber(), request.getTermNumber()));
        changes.setCredits(ProtoBridge.text(request.hasCredits(), request.getCredits()));
        changes.setSubjectCategory(ProtoBridge.text(request.hasSubjectCategory(), request.getSubjectCategory()));
        changes.setMaxMarks(ProtoBridge.text(request.hasMaxMarks(), request.getMaxMarks()));
        changes.setSubjectId(bridge.uuid(request.hasSubjectId(), request.getSubjectId(), "subject_id"));
        return bridge.patch(changes);
    }
}
