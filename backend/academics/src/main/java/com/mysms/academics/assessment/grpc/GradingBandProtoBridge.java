package com.mysms.academics.assessment.grpc;

import com.mysms.academics.assessment.api.model.GradingBandUpdate;
import com.mysms.core.grpc.ProtoBridge;

/**
 * UpdateGradingBand over gRPC (JAVA-CODING-RULES §7.2): the {@code assessment.v1.GradingBandUpdate} message as REST's
 * {@link GradingBandUpdate} body, one explicit line per field: unset keeps the value, null clears it, a value sets it.
 */
final class GradingBandProtoBridge {

    private GradingBandProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static GradingBandUpdate changes(ProtoBridge bridge, com.mysms.assessment.v1.GradingBandUpdate request) {
        GradingBandUpdate changes = new GradingBandUpdate();
        changes.setGradingSchemeId(bridge.uuid(
                request.hasGradingSchemeId(), request.getGradingSchemeId(), "grading_scheme_id"));
        changes.setMinPercent(ProtoBridge.text(request.hasMinPercent(), request.getMinPercent()));
        changes.setMaxPercent(ProtoBridge.text(request.hasMaxPercent(), request.getMaxPercent()));
        changes.setGradeLabel(ProtoBridge.text(request.hasGradeLabel(), request.getGradeLabel()));
        changes.setGradePoint(ProtoBridge.text(request.hasGradePoint(), request.getGradePoint()));
        changes.setIsPass(ProtoBridge.flag(request.hasIsPass(), request.getIsPass()));
        changes.setDescription(ProtoBridge.text(request.hasDescription(), request.getDescription()));
        return bridge.patch(changes);
    }
}
