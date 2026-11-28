package com.mysms.academics.assessment.grpc;

import com.mysms.academics.assessment.api.model.GradingSchemeUpdate;
import com.mysms.core.grpc.ProtoBridge;

/**
 * UpdateGradingScheme over gRPC (JAVA-CODING-RULES §7.2): the {@code assessment.v1.GradingSchemeUpdate} message as
 * REST's {@link GradingSchemeUpdate} body, one explicit line per field: unset keeps the value, null clears it, a value
 * sets it.
 */
final class GradingSchemeProtoBridge {

    private GradingSchemeProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static GradingSchemeUpdate changes(ProtoBridge bridge, com.mysms.assessment.v1.GradingSchemeUpdate request) {
        GradingSchemeUpdate changes = new GradingSchemeUpdate();
        changes.setInstitutionId(bridge.uuid(request.hasInstitutionId(), request.getInstitutionId(), "institution_id"));
        changes.setAcademicYearId(bridge.uuid(
                request.hasAcademicYearId(), request.getAcademicYearId(), "academic_year_id"));
        changes.setName(ProtoBridge.text(request.hasName(), request.getName()));
        changes.setSchemeType(bridge.choice(
                request.hasSchemeType(), request.getSchemeType(), GradingSchemeUpdate.SchemeTypeEnum::fromValue));
        changes.setGradingMethod(bridge.choice(
                request.hasGradingMethod(), request.getGradingMethod(),
                GradingSchemeUpdate.GradingMethodEnum::fromValue));
        changes.setCriteria(bridge.json(request.hasCriteria(), request.getCriteria()));
        changes.setIsActive(ProtoBridge.flag(request.hasIsActive(), request.getIsActive()));
        changes.setIsTemplate(ProtoBridge.flag(request.hasIsTemplate(), request.getIsTemplate()));
        return bridge.patch(changes);
    }
}
