package com.mysms.finance.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.finance.api.model.FeeStructureUpdate;

/**
 * UpdateFeeStructure over gRPC (JAVA-CODING-RULES §7.2): the {@code finance.v1.FeeStructureUpdate} message
 * as REST's {@link FeeStructureUpdate} body, one explicit line per field: unset keeps the value, null
 * clears it, a value sets it.
 */
final class FeeStructureProtoBridge {

    private FeeStructureProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static FeeStructureUpdate changes(ProtoBridge bridge, com.mysms.finance.v1.FeeStructureUpdate request) {
        FeeStructureUpdate changes = new FeeStructureUpdate();
        changes.setInstitutionId(bridge.uuid(request.hasInstitutionId(), request.getInstitutionId(), "institution_id"));
        changes.setAcademicYearId(bridge.uuid(
                request.hasAcademicYearId(), request.getAcademicYearId(), "academic_year_id"));
        changes.setGradeLevelId(bridge.uuid(request.hasGradeLevelId(), request.getGradeLevelId(), "grade_level_id"));
        changes.setProgramId(bridge.uuid(request.hasProgramId(), request.getProgramId(), "program_id"));
        changes.setFeePlanId(bridge.uuid(request.hasFeePlanId(), request.getFeePlanId(), "fee_plan_id"));
        changes.setName(ProtoBridge.text(request.hasName(), request.getName()));
        return bridge.patch(changes);
    }
}
