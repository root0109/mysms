package com.mysms.finance.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.finance.api.model.FeePlanUpdate;

/**
 * UpdateFeePlan over gRPC (JAVA-CODING-RULES §7.2): the {@code finance.v1.FeePlanUpdate} message
 * as REST's {@link FeePlanUpdate} body, one explicit line per field: unset keeps the value, null
 * clears it, a value sets it.
 */
final class FeePlanProtoBridge {

    private FeePlanProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static FeePlanUpdate changes(ProtoBridge bridge, com.mysms.finance.v1.FeePlanUpdate request) {
        FeePlanUpdate changes = new FeePlanUpdate();
        changes.setInstitutionId(bridge.uuid(request.hasInstitutionId(), request.getInstitutionId(), "institution_id"));
        changes.setCode(ProtoBridge.text(request.hasCode(), request.getCode()));
        changes.setName(ProtoBridge.text(request.hasName(), request.getName()));
        changes.setScheduleBasis(bridge.choice(
                request.hasScheduleBasis(), request.getScheduleBasis(), FeePlanUpdate.ScheduleBasisEnum::fromValue));
        changes.setOffsetDays(ProtoBridge.number(request.hasOffsetDays(), request.getOffsetDays()));
        changes.setOffsetDirection(bridge.choice(
                request.hasOffsetDirection(), request.getOffsetDirection(), FeePlanUpdate.OffsetDirectionEnum::fromValue));
        changes.setDiscountPercent(ProtoBridge.text(request.hasDiscountPercent(), request.getDiscountPercent()));
        changes.setNotes(ProtoBridge.text(request.hasNotes(), request.getNotes()));
        changes.setIsActive(ProtoBridge.flag(request.hasIsActive(), request.getIsActive()));
        return bridge.patch(changes);
    }
}
