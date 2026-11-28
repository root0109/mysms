package com.mysms.campus.library.grpc;

import com.mysms.campus.library.api.model.PurchaseRequestSubjectUpdate;
import com.mysms.core.grpc.ProtoBridge;

/**
 * UpdatePurchaseRequestSubject over gRPC (JAVA-CODING-RULES §7.2): the {@code library.v1.PurchaseRequestSubjectUpdate}
 * message as REST's {@link PurchaseRequestSubjectUpdate} body, one explicit line per field: unset keeps the value, null
 * clears it, a value sets it.
 */
final class PurchaseRequestSubjectProtoBridge {

    private PurchaseRequestSubjectProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static PurchaseRequestSubjectUpdate changes(
            ProtoBridge bridge, com.mysms.library.v1.PurchaseRequestSubjectUpdate request) {
        PurchaseRequestSubjectUpdate changes = new PurchaseRequestSubjectUpdate();
        changes.setSubjectId(bridge.uuid(request.hasSubjectId(), request.getSubjectId(), "subject_id"));
        return bridge.patch(changes);
    }
}
