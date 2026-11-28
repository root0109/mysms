package com.mysms.learning.lms.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.learning.lms.api.model.ExternalToolOptionUpdate;

/**
 * UpdateExternalToolOption over gRPC (JAVA-CODING-RULES §7.2): the {@code lms.v1.ExternalToolOptionUpdate} message as
 * REST's {@link ExternalToolOptionUpdate} body, one explicit line per field: unset keeps the value, null clears it, a
 * value sets it.
 */
final class ExternalToolOptionProtoBridge {

    private ExternalToolOptionProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static ExternalToolOptionUpdate changes(ProtoBridge bridge, com.mysms.lms.v1.ExternalToolOptionUpdate request) {
        ExternalToolOptionUpdate changes = new ExternalToolOptionUpdate();
        changes.setOptionKind(bridge.choice(
                request.hasOptionKind(), request.getOptionKind(), ExternalToolOptionUpdate.OptionKindEnum::fromValue));
        changes.setOptionValue(ProtoBridge.text(request.hasOptionValue(), request.getOptionValue()));
        return bridge.patch(changes);
    }
}
