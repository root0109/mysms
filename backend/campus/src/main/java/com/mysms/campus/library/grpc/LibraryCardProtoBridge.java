package com.mysms.campus.library.grpc;

import com.mysms.campus.library.api.model.LibraryCardUpdate;
import com.mysms.core.grpc.ProtoBridge;

/**
 * UpdateLibraryCard over gRPC (JAVA-CODING-RULES §7.2): the {@code library.v1.LibraryCardUpdate} message as REST's
 * {@link LibraryCardUpdate} body, one explicit line per field: unset keeps the value, null clears it, a value sets it.
 */
final class LibraryCardProtoBridge {

    private LibraryCardProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static LibraryCardUpdate changes(ProtoBridge bridge, com.mysms.library.v1.LibraryCardUpdate request) {
        LibraryCardUpdate changes = new LibraryCardUpdate();
        changes.setInstitutionId(bridge.uuid(request.hasInstitutionId(), request.getInstitutionId(), "institution_id"));
        changes.setCardNumber(ProtoBridge.text(request.hasCardNumber(), request.getCardNumber()));
        changes.setCardTypeId(bridge.uuid(request.hasCardTypeId(), request.getCardTypeId(), "card_type_id"));
        changes.setHolderType(bridge.choice(
                request.hasHolderType(), request.getHolderType(), LibraryCardUpdate.HolderTypeEnum::fromValue));
        changes.setPersonId(bridge.uuid(request.hasPersonId(), request.getPersonId(), "person_id"));
        changes.setStudentId(bridge.uuid(request.hasStudentId(), request.getStudentId(), "student_id"));
        changes.setStaffId(bridge.uuid(request.hasStaffId(), request.getStaffId(), "staff_id"));
        changes.setMaxItemsOverride(ProtoBridge.number(request.hasMaxItemsOverride(), request.getMaxItemsOverride()));
        changes.setIssueDate(bridge.date(request.hasIssueDate(), request.getIssueDate(), "issue_date"));
        changes.setExpiryDate(bridge.date(request.hasExpiryDate(), request.getExpiryDate(), "expiry_date"));
        changes.setStatus(bridge.choice(
                request.hasStatus(), request.getStatus(), LibraryCardUpdate.StatusEnum::fromValue));
        return bridge.patch(changes);
    }
}
