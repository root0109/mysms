package com.mysms.learning.lms.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.learning.lms.api.model.EbookUpdate;

/**
 * UpdateEbook over gRPC (JAVA-CODING-RULES §7.2): the {@code lms.v1.EbookUpdate} message as REST's {@link EbookUpdate}
 * body, one explicit line per field: unset keeps the value, null clears it, a value sets it.
 */
final class EbookProtoBridge {

    private EbookProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static EbookUpdate changes(ProtoBridge bridge, com.mysms.lms.v1.EbookUpdate request) {
        EbookUpdate changes = new EbookUpdate();
        changes.setInstitutionId(bridge.uuid(request.hasInstitutionId(), request.getInstitutionId(), "institution_id"));
        changes.setTitle(ProtoBridge.text(request.hasTitle(), request.getTitle()));
        changes.setDescription(ProtoBridge.text(request.hasDescription(), request.getDescription()));
        changes.setEbookType(bridge.choice(
                request.hasEbookType(), request.getEbookType(), EbookUpdate.EbookTypeEnum::fromValue));
        changes.setFileId(bridge.uuid(request.hasFileId(), request.getFileId(), "file_id"));
        changes.setSourceFileId(bridge.uuid(request.hasSourceFileId(), request.getSourceFileId(), "source_file_id"));
        changes.setConversionJobRef(ProtoBridge.text(request.hasConversionJobRef(), request.getConversionJobRef()));
        changes.setShowRelatedMaterials(ProtoBridge.flag(
                request.hasShowRelatedMaterials(), request.getShowRelatedMaterials()));
        changes.setRelatedMaterialSeed(ProtoBridge.text(
                request.hasRelatedMaterialSeed(), request.getRelatedMaterialSeed()));
        changes.setCategoryId(bridge.uuid(request.hasCategoryId(), request.getCategoryId(), "category_id"));
        changes.setIsInRepository(ProtoBridge.flag(request.hasIsInRepository(), request.getIsInRepository()));
        return bridge.patch(changes);
    }
}
