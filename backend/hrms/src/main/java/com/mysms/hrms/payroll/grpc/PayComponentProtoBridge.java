package com.mysms.hrms.payroll.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.hrms.payroll.api.model.PayComponentUpdate;

/**
 * UpdatePayComponent over gRPC (JAVA-CODING-RULES §7.2): the {@code payroll.v1.PayComponentUpdate} message as REST's
 * {@link PayComponentUpdate} body, one explicit line per field: unset keeps the value, null clears it, a value sets it.
 */
final class PayComponentProtoBridge {

    private PayComponentProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static PayComponentUpdate changes(ProtoBridge bridge, com.mysms.payroll.v1.PayComponentUpdate request) {
        PayComponentUpdate changes = new PayComponentUpdate();
        changes.setCode(ProtoBridge.text(request.hasCode(), request.getCode()));
        changes.setName(ProtoBridge.text(request.hasName(), request.getName()));
        changes.setComponentType(bridge.choice(
                request.hasComponentType(), request.getComponentType(),
                PayComponentUpdate.ComponentTypeEnum::fromValue));
        return bridge.patch(changes);
    }
}
