package com.mysms.academics.assessment.grpc;

import com.mysms.academics.assessment.api.model.ResultTemplateBandUpdate;
import com.mysms.core.grpc.ProtoBridge;

/**
 * UpdateResultTemplateBand over gRPC (JAVA-CODING-RULES §7.2): the {@code assessment.v1.ResultTemplateBandUpdate}
 * message as REST's {@link ResultTemplateBandUpdate} body, one explicit line per field: unset keeps the value, null
 * clears it, a value sets it.
 */
final class ResultTemplateBandProtoBridge {

    private ResultTemplateBandProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static ResultTemplateBandUpdate changes(
            ProtoBridge bridge, com.mysms.assessment.v1.ResultTemplateBandUpdate request) {
        ResultTemplateBandUpdate changes = new ResultTemplateBandUpdate();
        changes.setGradingBandId(bridge.uuid(
                request.hasGradingBandId(), request.getGradingBandId(), "grading_band_id"));
        return bridge.patch(changes);
    }
}
