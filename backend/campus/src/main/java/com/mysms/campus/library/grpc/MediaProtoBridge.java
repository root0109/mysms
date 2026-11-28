package com.mysms.campus.library.grpc;

import com.mysms.campus.library.api.model.MediaUpdate;
import com.mysms.core.grpc.ProtoBridge;

/**
 * UpdateMedia over gRPC (JAVA-CODING-RULES §7.2): the {@code library.v1.MediaUpdate} message as REST's {@link
 * MediaUpdate} body, one explicit line per field: unset keeps the value, null clears it, a value sets it.
 */
final class MediaProtoBridge {

    private MediaProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static MediaUpdate changes(ProtoBridge bridge, com.mysms.library.v1.MediaUpdate request) {
        MediaUpdate changes = new MediaUpdate();
        changes.setInstitutionId(bridge.uuid(request.hasInstitutionId(), request.getInstitutionId(), "institution_id"));
        changes.setAcquiredAcademicYearId(bridge.uuid(
                request.hasAcquiredAcademicYearId(), request.getAcquiredAcademicYearId(), "acquired_academic_year_id"));
        changes.setTitle(ProtoBridge.text(request.hasTitle(), request.getTitle()));
        changes.setIsbn(ProtoBridge.text(request.hasIsbn(), request.getIsbn()));
        changes.setInternalCode(ProtoBridge.text(request.hasInternalCode(), request.getInternalCode()));
        changes.setEdition(ProtoBridge.text(request.hasEdition(), request.getEdition()));
        changes.setDescription(ProtoBridge.text(request.hasDescription(), request.getDescription()));
        changes.setMediaTypeId(bridge.uuid(request.hasMediaTypeId(), request.getMediaTypeId(), "media_type_id"));
        changes.setMediaCategoryId(bridge.uuid(
                request.hasMediaCategoryId(), request.getMediaCategoryId(), "media_category_id"));
        changes.setCoverImageFileId(bridge.uuid(
                request.hasCoverImageFileId(), request.getCoverImageFileId(), "cover_image_file_id"));
        changes.setIsActive(ProtoBridge.flag(request.hasIsActive(), request.getIsActive()));
        return bridge.patch(changes);
    }
}
