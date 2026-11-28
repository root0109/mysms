package com.mysms.learning.lms.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.learning.lms.api.model.ContentItemUpdate;

/**
 * UpdateContentItem over gRPC (JAVA-CODING-RULES §7.2): the {@code lms.v1.ContentItemUpdate} message as REST's {@link
 * ContentItemUpdate} body, one explicit line per field: unset keeps the value, null clears it, a value sets it.
 */
final class ContentItemProtoBridge {

    private ContentItemProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static ContentItemUpdate changes(ProtoBridge bridge, com.mysms.lms.v1.ContentItemUpdate request) {
        ContentItemUpdate changes = new ContentItemUpdate();
        changes.setExerciseId(bridge.uuid(request.hasExerciseId(), request.getExerciseId(), "exercise_id"));
        changes.setChapterId(bridge.uuid(request.hasChapterId(), request.getChapterId(), "chapter_id"));
        changes.setCourseId(bridge.uuid(request.hasCourseId(), request.getCourseId(), "course_id"));
        changes.setCohortId(bridge.uuid(request.hasCohortId(), request.getCohortId(), "cohort_id"));
        changes.setItemKind(bridge.choice(
                request.hasItemKind(), request.getItemKind(), ContentItemUpdate.ItemKindEnum::fromValue));
        changes.setLessonId(bridge.uuid(request.hasLessonId(), request.getLessonId(), "lesson_id"));
        changes.setFileId(bridge.uuid(request.hasFileId(), request.getFileId(), "file_id"));
        changes.setAssignmentId(bridge.uuid(request.hasAssignmentId(), request.getAssignmentId(), "assignment_id"));
        changes.setQuizId(bridge.uuid(request.hasQuizId(), request.getQuizId(), "quiz_id"));
        changes.setDiscussionPromptId(bridge.uuid(
                request.hasDiscussionPromptId(), request.getDiscussionPromptId(), "discussion_prompt_id"));
        changes.setParticipationActivityId(bridge.uuid(
                request.hasParticipationActivityId(), request.getParticipationActivityId(),
                "participation_activity_id"));
        changes.setWebResourceId(bridge.uuid(
                request.hasWebResourceId(), request.getWebResourceId(), "web_resource_id"));
        changes.setEbookId(bridge.uuid(request.hasEbookId(), request.getEbookId(), "ebook_id"));
        changes.setExternalToolResourceId(bridge.uuid(
                request.hasExternalToolResourceId(), request.getExternalToolResourceId(), "external_tool_resource_id"));
        changes.setLiveClassId(bridge.uuid(request.hasLiveClassId(), request.getLiveClassId(), "live_class_id"));
        changes.setLinkUrl(ProtoBridge.text(request.hasLinkUrl(), request.getLinkUrl()));
        changes.setTitle(ProtoBridge.text(request.hasTitle(), request.getTitle()));
        changes.setSortOrder(ProtoBridge.number(request.hasSortOrder(), request.getSortOrder()));
        changes.setIsActive(ProtoBridge.flag(request.hasIsActive(), request.getIsActive()));
        changes.setIsAlwaysVisible(ProtoBridge.flag(request.hasIsAlwaysVisible(), request.getIsAlwaysVisible()));
        changes.setVisibleFrom(ProtoBridge.time(request.hasVisibleFrom(), request.getVisibleFrom()));
        changes.setVisibleUntil(ProtoBridge.time(request.hasVisibleUntil(), request.getVisibleUntil()));
        changes.setVisibleUntilCohortEnd(ProtoBridge.flag(
                request.hasVisibleUntilCohortEnd(), request.getVisibleUntilCohortEnd()));
        changes.setAttemptFrom(ProtoBridge.time(request.hasAttemptFrom(), request.getAttemptFrom()));
        changes.setDueAt(ProtoBridge.time(request.hasDueAt(), request.getDueAt()));
        changes.setAttemptUntilCohortEnd(ProtoBridge.flag(
                request.hasAttemptUntilCohortEnd(), request.getAttemptUntilCohortEnd()));
        changes.setVisibleStartDay(ProtoBridge.number(request.hasVisibleStartDay(), request.getVisibleStartDay()));
        changes.setVisibleStartTime(ProtoBridge.text(request.hasVisibleStartTime(), request.getVisibleStartTime()));
        changes.setVisibleEndDay(ProtoBridge.number(request.hasVisibleEndDay(), request.getVisibleEndDay()));
        changes.setVisibleEndTime(ProtoBridge.text(request.hasVisibleEndTime(), request.getVisibleEndTime()));
        changes.setAttemptStartDay(ProtoBridge.number(request.hasAttemptStartDay(), request.getAttemptStartDay()));
        changes.setAttemptStartTime(ProtoBridge.text(request.hasAttemptStartTime(), request.getAttemptStartTime()));
        changes.setAttemptEndDay(ProtoBridge.number(request.hasAttemptEndDay(), request.getAttemptEndDay()));
        changes.setAttemptEndTime(ProtoBridge.text(request.hasAttemptEndTime(), request.getAttemptEndTime()));
        changes.setLateSubmissionAllowed(ProtoBridge.flag(
                request.hasLateSubmissionAllowed(), request.getLateSubmissionAllowed()));
        changes.setLateSubmissionDays(ProtoBridge.number(
                request.hasLateSubmissionDays(), request.getLateSubmissionDays()));
        changes.setIsGraded(ProtoBridge.flag(request.hasIsGraded(), request.getIsGraded()));
        changes.setPoints(ProtoBridge.text(request.hasPoints(), request.getPoints()));
        changes.setWeightPercent(ProtoBridge.text(request.hasWeightPercent(), request.getWeightPercent()));
        changes.setGradingCategoryId(bridge.uuid(
                request.hasGradingCategoryId(), request.getGradingCategoryId(), "grading_category_id"));
        changes.setRubricId(bridge.uuid(request.hasRubricId(), request.getRubricId(), "rubric_id"));
        changes.setPlagiarismCheckRequired(ProtoBridge.flag(
                request.hasPlagiarismCheckRequired(), request.getPlagiarismCheckRequired()));
        changes.setShowInCalendar(ProtoBridge.flag(request.hasShowInCalendar(), request.getShowInCalendar()));
        changes.setAllowDelete(ProtoBridge.flag(request.hasAllowDelete(), request.getAllowDelete()));
        changes.setAllowEditDetails(ProtoBridge.flag(request.hasAllowEditDetails(), request.getAllowEditDetails()));
        changes.setAllowEditDates(ProtoBridge.flag(request.hasAllowEditDates(), request.getAllowEditDates()));
        changes.setAllowEditGrading(ProtoBridge.flag(request.hasAllowEditGrading(), request.getAllowEditGrading()));
        return bridge.patch(changes);
    }
}
