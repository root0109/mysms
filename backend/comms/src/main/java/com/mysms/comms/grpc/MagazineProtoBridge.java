package com.mysms.comms.grpc;

import com.mysms.comms.api.model.MagazineUpdate;
import com.mysms.core.grpc.ProtoBridge;

/**
 * UpdateMagazine over gRPC (JAVA-CODING-RULES §7.2): the {@code comms.v1.MagazineUpdate} message
 * as REST's {@link MagazineUpdate} body, one explicit line per field: unset keeps the value, null
 * clears it, a value sets it.
 */
final class MagazineProtoBridge {

    private MagazineProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static MagazineUpdate changes(ProtoBridge bridge, com.mysms.comms.v1.MagazineUpdate request) {
        MagazineUpdate changes = new MagazineUpdate();
        changes.setInstitutionId(bridge.uuid(request.hasInstitutionId(), request.getInstitutionId(), "institution_id"));
        changes.setAcademicYearId(bridge.uuid(
                request.hasAcademicYearId(), request.getAcademicYearId(), "academic_year_id"));
        changes.setTitle(ProtoBridge.text(request.hasTitle(), request.getTitle()));
        changes.setPublicationYear(ProtoBridge.number(request.hasPublicationYear(), request.getPublicationYear()));
        changes.setCoverImageFileId(bridge.uuid(
                request.hasCoverImageFileId(), request.getCoverImageFileId(), "cover_image_file_id"));
        changes.setDocumentFileId(bridge.uuid(
                request.hasDocumentFileId(), request.getDocumentFileId(), "document_file_id"));
        return bridge.patch(changes);
    }
}
