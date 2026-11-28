package com.mysms.academics.assessment.grpc;

import com.mysms.academics.assessment.api.model.HomeworkUpdate;
import com.mysms.core.grpc.ProtoBridge;

/**
 * UpdateHomework over gRPC (JAVA-CODING-RULES §7.2): the {@code assessment.v1.HomeworkUpdate} message as REST's {@link
 * HomeworkUpdate} body, one explicit line per field: unset keeps the value, null clears it, a value sets it.
 */
final class HomeworkProtoBridge {

    private HomeworkProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static HomeworkUpdate changes(ProtoBridge bridge, com.mysms.assessment.v1.HomeworkUpdate request) {
        HomeworkUpdate changes = new HomeworkUpdate();
        changes.setInstitutionId(bridge.uuid(request.hasInstitutionId(), request.getInstitutionId(), "institution_id"));
        changes.setAcademicYearId(bridge.uuid(
                request.hasAcademicYearId(), request.getAcademicYearId(), "academic_year_id"));
        changes.setClassSectionId(bridge.uuid(
                request.hasClassSectionId(), request.getClassSectionId(), "class_section_id"));
        changes.setSubjectId(bridge.uuid(request.hasSubjectId(), request.getSubjectId(), "subject_id"));
        changes.setAssignedByStaffId(bridge.uuid(
                request.hasAssignedByStaffId(), request.getAssignedByStaffId(), "assigned_by_staff_id"));
        changes.setDescription(ProtoBridge.text(request.hasDescription(), request.getDescription()));
        changes.setAttachmentFileId(bridge.uuid(
                request.hasAttachmentFileId(), request.getAttachmentFileId(), "attachment_file_id"));
        changes.setAssignedOn(ProtoBridge.time(request.hasAssignedOn(), request.getAssignedOn()));
        changes.setDueAt(ProtoBridge.time(request.hasDueAt(), request.getDueAt()));
        return bridge.patch(changes);
    }
}
