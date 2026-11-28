package com.mysms.students.careers.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.students.careers.api.model.CandidatePreferredFunctionUpdate;

/**
 * UpdateCandidatePreferredFunction over gRPC (JAVA-CODING-RULES §7.2): the {@code
 * careers.v1.CandidatePreferredFunctionUpdate} message as REST's {@link CandidatePreferredFunctionUpdate} body, one
 * explicit line per field: unset keeps the value, null clears it, a value sets it.
 */
final class CandidatePreferredFunctionProtoBridge {

    private CandidatePreferredFunctionProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static CandidatePreferredFunctionUpdate changes(
            ProtoBridge bridge, com.mysms.careers.v1.CandidatePreferredFunctionUpdate request) {
        CandidatePreferredFunctionUpdate changes = new CandidatePreferredFunctionUpdate();
        changes.setFunctionAreaId(bridge.uuid(
                request.hasFunctionAreaId(), request.getFunctionAreaId(), "function_area_id"));
        return bridge.patch(changes);
    }
}
