package com.mysms.students.careers.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.students.careers.api.model.IndustryUpdate;

/**
 * UpdateIndustry over gRPC (JAVA-CODING-RULES §7.2): the {@code careers.v1.IndustryUpdate} message as REST's {@link
 * IndustryUpdate} body, one explicit line per field: unset keeps the value, null clears it, a value sets it.
 */
final class IndustryProtoBridge {

    private IndustryProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static IndustryUpdate changes(ProtoBridge bridge, com.mysms.careers.v1.IndustryUpdate request) {
        IndustryUpdate changes = new IndustryUpdate();
        changes.setCode(ProtoBridge.text(request.hasCode(), request.getCode()));
        changes.setSortOrder(ProtoBridge.number(request.hasSortOrder(), request.getSortOrder()));
        changes.setIsActive(ProtoBridge.flag(request.hasIsActive(), request.getIsActive()));
        changes.setName(ProtoBridge.text(request.hasName(), request.getName()));
        return bridge.patch(changes);
    }
}
