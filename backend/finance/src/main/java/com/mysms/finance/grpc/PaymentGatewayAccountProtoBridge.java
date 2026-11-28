package com.mysms.finance.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.finance.api.model.PaymentGatewayAccountUpdate;

/**
 * UpdatePaymentGatewayAccount over gRPC (JAVA-CODING-RULES §7.2): the {@code finance.v1.PaymentGatewayAccountUpdate} message
 * as REST's {@link PaymentGatewayAccountUpdate} body, one explicit line per field: unset keeps the value, null
 * clears it, a value sets it.
 */
final class PaymentGatewayAccountProtoBridge {

    private PaymentGatewayAccountProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static PaymentGatewayAccountUpdate changes(ProtoBridge bridge, com.mysms.finance.v1.PaymentGatewayAccountUpdate request) {
        PaymentGatewayAccountUpdate changes = new PaymentGatewayAccountUpdate();
        changes.setKeySecret(ProtoBridge.text(request.hasKeySecret(), request.getKeySecret()));
        changes.setWebhookSecret(ProtoBridge.text(request.hasWebhookSecret(), request.getWebhookSecret()));
        changes.setInstitutionId(bridge.uuid(request.hasInstitutionId(), request.getInstitutionId(), "institution_id"));
        changes.setGatewayName(ProtoBridge.text(request.hasGatewayName(), request.getGatewayName()));
        changes.setDisplayName(ProtoBridge.text(request.hasDisplayName(), request.getDisplayName()));
        changes.setMerchantId(ProtoBridge.text(request.hasMerchantId(), request.getMerchantId()));
        changes.setKeyId(ProtoBridge.text(request.hasKeyId(), request.getKeyId()));
        changes.setCurrencyId(bridge.uuid(request.hasCurrencyId(), request.getCurrencyId(), "currency_id"));
        changes.setIsDefault(ProtoBridge.flag(request.hasIsDefault(), request.getIsDefault()));
        changes.setIsActive(ProtoBridge.flag(request.hasIsActive(), request.getIsActive()));
        return bridge.patch(changes);
    }
}
