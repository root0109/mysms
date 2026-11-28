package com.mysms.learning.lms.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.learning.lms.api.model.LessonNoteUpdate;

/**
 * UpdateLessonNote over gRPC (JAVA-CODING-RULES §7.2): the {@code lms.v1.LessonNoteUpdate} message as REST's {@link
 * LessonNoteUpdate} body, one explicit line per field: unset keeps the value, null clears it, a value sets it.
 */
final class LessonNoteProtoBridge {

    private LessonNoteProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static LessonNoteUpdate changes(ProtoBridge bridge, com.mysms.lms.v1.LessonNoteUpdate request) {
        LessonNoteUpdate changes = new LessonNoteUpdate();
        changes.setUserId(bridge.uuid(request.hasUserId(), request.getUserId(), "user_id"));
        changes.setColor(bridge.choice(request.hasColor(), request.getColor(), LessonNoteUpdate.ColorEnum::fromValue));
        changes.setHighlightedText(ProtoBridge.text(request.hasHighlightedText(), request.getHighlightedText()));
        changes.setNote(ProtoBridge.text(request.hasNote(), request.getNote()));
        return bridge.patch(changes);
    }
}
