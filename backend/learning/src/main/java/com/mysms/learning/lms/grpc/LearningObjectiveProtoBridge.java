package com.mysms.learning.lms.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.learning.lms.api.model.LearningObjectiveUpdate;

/**
 * UpdateLearningObjective over gRPC (JAVA-CODING-RULES §7.2): the {@code lms.v1.LearningObjectiveUpdate} message as
 * REST's {@link LearningObjectiveUpdate} body, one explicit line per field: unset keeps the value, null clears it, a
 * value sets it.
 */
final class LearningObjectiveProtoBridge {

    private LearningObjectiveProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static LearningObjectiveUpdate changes(ProtoBridge bridge, com.mysms.lms.v1.LearningObjectiveUpdate request) {
        LearningObjectiveUpdate changes = new LearningObjectiveUpdate();
        changes.setCourseId(bridge.uuid(request.hasCourseId(), request.getCourseId(), "course_id"));
        changes.setChapterId(bridge.uuid(request.hasChapterId(), request.getChapterId(), "chapter_id"));
        changes.setObjective(ProtoBridge.text(request.hasObjective(), request.getObjective()));
        changes.setSortOrder(ProtoBridge.number(request.hasSortOrder(), request.getSortOrder()));
        changes.setCopiedFromId(bridge.uuid(request.hasCopiedFromId(), request.getCopiedFromId(), "copied_from_id"));
        return bridge.patch(changes);
    }
}
