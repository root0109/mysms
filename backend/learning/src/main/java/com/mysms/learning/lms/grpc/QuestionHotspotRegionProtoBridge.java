package com.mysms.learning.lms.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.learning.lms.api.model.QuestionHotspotRegionUpdate;

/**
 * UpdateQuestionHotspotRegion over gRPC (JAVA-CODING-RULES §7.2): the {@code lms.v1.QuestionHotspotRegionUpdate}
 * message as REST's {@link QuestionHotspotRegionUpdate} body, one explicit line per field: unset keeps the value, null
 * clears it, a value sets it.
 */
final class QuestionHotspotRegionProtoBridge {

    private QuestionHotspotRegionProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static QuestionHotspotRegionUpdate changes(
            ProtoBridge bridge, com.mysms.lms.v1.QuestionHotspotRegionUpdate request) {
        QuestionHotspotRegionUpdate changes = new QuestionHotspotRegionUpdate();
        changes.setQuestionHotspotId(bridge.uuid(
                request.hasQuestionHotspotId(), request.getQuestionHotspotId(), "question_hotspot_id"));
        changes.setLabel(ProtoBridge.text(request.hasLabel(), request.getLabel()));
        changes.setShape(ProtoBridge.text(request.hasShape(), request.getShape()));
        changes.setCoordinates(ProtoBridge.text(request.hasCoordinates(), request.getCoordinates()));
        changes.setIsCorrect(ProtoBridge.flag(request.hasIsCorrect(), request.getIsCorrect()));
        changes.setFeedback(ProtoBridge.text(request.hasFeedback(), request.getFeedback()));
        return bridge.patch(changes);
    }
}
