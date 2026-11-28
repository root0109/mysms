package com.mysms.learning.lms.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.learning.lms.api.model.ProgrammingExerciseUpdate;

/**
 * UpdateProgrammingExercise over gRPC (JAVA-CODING-RULES §7.2): the {@code lms.v1.ProgrammingExerciseUpdate} message as
 * REST's {@link ProgrammingExerciseUpdate} body, one explicit line per field: unset keeps the value, null clears it, a
 * value sets it.
 */
final class ProgrammingExerciseProtoBridge {

    private ProgrammingExerciseProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static ProgrammingExerciseUpdate changes(ProtoBridge bridge, com.mysms.lms.v1.ProgrammingExerciseUpdate request) {
        ProgrammingExerciseUpdate changes = new ProgrammingExerciseUpdate();
        changes.setTitle(ProtoBridge.text(request.hasTitle(), request.getTitle()));
        changes.setProblemStatement(ProtoBridge.text(request.hasProblemStatement(), request.getProblemStatement()));
        changes.setLanguage(bridge.choice(
                request.hasLanguage(), request.getLanguage(), ProgrammingExerciseUpdate.LanguageEnum::fromValue));
        changes.setStarterCode(ProtoBridge.text(request.hasStarterCode(), request.getStarterCode()));
        return bridge.patch(changes);
    }
}
