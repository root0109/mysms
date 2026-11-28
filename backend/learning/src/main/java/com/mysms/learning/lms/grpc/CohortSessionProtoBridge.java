package com.mysms.learning.lms.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.learning.lms.api.model.CohortSessionUpdate;

/**
 * UpdateCohortSession over gRPC (JAVA-CODING-RULES §7.2): the {@code lms.v1.CohortSessionUpdate} message as REST's
 * {@link CohortSessionUpdate} body, one explicit line per field: unset keeps the value, null clears it, a value sets
 * it.
 */
final class CohortSessionProtoBridge {

    private CohortSessionProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static CohortSessionUpdate changes(ProtoBridge bridge, com.mysms.lms.v1.CohortSessionUpdate request) {
        CohortSessionUpdate changes = new CohortSessionUpdate();
        changes.setStartsAt(ProtoBridge.time(request.hasStartsAt(), request.getStartsAt()));
        changes.setEndsAt(ProtoBridge.time(request.hasEndsAt(), request.getEndsAt()));
        changes.setDescription(ProtoBridge.text(request.hasDescription(), request.getDescription()));
        changes.setVenue(ProtoBridge.text(request.hasVenue(), request.getVenue()));
        changes.setIsCancelled(ProtoBridge.flag(request.hasIsCancelled(), request.getIsCancelled()));
        return bridge.patch(changes);
    }
}
