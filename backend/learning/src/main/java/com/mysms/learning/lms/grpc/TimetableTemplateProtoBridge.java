package com.mysms.learning.lms.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.learning.lms.api.model.TimetableTemplateUpdate;

/**
 * UpdateTimetableTemplate over gRPC (JAVA-CODING-RULES §7.2): the {@code lms.v1.TimetableTemplateUpdate} message as
 * REST's {@link TimetableTemplateUpdate} body, one explicit line per field: unset keeps the value, null clears it, a
 * value sets it.
 */
final class TimetableTemplateProtoBridge {

    private TimetableTemplateProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static TimetableTemplateUpdate changes(ProtoBridge bridge, com.mysms.lms.v1.TimetableTemplateUpdate request) {
        TimetableTemplateUpdate changes = new TimetableTemplateUpdate();
        changes.setTitle(ProtoBridge.text(request.hasTitle(), request.getTitle()));
        return bridge.patch(changes);
    }
}
