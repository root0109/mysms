package com.mysms.learning.lms.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.learning.lms.api.model.TimetableEntryUpdate;

/**
 * UpdateTimetableEntry over gRPC (JAVA-CODING-RULES §7.2): the {@code lms.v1.TimetableEntryUpdate} message as REST's
 * {@link TimetableEntryUpdate} body, one explicit line per field: unset keeps the value, null clears it, a value sets
 * it.
 */
final class TimetableEntryProtoBridge {

    private TimetableEntryProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static TimetableEntryUpdate changes(ProtoBridge bridge, com.mysms.lms.v1.TimetableEntryUpdate request) {
        TimetableEntryUpdate changes = new TimetableEntryUpdate();
        changes.setCohortId(bridge.uuid(request.hasCohortId(), request.getCohortId(), "cohort_id"));
        changes.setTimetableTemplateId(bridge.uuid(
                request.hasTimetableTemplateId(), request.getTimetableTemplateId(), "timetable_template_id"));
        changes.setLessonId(bridge.uuid(request.hasLessonId(), request.getLessonId(), "lesson_id"));
        changes.setQuizId(bridge.uuid(request.hasQuizId(), request.getQuizId(), "quiz_id"));
        changes.setAssignmentId(bridge.uuid(request.hasAssignmentId(), request.getAssignmentId(), "assignment_id"));
        changes.setExerciseId(bridge.uuid(request.hasExerciseId(), request.getExerciseId(), "exercise_id"));
        changes.setLiveClassId(bridge.uuid(request.hasLiveClassId(), request.getLiveClassId(), "live_class_id"));
        changes.setEntryDate(bridge.date(request.hasEntryDate(), request.getEntryDate(), "entry_date"));
        changes.setDayNumber(ProtoBridge.number(request.hasDayNumber(), request.getDayNumber()));
        changes.setStartTime(ProtoBridge.text(request.hasStartTime(), request.getStartTime()));
        changes.setEndTime(ProtoBridge.text(request.hasEndTime(), request.getEndTime()));
        changes.setDurationMinutes(ProtoBridge.duration(request.hasDurationMinutes(), request.getDurationMinutes()));
        changes.setIsMilestone(ProtoBridge.flag(request.hasIsMilestone(), request.getIsMilestone()));
        return bridge.patch(changes);
    }
}
