package com.mysms.learning.lms.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.learning.lms.api.model.QuizUpdate;

/**
 * UpdateQuiz over gRPC (JAVA-CODING-RULES §7.2): the {@code lms.v1.QuizUpdate} message as REST's {@link QuizUpdate}
 * body, one explicit line per field: unset keeps the value, null clears it, a value sets it.
 */
final class QuizProtoBridge {

    private QuizProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static QuizUpdate changes(ProtoBridge bridge, com.mysms.lms.v1.QuizUpdate request) {
        QuizUpdate changes = new QuizUpdate();
        changes.setTitle(ProtoBridge.text(request.hasTitle(), request.getTitle()));
        changes.setCourseId(bridge.uuid(request.hasCourseId(), request.getCourseId(), "course_id"));
        changes.setLessonId(bridge.uuid(request.hasLessonId(), request.getLessonId(), "lesson_id"));
        changes.setMaxAttempts(ProtoBridge.number(request.hasMaxAttempts(), request.getMaxAttempts()));
        changes.setPassingPercentage(ProtoBridge.number(
                request.hasPassingPercentage(), request.getPassingPercentage()));
        changes.setTotalMarks(ProtoBridge.text(request.hasTotalMarks(), request.getTotalMarks()));
        changes.setShowAnswers(ProtoBridge.flag(request.hasShowAnswers(), request.getShowAnswers()));
        changes.setShowSubmissionHistory(ProtoBridge.flag(
                request.hasShowSubmissionHistory(), request.getShowSubmissionHistory()));
        changes.setShuffleQuestions(ProtoBridge.flag(request.hasShuffleQuestions(), request.getShuffleQuestions()));
        changes.setLimitQuestionsTo(ProtoBridge.number(request.hasLimitQuestionsTo(), request.getLimitQuestionsTo()));
        changes.setDurationMinutes(ProtoBridge.number(request.hasDurationMinutes(), request.getDurationMinutes()));
        changes.setProctoringEnabled(ProtoBridge.flag(request.hasProctoringEnabled(), request.getProctoringEnabled()));
        changes.setMaxViolations(ProtoBridge.number(request.hasMaxViolations(), request.getMaxViolations()));
        changes.setNegativeMarkingEnabled(ProtoBridge.flag(
                request.hasNegativeMarkingEnabled(), request.getNegativeMarkingEnabled()));
        changes.setNegativeMarks(ProtoBridge.text(request.hasNegativeMarks(), request.getNegativeMarks()));
        changes.setSchedulingEnabled(ProtoBridge.flag(request.hasSchedulingEnabled(), request.getSchedulingEnabled()));
        changes.setScheduleStart(ProtoBridge.time(request.hasScheduleStart(), request.getScheduleStart()));
        changes.setScheduleEnd(ProtoBridge.time(request.hasScheduleEnd(), request.getScheduleEnd()));
        changes.setInstitutionId(bridge.uuid(request.hasInstitutionId(), request.getInstitutionId(), "institution_id"));
        changes.setDescription(ProtoBridge.text(request.hasDescription(), request.getDescription()));
        changes.setIsAutoGraded(ProtoBridge.flag(request.hasIsAutoGraded(), request.getIsAutoGraded()));
        changes.setIsAdaptive(ProtoBridge.flag(request.hasIsAdaptive(), request.getIsAdaptive()));
        changes.setEqualQuestionWeighting(ProtoBridge.flag(
                request.hasEqualQuestionWeighting(), request.getEqualQuestionWeighting()));
        changes.setMarksPerQuestion(ProtoBridge.text(request.hasMarksPerQuestion(), request.getMarksPerQuestion()));
        changes.setAllowPause(ProtoBridge.flag(request.hasAllowPause(), request.getAllowPause()));
        changes.setAttemptScoring(bridge.choice(
                request.hasAttemptScoring(), request.getAttemptScoring(), QuizUpdate.AttemptScoringEnum::fromValue));
        changes.setDropLowestAttempt(ProtoBridge.flag(request.hasDropLowestAttempt(), request.getDropLowestAttempt()));
        changes.setDrawsFromPool(ProtoBridge.flag(request.hasDrawsFromPool(), request.getDrawsFromPool()));
        changes.setShowFeedback(ProtoBridge.flag(request.hasShowFeedback(), request.getShowFeedback()));
        changes.setIsInRepository(ProtoBridge.flag(request.hasIsInRepository(), request.getIsInRepository()));
        return bridge.patch(changes);
    }
}
