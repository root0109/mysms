package com.mysms.students.careers.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.students.careers.api.model.FunctionAreaUpdate;

/**
 * UpdateFunctionArea over gRPC (JAVA-CODING-RULES §7.2): the {@code careers.v1.FunctionAreaUpdate} message as REST's
 * {@link FunctionAreaUpdate} body, one explicit line per field: unset keeps the value, null clears it, a value sets it.
 */
final class FunctionAreaProtoBridge {

    private FunctionAreaProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static FunctionAreaUpdate changes(ProtoBridge bridge, com.mysms.careers.v1.FunctionAreaUpdate request) {
        FunctionAreaUpdate changes = new FunctionAreaUpdate();
        changes.setCode(ProtoBridge.text(request.hasCode(), request.getCode()));
        changes.setSortOrder(ProtoBridge.number(request.hasSortOrder(), request.getSortOrder()));
        changes.setIsActive(ProtoBridge.flag(request.hasIsActive(), request.getIsActive()));
        changes.setName(ProtoBridge.text(request.hasName(), request.getName()));
        return bridge.patch(changes);
    }
}
