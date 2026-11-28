package com.mysms.learning.lms.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.learning.lms.api.model.ExerciseTestCaseUpdate;

/**
 * UpdateExerciseTestCase over gRPC (JAVA-CODING-RULES §7.2): the {@code lms.v1.ExerciseTestCaseUpdate} message as
 * REST's {@link ExerciseTestCaseUpdate} body, one explicit line per field: unset keeps the value, null clears it, a
 * value sets it.
 */
final class ExerciseTestCaseProtoBridge {

    private ExerciseTestCaseProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static ExerciseTestCaseUpdate changes(ProtoBridge bridge, com.mysms.lms.v1.ExerciseTestCaseUpdate request) {
        ExerciseTestCaseUpdate changes = new ExerciseTestCaseUpdate();
        changes.setSortOrder(ProtoBridge.number(request.hasSortOrder(), request.getSortOrder()));
        changes.setInput(ProtoBridge.text(request.hasInput(), request.getInput()));
        changes.setExpectedOutput(ProtoBridge.text(request.hasExpectedOutput(), request.getExpectedOutput()));
        changes.setIsHidden(ProtoBridge.flag(request.hasIsHidden(), request.getIsHidden()));
        return bridge.patch(changes);
    }
}
