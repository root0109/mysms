package com.mysms.platform.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.platform.api.model.SalesEnquiryUpdate;

/**
 * UpdateSalesEnquiry over gRPC (JAVA-CODING-RULES §7.2): the {@code platform.v1.SalesEnquiryUpdate} message
 * as REST's {@link SalesEnquiryUpdate} body, one explicit line per field: unset keeps the value, null
 * clears it, a value sets it.
 */
final class SalesEnquiryProtoBridge {

    private SalesEnquiryProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static SalesEnquiryUpdate changes(ProtoBridge bridge, com.mysms.platform.v1.SalesEnquiryUpdate request) {
        SalesEnquiryUpdate changes = new SalesEnquiryUpdate();
        changes.setContactName(ProtoBridge.text(request.hasContactName(), request.getContactName()));
        changes.setEmail(ProtoBridge.text(request.hasEmail(), request.getEmail()));
        changes.setPhone(ProtoBridge.text(request.hasPhone(), request.getPhone()));
        changes.setOrganisationName(ProtoBridge.text(request.hasOrganisationName(), request.getOrganisationName()));
        changes.setDesignation(ProtoBridge.text(request.hasDesignation(), request.getDesignation()));
        changes.setChannel(ProtoBridge.text(request.hasChannel(), request.getChannel()));
        changes.setMessage(ProtoBridge.text(request.hasMessage(), request.getMessage()));
        changes.setStatus(bridge.choice(
                request.hasStatus(), request.getStatus(), SalesEnquiryUpdate.StatusEnum::fromValue));
        return bridge.patch(changes);
    }
}
