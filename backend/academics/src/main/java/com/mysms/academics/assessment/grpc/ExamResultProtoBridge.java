package com.mysms.academics.assessment.grpc;

import com.mysms.academics.assessment.api.model.ExamResultUpdate;
import com.mysms.core.grpc.ProtoBridge;

/**
 * UpdateExamResult over gRPC (JAVA-CODING-RULES §7.2): the {@code assessment.v1.ExamResultUpdate} message as REST's
 * {@link ExamResultUpdate} body, one explicit line per field: unset keeps the value, null clears it, a value sets it.
 */
final class ExamResultProtoBridge {

    private ExamResultProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static ExamResultUpdate changes(ProtoBridge bridge, com.mysms.assessment.v1.ExamResultUpdate request) {
        ExamResultUpdate changes = new ExamResultUpdate();
        changes.setRoomId(bridge.uuid(request.hasRoomId(), request.getRoomId(), "room_id"));
        changes.setNote(ProtoBridge.text(request.hasNote(), request.getNote()));
        return bridge.patch(changes);
    }
}
