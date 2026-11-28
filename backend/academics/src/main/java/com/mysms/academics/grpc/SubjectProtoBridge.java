package com.mysms.academics.grpc;

import com.mysms.academics.api.model.SubjectUpdate;
import com.mysms.core.grpc.ProtoBridge;

/**
 * UpdateSubject over gRPC (JAVA-CODING-RULES §7.2): the {@code academics.v1.SubjectUpdate} message as REST's {@link
 * SubjectUpdate} body, one explicit line per field: unset keeps the value, null clears it, a value sets it.
 */
final class SubjectProtoBridge {

    private SubjectProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static SubjectUpdate changes(ProtoBridge bridge, com.mysms.academics.v1.SubjectUpdate request) {
        SubjectUpdate changes = new SubjectUpdate();
        changes.setInstitutionId(bridge.uuid(request.hasInstitutionId(), request.getInstitutionId(), "institution_id"));
        changes.setDepartmentId(bridge.uuid(request.hasDepartmentId(), request.getDepartmentId(), "department_id"));
        changes.setCode(ProtoBridge.text(request.hasCode(), request.getCode()));
        changes.setName(ProtoBridge.text(request.hasName(), request.getName()));
        changes.setDeliveryType(bridge.choice(
                request.hasDeliveryType(), request.getDeliveryType(), SubjectUpdate.DeliveryTypeEnum::fromValue));
        changes.setDefaultCategory(bridge.choice(
                request.hasDefaultCategory(), request.getDefaultCategory(),
                SubjectUpdate.DefaultCategoryEnum::fromValue));
        changes.setGradeWeightage(ProtoBridge.text(request.hasGradeWeightage(), request.getGradeWeightage()));
        changes.setIsActive(ProtoBridge.flag(request.hasIsActive(), request.getIsActive()));
        return bridge.patch(changes);
    }
}
