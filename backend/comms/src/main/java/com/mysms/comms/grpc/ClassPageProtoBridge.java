package com.mysms.comms.grpc;

import com.mysms.comms.api.model.ClassPageUpdate;
import com.mysms.core.grpc.ProtoBridge;

/**
 * UpdateClassPage over gRPC (JAVA-CODING-RULES §7.2): the {@code comms.v1.ClassPageUpdate} message
 * as REST's {@link ClassPageUpdate} body, one explicit line per field: unset keeps the value, null
 * clears it, a value sets it.
 */
final class ClassPageProtoBridge {

    private ClassPageProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static ClassPageUpdate changes(ProtoBridge bridge, com.mysms.comms.v1.ClassPageUpdate request) {
        ClassPageUpdate changes = new ClassPageUpdate();
        changes.setInstitutionId(bridge.uuid(request.hasInstitutionId(), request.getInstitutionId(), "institution_id"));
        changes.setAcademicYearId(bridge.uuid(
                request.hasAcademicYearId(), request.getAcademicYearId(), "academic_year_id"));
        changes.setPageCategoryId(bridge.uuid(
                request.hasPageCategoryId(), request.getPageCategoryId(), "page_category_id"));
        changes.setName(ProtoBridge.text(request.hasName(), request.getName()));
        changes.setDescription(ProtoBridge.text(request.hasDescription(), request.getDescription()));
        changes.setCoverImageFileId(bridge.uuid(
                request.hasCoverImageFileId(), request.getCoverImageFileId(), "cover_image_file_id"));
        changes.setOwnerUserId(bridge.uuid(request.hasOwnerUserId(), request.getOwnerUserId(), "owner_user_id"));
        changes.setIsActive(ProtoBridge.flag(request.hasIsActive(), request.getIsActive()));
        return bridge.patch(changes);
    }
}
