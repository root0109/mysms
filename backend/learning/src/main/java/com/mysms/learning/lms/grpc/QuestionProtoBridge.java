package com.mysms.learning.lms.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.learning.lms.api.model.QuestionUpdate;

/**
 * UpdateQuestion over gRPC (JAVA-CODING-RULES §7.2): the {@code lms.v1.QuestionUpdate} message as REST's {@link
 * QuestionUpdate} body, one explicit line per field: unset keeps the value, null clears it, a value sets it.
 */
final class QuestionProtoBridge {

    private QuestionProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static QuestionUpdate changes(ProtoBridge bridge, com.mysms.lms.v1.QuestionUpdate request) {
        QuestionUpdate changes = new QuestionUpdate();
        changes.setQuestionCode(ProtoBridge.text(request.hasQuestionCode(), request.getQuestionCode()));
        changes.setQuestionText(ProtoBridge.text(request.hasQuestionText(), request.getQuestionText()));
        changes.setQuestionType(bridge.choice(
                request.hasQuestionType(), request.getQuestionType(), QuestionUpdate.QuestionTypeEnum::fromValue));
        changes.setAllowsMultiple(ProtoBridge.flag(request.hasAllowsMultiple(), request.getAllowsMultiple()));
        changes.setMarks(ProtoBridge.text(request.hasMarks(), request.getMarks()));
        changes.setTitle(ProtoBridge.text(request.hasTitle(), request.getTitle()));
        changes.setShuffleOptions(ProtoBridge.flag(request.hasShuffleOptions(), request.getShuffleOptions()));
        changes.setOptionLabelStyle(ProtoBridge.text(request.hasOptionLabelStyle(), request.getOptionLabelStyle()));
        changes.setMaxSelections(ProtoBridge.number(request.hasMaxSelections(), request.getMaxSelections()));
        changes.setDifficulty(ProtoBridge.text(request.hasDifficulty(), request.getDifficulty()));
        changes.setGeneralFeedback(ProtoBridge.text(request.hasGeneralFeedback(), request.getGeneralFeedback()));
        changes.setCorrectFeedback(ProtoBridge.text(request.hasCorrectFeedback(), request.getCorrectFeedback()));
        changes.setIncorrectFeedback(ProtoBridge.text(request.hasIncorrectFeedback(), request.getIncorrectFeedback()));
        changes.setModelAnswer(ProtoBridge.text(request.hasModelAnswer(), request.getModelAnswer()));
        changes.setTrueFalseAnswer(ProtoBridge.flag(request.hasTrueFalseAnswer(), request.getTrueFalseAnswer()));
        changes.setNumericAnswer(ProtoBridge.text(request.hasNumericAnswer(), request.getNumericAnswer()));
        changes.setNumericTolerance(ProtoBridge.text(request.hasNumericTolerance(), request.getNumericTolerance()));
        changes.setNumericIsRange(ProtoBridge.flag(request.hasNumericIsRange(), request.getNumericIsRange()));
        changes.setCaseSensitive(ProtoBridge.flag(request.hasCaseSensitive(), request.getCaseSensitive()));
        changes.setAllowSuperscript(ProtoBridge.flag(request.hasAllowSuperscript(), request.getAllowSuperscript()));
        changes.setAllowSubscript(ProtoBridge.flag(request.hasAllowSubscript(), request.getAllowSubscript()));
        changes.setIsRetired(ProtoBridge.flag(request.hasIsRetired(), request.getIsRetired()));
        return bridge.patch(changes);
    }
}
