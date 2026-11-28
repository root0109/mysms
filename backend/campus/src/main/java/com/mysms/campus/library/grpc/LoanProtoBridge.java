package com.mysms.campus.library.grpc;

import com.mysms.campus.library.api.model.LoanUpdate;
import com.mysms.core.grpc.ProtoBridge;

/**
 * UpdateLoan over gRPC (JAVA-CODING-RULES §7.2): the {@code library.v1.LoanUpdate} message as REST's {@link LoanUpdate}
 * body, one explicit line per field: unset keeps the value, null clears it, a value sets it.
 */
final class LoanProtoBridge {

    private LoanProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static LoanUpdate changes(ProtoBridge bridge, com.mysms.library.v1.LoanUpdate request) {
        LoanUpdate changes = new LoanUpdate();
        changes.setMediaUnitId(bridge.uuid(request.hasMediaUnitId(), request.getMediaUnitId(), "media_unit_id"));
        changes.setMediaId(bridge.uuid(request.hasMediaId(), request.getMediaId(), "media_id"));
        changes.setLibraryCardId(bridge.uuid(
                request.hasLibraryCardId(), request.getLibraryCardId(), "library_card_id"));
        changes.setBorrowerUserId(bridge.uuid(
                request.hasBorrowerUserId(), request.getBorrowerUserId(), "borrower_user_id"));
        changes.setIssuedOn(bridge.date(request.hasIssuedOn(), request.getIssuedOn(), "issued_on"));
        changes.setDueOn(bridge.date(request.hasDueOn(), request.getDueOn(), "due_on"));
        changes.setReturnedOn(bridge.date(request.hasReturnedOn(), request.getReturnedOn(), "returned_on"));
        changes.setFineAmount(ProtoBridge.text(request.hasFineAmount(), request.getFineAmount()));
        changes.setInvoiceId(bridge.uuid(request.hasInvoiceId(), request.getInvoiceId(), "invoice_id"));
        changes.setIssuedByUserId(bridge.uuid(
                request.hasIssuedByUserId(), request.getIssuedByUserId(), "issued_by_user_id"));
        return bridge.patch(changes);
    }
}
