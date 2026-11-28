package com.mysms.learning.lms.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.learning.lms.api.model.SettingsPrimaryCountryUpdate;

/**
 * UpdateSettingsPrimaryCountry over gRPC (JAVA-CODING-RULES §7.2): the {@code lms.v1.SettingsPrimaryCountryUpdate}
 * message as REST's {@link SettingsPrimaryCountryUpdate} body, one explicit line per field: unset keeps the value, null
 * clears it, a value sets it.
 */
final class SettingsPrimaryCountryProtoBridge {

    private SettingsPrimaryCountryProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static SettingsPrimaryCountryUpdate changes(
            ProtoBridge bridge, com.mysms.lms.v1.SettingsPrimaryCountryUpdate request) {
        SettingsPrimaryCountryUpdate changes = new SettingsPrimaryCountryUpdate();
        changes.setCountryId(bridge.uuid(request.hasCountryId(), request.getCountryId(), "country_id"));
        return bridge.patch(changes);
    }
}
