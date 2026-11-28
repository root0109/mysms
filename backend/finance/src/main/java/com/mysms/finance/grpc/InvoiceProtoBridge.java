package com.mysms.finance.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.finance.api.model.InvoiceUpdate;

/**
 * UpdateInvoice over gRPC (JAVA-CODING-RULES §7.2): the {@code finance.v1.InvoiceUpdate} message
 * as REST's {@link InvoiceUpdate} body, one explicit line per field: unset keeps the value, null
 * clears it, a value sets it.
 */
final class InvoiceProtoBridge {

    private InvoiceProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static InvoiceUpdate changes(ProtoBridge bridge, com.mysms.finance.v1.InvoiceUpdate request) {
        InvoiceUpdate changes = new InvoiceUpdate();
        changes.setInstitutionId(bridge.uuid(request.hasInstitutionId(), request.getInstitutionId(), "institution_id"));
        changes.setBillToName(ProtoBridge.text(request.hasBillToName(), request.getBillToName()));
        changes.setInvoiceDate(bridge.date(request.hasInvoiceDate(), request.getInvoiceDate(), "invoice_date"));
        changes.setDueDate(bridge.date(request.hasDueDate(), request.getDueDate(), "due_date"));
        return bridge.patch(changes);
    }
}
