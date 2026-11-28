package com.mysms.learning.lms.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.learning.lms.api.model.RubricAssessmentUpdate;

/**
 * UpdateRubricAssessment over gRPC (JAVA-CODING-RULES §7.2): the {@code lms.v1.RubricAssessmentUpdate} message as
 * REST's {@link RubricAssessmentUpdate} body, one explicit line per field: unset keeps the value, null clears it, a
 * value sets it.
 */
final class RubricAssessmentProtoBridge {

    private RubricAssessmentProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static RubricAssessmentUpdate changes(ProtoBridge bridge, com.mysms.lms.v1.RubricAssessmentUpdate request) {
        RubricAssessmentUpdate changes = new RubricAssessmentUpdate();
        changes.setContentItemId(bridge.uuid(
                request.hasContentItemId(), request.getContentItemId(), "content_item_id"));
        changes.setUserId(bridge.uuid(request.hasUserId(), request.getUserId(), "user_id"));
        changes.setGroupId(bridge.uuid(request.hasGroupId(), request.getGroupId(), "group_id"));
        changes.setRubricCellId(bridge.uuid(request.hasRubricCellId(), request.getRubricCellId(), "rubric_cell_id"));
        changes.setComment(ProtoBridge.text(request.hasComment(), request.getComment()));
        return bridge.patch(changes);
    }
}
