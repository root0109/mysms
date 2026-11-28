package com.mysms.academics.assessment.grpc;

import com.mysms.academics.assessment.api.model.AssignmentUpdate;
import com.mysms.core.grpc.ProtoBridge;

/**
 * UpdateAssignment over gRPC (JAVA-CODING-RULES §7.2): the {@code assessment.v1.AssignmentUpdate} message as REST's
 * {@link AssignmentUpdate} body, one explicit line per field: unset keeps the value, null clears it, a value sets it.
 */
final class AssignmentProtoBridge {

    private AssignmentProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static AssignmentUpdate changes(ProtoBridge bridge, com.mysms.assessment.v1.AssignmentUpdate request) {
        AssignmentUpdate changes = new AssignmentUpdate();
        changes.setInstitutionId(bridge.uuid(request.hasInstitutionId(), request.getInstitutionId(), "institution_id"));
        changes.setAcademicYearId(bridge.uuid(
                request.hasAcademicYearId(), request.getAcademicYearId(), "academic_year_id"));
        changes.setTitle(ProtoBridge.text(request.hasTitle(), request.getTitle()));
        changes.setDescription(ProtoBridge.text(request.hasDescription(), request.getDescription()));
        changes.setAssignmentTypeId(bridge.uuid(
                request.hasAssignmentTypeId(), request.getAssignmentTypeId(), "assignment_type_id"));
        changes.setClassSectionId(bridge.uuid(
                request.hasClassSectionId(), request.getClassSectionId(), "class_section_id"));
        changes.setCourseId(bridge.uuid(request.hasCourseId(), request.getCourseId(), "course_id"));
        changes.setBatchId(bridge.uuid(request.hasBatchId(), request.getBatchId(), "batch_id"));
        changes.setAssignedByStaffId(bridge.uuid(
                request.hasAssignedByStaffId(), request.getAssignedByStaffId(), "assigned_by_staff_id"));
        changes.setReviewerStaffId(bridge.uuid(
                request.hasReviewerStaffId(), request.getReviewerStaffId(), "reviewer_staff_id"));
        changes.setMaxMarks(ProtoBridge.text(request.hasMaxMarks(), request.getMaxMarks()));
        changes.setIssuedOn(ProtoBridge.time(request.hasIssuedOn(), request.getIssuedOn()));
        changes.setDueAt(ProtoBridge.time(request.hasDueAt(), request.getDueAt()));
        changes.setAttachmentFileId(bridge.uuid(
                request.hasAttachmentFileId(), request.getAttachmentFileId(), "attachment_file_id"));
        return bridge.patch(changes);
    }
}
