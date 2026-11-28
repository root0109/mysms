package com.mysms.academics.assessment.grpc;

import com.mysms.academics.assessment.api.model.ResultTemplateUpdate;
import com.mysms.core.grpc.ProtoBridge;

/**
 * UpdateResultTemplate over gRPC (JAVA-CODING-RULES §7.2): the {@code assessment.v1.ResultTemplateUpdate} message as
 * REST's {@link ResultTemplateUpdate} body, one explicit line per field: unset keeps the value, null clears it, a value
 * sets it.
 */
final class ResultTemplateProtoBridge {

    private ResultTemplateProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static ResultTemplateUpdate changes(ProtoBridge bridge, com.mysms.assessment.v1.ResultTemplateUpdate request) {
        ResultTemplateUpdate changes = new ResultTemplateUpdate();
        changes.setExamSessionId(bridge.uuid(
                request.hasExamSessionId(), request.getExamSessionId(), "exam_session_id"));
        changes.setGradingSchemeId(bridge.uuid(
                request.hasGradingSchemeId(), request.getGradingSchemeId(), "grading_scheme_id"));
        changes.setName(ProtoBridge.text(request.hasName(), request.getName()));
        changes.setResultDate(bridge.date(request.hasResultDate(), request.getResultDate(), "result_date"));
        return bridge.patch(changes);
    }
}
