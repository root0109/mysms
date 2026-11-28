package com.mysms.comms.grpc;

import com.mysms.comms.api.model.PageCategoryUpdate;
import com.mysms.core.grpc.ProtoBridge;

/**
 * UpdatePageCategory over gRPC (JAVA-CODING-RULES §7.2): the {@code comms.v1.PageCategoryUpdate} message
 * as REST's {@link PageCategoryUpdate} body, one explicit line per field: unset keeps the value, null
 * clears it, a value sets it.
 */
final class PageCategoryProtoBridge {

    private PageCategoryProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static PageCategoryUpdate changes(ProtoBridge bridge, com.mysms.comms.v1.PageCategoryUpdate request) {
        PageCategoryUpdate changes = new PageCategoryUpdate();
        changes.setCode(ProtoBridge.text(request.hasCode(), request.getCode()));
        changes.setSortOrder(ProtoBridge.number(request.hasSortOrder(), request.getSortOrder()));
        changes.setInstitutionId(bridge.uuid(request.hasInstitutionId(), request.getInstitutionId(), "institution_id"));
        changes.setAcademicYearId(bridge.uuid(
                request.hasAcademicYearId(), request.getAcademicYearId(), "academic_year_id"));
        changes.setName(ProtoBridge.text(request.hasName(), request.getName()));
        changes.setIsActive(ProtoBridge.flag(request.hasIsActive(), request.getIsActive()));
        return bridge.patch(changes);
    }
}
