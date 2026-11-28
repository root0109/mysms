package com.mysms.academics.grpc;

import com.mysms.academics.api.model.GradeLevelUpdate;
import com.mysms.core.grpc.ProtoBridge;

/**
 * UpdateGradeLevel over gRPC (JAVA-CODING-RULES §7.2): the {@code academics.v1.GradeLevelUpdate} message as REST's
 * {@link GradeLevelUpdate} body, one explicit line per field: unset keeps the value, null clears it, a value sets it.
 */
final class GradeLevelProtoBridge {

    private GradeLevelProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static GradeLevelUpdate changes(ProtoBridge bridge, com.mysms.academics.v1.GradeLevelUpdate request) {
        GradeLevelUpdate changes = new GradeLevelUpdate();
        changes.setInstitutionId(bridge.uuid(request.hasInstitutionId(), request.getInstitutionId(), "institution_id"));
        changes.setName(ProtoBridge.text(request.hasName(), request.getName()));
        changes.setSlug(ProtoBridge.text(request.hasSlug(), request.getSlug()));
        changes.setSortOrder(ProtoBridge.number(request.hasSortOrder(), request.getSortOrder()));
        changes.setIsActive(ProtoBridge.flag(request.hasIsActive(), request.getIsActive()));
        return bridge.patch(changes);
    }
}
