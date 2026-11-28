package com.mysms.platform.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.platform.api.model.TenantUpdate;

/**
 * UpdateTenant over gRPC (JAVA-CODING-RULES §7.2): the {@code platform.v1.TenantUpdate} message
 * as REST's {@link TenantUpdate} body, one explicit line per field: unset keeps the value, null
 * clears it, a value sets it.
 */
final class TenantProtoBridge {

    private TenantProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static TenantUpdate changes(ProtoBridge bridge, com.mysms.platform.v1.TenantUpdate request) {
        TenantUpdate changes = new TenantUpdate();
        changes.setCode(ProtoBridge.text(request.hasCode(), request.getCode()));
        changes.setName(ProtoBridge.text(request.hasName(), request.getName()));
        changes.setLegalName(ProtoBridge.text(request.hasLegalName(), request.getLegalName()));
        changes.setStatus(bridge.choice(request.hasStatus(), request.getStatus(), TenantUpdate.StatusEnum::fromValue));
        changes.setPrimaryEmail(ProtoBridge.text(request.hasPrimaryEmail(), request.getPrimaryEmail()));
        changes.setPrimaryPhone(ProtoBridge.text(request.hasPrimaryPhone(), request.getPrimaryPhone()));
        changes.setCountryId(bridge.uuid(request.hasCountryId(), request.getCountryId(), "country_id"));
        changes.setDefaultCurrencyId(bridge.uuid(
                request.hasDefaultCurrencyId(), request.getDefaultCurrencyId(), "default_currency_id"));
        changes.setTimezone(ProtoBridge.text(request.hasTimezone(), request.getTimezone()));
        changes.setLocale(ProtoBridge.text(request.hasLocale(), request.getLocale()));
        return bridge.patch(changes);
    }
}
