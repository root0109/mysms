package com.mysms.finance.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.finance.api.model.FeeConcessionUpdate;

/**
 * UpdateFeeConcession over gRPC (JAVA-CODING-RULES §7.2): the {@code finance.v1.FeeConcessionUpdate} message
 * as REST's {@link FeeConcessionUpdate} body, one explicit line per field: unset keeps the value, null
 * clears it, a value sets it.
 */
final class FeeConcessionProtoBridge {

    private FeeConcessionProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static FeeConcessionUpdate changes(ProtoBridge bridge, com.mysms.finance.v1.FeeConcessionUpdate request) {
        FeeConcessionUpdate changes = new FeeConcessionUpdate();
        changes.setInstitutionId(bridge.uuid(request.hasInstitutionId(), request.getInstitutionId(), "institution_id"));
        changes.setFeeHeadId(bridge.uuid(request.hasFeeHeadId(), request.getFeeHeadId(), "fee_head_id"));
        changes.setConcessionType(bridge.choice(
                request.hasConcessionType(), request.getConcessionType(), FeeConcessionUpdate.ConcessionTypeEnum::fromValue));
        changes.setConcessionValue(ProtoBridge.text(request.hasConcessionValue(), request.getConcessionValue()));
        changes.setReason(ProtoBridge.text(request.hasReason(), request.getReason()));
        changes.setStatus(bridge.choice(
                request.hasStatus(), request.getStatus(), FeeConcessionUpdate.StatusEnum::fromValue));
        changes.setValidFrom(bridge.date(request.hasValidFrom(), request.getValidFrom(), "valid_from"));
        changes.setValidUntil(bridge.date(request.hasValidUntil(), request.getValidUntil(), "valid_until"));
        return bridge.patch(changes);
    }
}
