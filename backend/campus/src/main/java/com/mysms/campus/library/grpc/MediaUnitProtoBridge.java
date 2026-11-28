package com.mysms.campus.library.grpc;

import com.mysms.campus.library.api.model.MediaUnitUpdate;
import com.mysms.core.grpc.ProtoBridge;

/**
 * UpdateMediaUnit over gRPC (JAVA-CODING-RULES §7.2): the {@code library.v1.MediaUnitUpdate} message as REST's {@link
 * MediaUnitUpdate} body, one explicit line per field: unset keeps the value, null clears it, a value sets it.
 */
final class MediaUnitProtoBridge {

    private MediaUnitProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static MediaUnitUpdate changes(ProtoBridge bridge, com.mysms.library.v1.MediaUnitUpdate request) {
        MediaUnitUpdate changes = new MediaUnitUpdate();
        changes.setAccessionNumber(ProtoBridge.text(request.hasAccessionNumber(), request.getAccessionNumber()));
        changes.setBarcode(ProtoBridge.text(request.hasBarcode(), request.getBarcode()));
        changes.setStatus(bridge.choice(
                request.hasStatus(), request.getStatus(), MediaUnitUpdate.StatusEnum::fromValue));
        return bridge.patch(changes);
    }
}
