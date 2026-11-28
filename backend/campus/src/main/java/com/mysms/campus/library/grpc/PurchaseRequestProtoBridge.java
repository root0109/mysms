package com.mysms.campus.library.grpc;

import com.mysms.campus.library.api.model.PurchaseRequestUpdate;
import com.mysms.core.grpc.ProtoBridge;

/**
 * UpdatePurchaseRequest over gRPC (JAVA-CODING-RULES §7.2): the {@code library.v1.PurchaseRequestUpdate} message as
 * REST's {@link PurchaseRequestUpdate} body, one explicit line per field: unset keeps the value, null clears it, a
 * value sets it.
 */
final class PurchaseRequestProtoBridge {

    private PurchaseRequestProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static PurchaseRequestUpdate changes(ProtoBridge bridge, com.mysms.library.v1.PurchaseRequestUpdate request) {
        PurchaseRequestUpdate changes = new PurchaseRequestUpdate();
        changes.setRequestNumber(ProtoBridge.text(request.hasRequestNumber(), request.getRequestNumber()));
        changes.setTitle(ProtoBridge.text(request.hasTitle(), request.getTitle()));
        changes.setAuthorText(ProtoBridge.text(request.hasAuthorText(), request.getAuthorText()));
        changes.setEdition(ProtoBridge.text(request.hasEdition(), request.getEdition()));
        changes.setPublisherText(ProtoBridge.text(request.hasPublisherText(), request.getPublisherText()));
        changes.setMediaTypeId(bridge.uuid(request.hasMediaTypeId(), request.getMediaTypeId(), "media_type_id"));
        changes.setRequestedByPersonId(bridge.uuid(
                request.hasRequestedByPersonId(), request.getRequestedByPersonId(), "requested_by_person_id"));
        changes.setRequestDate(bridge.date(request.hasRequestDate(), request.getRequestDate(), "request_date"));
        return bridge.patch(changes);
    }
}
