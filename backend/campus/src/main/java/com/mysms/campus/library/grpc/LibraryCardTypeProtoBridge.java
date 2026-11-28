package com.mysms.campus.library.grpc;

import com.mysms.campus.library.api.model.LibraryCardTypeUpdate;
import com.mysms.core.grpc.ProtoBridge;

/**
 * UpdateLibraryCardType over gRPC (JAVA-CODING-RULES §7.2): the {@code library.v1.LibraryCardTypeUpdate} message as
 * REST's {@link LibraryCardTypeUpdate} body, one explicit line per field: unset keeps the value, null clears it, a
 * value sets it.
 */
final class LibraryCardTypeProtoBridge {

    private LibraryCardTypeProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static LibraryCardTypeUpdate changes(ProtoBridge bridge, com.mysms.library.v1.LibraryCardTypeUpdate request) {
        LibraryCardTypeUpdate changes = new LibraryCardTypeUpdate();
        changes.setCode(ProtoBridge.text(request.hasCode(), request.getCode()));
        changes.setSortOrder(ProtoBridge.number(request.hasSortOrder(), request.getSortOrder()));
        changes.setIsActive(ProtoBridge.flag(request.hasIsActive(), request.getIsActive()));
        changes.setName(ProtoBridge.text(request.hasName(), request.getName()));
        changes.setMaxItems(ProtoBridge.number(request.hasMaxItems(), request.getMaxItems()));
        changes.setLoanDays(ProtoBridge.number(request.hasLoanDays(), request.getLoanDays()));
        changes.setFinePerDay(ProtoBridge.text(request.hasFinePerDay(), request.getFinePerDay()));
        return bridge.patch(changes);
    }
}
