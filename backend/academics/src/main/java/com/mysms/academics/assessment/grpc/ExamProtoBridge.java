package com.mysms.academics.assessment.grpc;

import com.mysms.academics.assessment.api.model.ExamUpdate;
import com.mysms.core.grpc.ProtoBridge;

/**
 * UpdateExam over gRPC (JAVA-CODING-RULES §7.2): the {@code assessment.v1.ExamUpdate} message as REST's {@link
 * ExamUpdate} body, one explicit line per field: unset keeps the value, null clears it, a value sets it.
 */
final class ExamProtoBridge {

    private ExamProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static ExamUpdate changes(ProtoBridge bridge, com.mysms.assessment.v1.ExamUpdate request) {
        ExamUpdate changes = new ExamUpdate();
        changes.setExamSessionId(bridge.uuid(
                request.hasExamSessionId(), request.getExamSessionId(), "exam_session_id"));
        changes.setSubjectId(bridge.uuid(request.hasSubjectId(), request.getSubjectId(), "subject_id"));
        changes.setClassSectionId(bridge.uuid(
                request.hasClassSectionId(), request.getClassSectionId(), "class_section_id"));
        changes.setCode(ProtoBridge.text(request.hasCode(), request.getCode()));
        changes.setName(ProtoBridge.text(request.hasName(), request.getName()));
        changes.setStartAt(ProtoBridge.time(request.hasStartAt(), request.getStartAt()));
        changes.setEndAt(ProtoBridge.time(request.hasEndAt(), request.getEndAt()));
        changes.setTotalMarks(ProtoBridge.text(request.hasTotalMarks(), request.getTotalMarks()));
        changes.setPassMarks(ProtoBridge.text(request.hasPassMarks(), request.getPassMarks()));
        changes.setNotes(ProtoBridge.text(request.hasNotes(), request.getNotes()));
        return bridge.patch(changes);
    }
}
