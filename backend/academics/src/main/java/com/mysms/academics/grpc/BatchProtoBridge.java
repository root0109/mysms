package com.mysms.academics.grpc;

import com.mysms.academics.api.model.BatchUpdate;
import com.mysms.core.grpc.ProtoBridge;

/**
 * UpdateBatch over gRPC (JAVA-CODING-RULES §7.2): the {@code academics.v1.BatchUpdate} message as REST's {@link
 * BatchUpdate} body, one explicit line per field: unset keeps the value, null clears it, a value sets it.
 */
final class BatchProtoBridge {

    private BatchProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static BatchUpdate changes(ProtoBridge bridge, com.mysms.academics.v1.BatchUpdate request) {
        BatchUpdate changes = new BatchUpdate();
        changes.setCourseId(bridge.uuid(request.hasCourseId(), request.getCourseId(), "course_id"));
        changes.setCode(ProtoBridge.text(request.hasCode(), request.getCode()));
        changes.setName(ProtoBridge.text(request.hasName(), request.getName()));
        changes.setStartDate(bridge.date(request.hasStartDate(), request.getStartDate(), "start_date"));
        changes.setEndDate(bridge.date(request.hasEndDate(), request.getEndDate(), "end_date"));
        changes.setIsActive(ProtoBridge.flag(request.hasIsActive(), request.getIsActive()));
        return bridge.patch(changes);
    }
}
