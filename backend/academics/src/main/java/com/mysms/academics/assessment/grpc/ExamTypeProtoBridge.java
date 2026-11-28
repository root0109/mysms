package com.mysms.academics.assessment.grpc;

import com.mysms.academics.assessment.api.model.ExamTypeUpdate;
import com.mysms.core.grpc.ProtoBridge;

/**
 * UpdateExamType over gRPC (JAVA-CODING-RULES §7.2): the {@code assessment.v1.ExamTypeUpdate} message as REST's {@link
 * ExamTypeUpdate} body, one explicit line per field: unset keeps the value, null clears it, a value sets it.
 */
final class ExamTypeProtoBridge {

    private ExamTypeProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static ExamTypeUpdate changes(ProtoBridge bridge, com.mysms.assessment.v1.ExamTypeUpdate request) {
        ExamTypeUpdate changes = new ExamTypeUpdate();
        changes.setSortOrder(ProtoBridge.number(request.hasSortOrder(), request.getSortOrder()));
        changes.setIsActive(ProtoBridge.flag(request.hasIsActive(), request.getIsActive()));
        changes.setCode(ProtoBridge.text(request.hasCode(), request.getCode()));
        changes.setName(ProtoBridge.text(request.hasName(), request.getName()));
        return bridge.patch(changes);
    }
}
