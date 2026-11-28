package com.mysms.learning.lms.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.learning.lms.api.model.GradingCategoryUpdate;

/**
 * UpdateGradingCategory over gRPC (JAVA-CODING-RULES §7.2): the {@code lms.v1.GradingCategoryUpdate} message as REST's
 * {@link GradingCategoryUpdate} body, one explicit line per field: unset keeps the value, null clears it, a value sets
 * it.
 */
final class GradingCategoryProtoBridge {

    private GradingCategoryProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static GradingCategoryUpdate changes(ProtoBridge bridge, com.mysms.lms.v1.GradingCategoryUpdate request) {
        GradingCategoryUpdate changes = new GradingCategoryUpdate();
        changes.setCourseId(bridge.uuid(request.hasCourseId(), request.getCourseId(), "course_id"));
        changes.setCohortId(bridge.uuid(request.hasCohortId(), request.getCohortId(), "cohort_id"));
        changes.setName(ProtoBridge.text(request.hasName(), request.getName()));
        changes.setWeightPercent(ProtoBridge.text(request.hasWeightPercent(), request.getWeightPercent()));
        changes.setEqualItemWeighting(ProtoBridge.flag(
                request.hasEqualItemWeighting(), request.getEqualItemWeighting()));
        return bridge.patch(changes);
    }
}
