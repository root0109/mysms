package com.mysms.students.careers.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.students.careers.api.model.CandidatePreferredIndustryUpdate;

/**
 * UpdateCandidatePreferredIndustry over gRPC (JAVA-CODING-RULES §7.2): the {@code
 * careers.v1.CandidatePreferredIndustryUpdate} message as REST's {@link CandidatePreferredIndustryUpdate} body, one
 * explicit line per field: unset keeps the value, null clears it, a value sets it.
 */
final class CandidatePreferredIndustryProtoBridge {

    private CandidatePreferredIndustryProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static CandidatePreferredIndustryUpdate changes(
            ProtoBridge bridge, com.mysms.careers.v1.CandidatePreferredIndustryUpdate request) {
        CandidatePreferredIndustryUpdate changes = new CandidatePreferredIndustryUpdate();
        changes.setIndustryId(bridge.uuid(request.hasIndustryId(), request.getIndustryId(), "industry_id"));
        return bridge.patch(changes);
    }
}
