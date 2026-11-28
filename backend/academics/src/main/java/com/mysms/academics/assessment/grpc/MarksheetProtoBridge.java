package com.mysms.academics.assessment.grpc;

import com.mysms.academics.assessment.api.model.MarksheetUpdate;
import com.mysms.core.grpc.ProtoBridge;

/**
 * UpdateMarksheet over gRPC (JAVA-CODING-RULES §7.2): the {@code assessment.v1.MarksheetUpdate} message as REST's
 * {@link MarksheetUpdate} body, one explicit line per field: unset keeps the value, null clears it, a value sets it.
 */
final class MarksheetProtoBridge {

    private MarksheetProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static MarksheetUpdate changes(ProtoBridge bridge, com.mysms.assessment.v1.MarksheetUpdate request) {
        MarksheetUpdate changes = new MarksheetUpdate();
        changes.setVisibleToGuardians(ProtoBridge.flag(
                request.hasVisibleToGuardians(), request.getVisibleToGuardians()));
        changes.setExamSessionId(bridge.uuid(
                request.hasExamSessionId(), request.getExamSessionId(), "exam_session_id"));
        changes.setResultTemplateId(bridge.uuid(
                request.hasResultTemplateId(), request.getResultTemplateId(), "result_template_id"));
        changes.setName(ProtoBridge.text(request.hasName(), request.getName()));
        changes.setGeneratedOn(bridge.date(request.hasGeneratedOn(), request.getGeneratedOn(), "generated_on"));
        return bridge.patch(changes);
    }
}
