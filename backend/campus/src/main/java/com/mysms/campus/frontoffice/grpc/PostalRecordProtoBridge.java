package com.mysms.campus.frontoffice.grpc;

import com.mysms.campus.frontoffice.api.model.PostalRecordUpdate;
import com.mysms.core.grpc.ProtoBridge;

/**
 * UpdatePostalRecord over gRPC (JAVA-CODING-RULES §7.2): the {@code frontoffice.v1.PostalRecordUpdate} message as
 * REST's {@link PostalRecordUpdate} body, one explicit line per field: unset keeps the value, null clears it, a value
 * sets it.
 */
final class PostalRecordProtoBridge {

    private PostalRecordProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static PostalRecordUpdate changes(ProtoBridge bridge, com.mysms.frontoffice.v1.PostalRecordUpdate request) {
        PostalRecordUpdate changes = new PostalRecordUpdate();
        changes.setInstitutionId(bridge.uuid(request.hasInstitutionId(), request.getInstitutionId(), "institution_id"));
        changes.setAcademicYearId(bridge.uuid(
                request.hasAcademicYearId(), request.getAcademicYearId(), "academic_year_id"));
        changes.setDirection(bridge.choice(
                request.hasDirection(), request.getDirection(), PostalRecordUpdate.DirectionEnum::fromValue));
        changes.setPostType(ProtoBridge.text(request.hasPostType(), request.getPostType()));
        changes.setReferenceNumber(ProtoBridge.text(request.hasReferenceNumber(), request.getReferenceNumber()));
        changes.setIsConfidential(ProtoBridge.flag(request.hasIsConfidential(), request.getIsConfidential()));
        changes.setSenderName(ProtoBridge.text(request.hasSenderName(), request.getSenderName()));
        changes.setSenderAddress(ProtoBridge.text(request.hasSenderAddress(), request.getSenderAddress()));
        changes.setReceiverName(ProtoBridge.text(request.hasReceiverName(), request.getReceiverName()));
        changes.setReceiverAddress(ProtoBridge.text(request.hasReceiverAddress(), request.getReceiverAddress()));
        changes.setPostalDate(bridge.date(request.hasPostalDate(), request.getPostalDate(), "postal_date"));
        changes.setDescription(ProtoBridge.text(request.hasDescription(), request.getDescription()));
        changes.setAttachmentFileId(bridge.uuid(
                request.hasAttachmentFileId(), request.getAttachmentFileId(), "attachment_file_id"));
        changes.setLoggedByUserId(bridge.uuid(
                request.hasLoggedByUserId(), request.getLoggedByUserId(), "logged_by_user_id"));
        return bridge.patch(changes);
    }
}
