package com.mysms.learning.lms.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.learning.lms.api.model.QuestionHotspotUpdate;

/**
 * UpdateQuestionHotspot over gRPC (JAVA-CODING-RULES §7.2): the {@code lms.v1.QuestionHotspotUpdate} message as REST's
 * {@link QuestionHotspotUpdate} body, one explicit line per field: unset keeps the value, null clears it, a value sets
 * it.
 */
final class QuestionHotspotProtoBridge {

    private QuestionHotspotProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static QuestionHotspotUpdate changes(ProtoBridge bridge, com.mysms.lms.v1.QuestionHotspotUpdate request) {
        QuestionHotspotUpdate changes = new QuestionHotspotUpdate();
        changes.setHotspotType(ProtoBridge.text(request.hasHotspotType(), request.getHotspotType()));
        changes.setImageFileId(bridge.uuid(request.hasImageFileId(), request.getImageFileId(), "image_file_id"));
        return bridge.patch(changes);
    }
}
