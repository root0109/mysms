package com.mysms.learning.lms.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.learning.lms.api.model.BookmarkUpdate;

/**
 * UpdateBookmark over gRPC (JAVA-CODING-RULES §7.2): the {@code lms.v1.BookmarkUpdate} message as REST's {@link
 * BookmarkUpdate} body, one explicit line per field: unset keeps the value, null clears it, a value sets it.
 */
final class BookmarkProtoBridge {

    private BookmarkProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static BookmarkUpdate changes(ProtoBridge bridge, com.mysms.lms.v1.BookmarkUpdate request) {
        BookmarkUpdate changes = new BookmarkUpdate();
        changes.setTitle(ProtoBridge.text(request.hasTitle(), request.getTitle()));
        changes.setUrl(ProtoBridge.text(request.hasUrl(), request.getUrl()));
        changes.setModuleCode(ProtoBridge.text(request.hasModuleCode(), request.getModuleCode()));
        changes.setCohortId(bridge.uuid(request.hasCohortId(), request.getCohortId(), "cohort_id"));
        changes.setSavedAt(ProtoBridge.time(request.hasSavedAt(), request.getSavedAt()));
        return bridge.patch(changes);
    }
}
