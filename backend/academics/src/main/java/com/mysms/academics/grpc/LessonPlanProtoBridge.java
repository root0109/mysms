package com.mysms.academics.grpc;

import com.mysms.academics.api.model.LessonPlanUpdate;
import com.mysms.core.grpc.ProtoBridge;

/**
 * UpdateLessonPlan over gRPC (JAVA-CODING-RULES §7.2): the {@code academics.v1.LessonPlanUpdate} message as REST's
 * {@link LessonPlanUpdate} body, one explicit line per field: unset keeps the value, null clears it, a value sets it.
 */
final class LessonPlanProtoBridge {

    private LessonPlanProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static LessonPlanUpdate changes(ProtoBridge bridge, com.mysms.academics.v1.LessonPlanUpdate request) {
        LessonPlanUpdate changes = new LessonPlanUpdate();
        changes.setTeachingAssignmentId(bridge.uuid(
                request.hasTeachingAssignmentId(), request.getTeachingAssignmentId(), "teaching_assignment_id"));
        changes.setCurriculumUnitId(bridge.uuid(
                request.hasCurriculumUnitId(), request.getCurriculumUnitId(), "curriculum_unit_id"));
        changes.setUnitNo(ProtoBridge.text(request.hasUnitNo(), request.getUnitNo()));
        changes.setUnitName(ProtoBridge.text(request.hasUnitName(), request.getUnitName()));
        changes.setTitle(ProtoBridge.text(request.hasTitle(), request.getTitle()));
        changes.setPlannedDurationMinutes(ProtoBridge.duration(
                request.hasPlannedDurationMinutes(), request.getPlannedDurationMinutes()));
        changes.setDescription(ProtoBridge.text(request.hasDescription(), request.getDescription()));
        changes.setObjective(ProtoBridge.text(request.hasObjective(), request.getObjective()));
        changes.setMaterialsRequired(ProtoBridge.text(request.hasMaterialsRequired(), request.getMaterialsRequired()));
        changes.setIntroduction(ProtoBridge.text(request.hasIntroduction(), request.getIntroduction()));
        changes.setProcedureText(ProtoBridge.text(request.hasProcedureText(), request.getProcedureText()));
        changes.setConclusion(ProtoBridge.text(request.hasConclusion(), request.getConclusion()));
        changes.setAssessmentText(ProtoBridge.text(request.hasAssessmentText(), request.getAssessmentText()));
        changes.setModification(ProtoBridge.text(request.hasModification(), request.getModification()));
        changes.setNotes(ProtoBridge.text(request.hasNotes(), request.getNotes()));
        changes.setStartDate(bridge.date(request.hasStartDate(), request.getStartDate(), "start_date"));
        changes.setEndDate(bridge.date(request.hasEndDate(), request.getEndDate(), "end_date"));
        return bridge.patch(changes);
    }
}
