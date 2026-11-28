package com.mysms.comms.grpc;

import com.mysms.comms.api.model.PostUpdate;
import com.mysms.core.grpc.ProtoBridge;

/**
 * UpdatePost over gRPC (JAVA-CODING-RULES §7.2): the {@code comms.v1.PostUpdate} message
 * as REST's {@link PostUpdate} body, one explicit line per field: unset keeps the value, null
 * clears it, a value sets it.
 */
final class PostProtoBridge {

    private PostProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static PostUpdate changes(ProtoBridge bridge, com.mysms.comms.v1.PostUpdate request) {
        PostUpdate changes = new PostUpdate();
        changes.setInstitutionId(bridge.uuid(request.hasInstitutionId(), request.getInstitutionId(), "institution_id"));
        changes.setAcademicYearId(bridge.uuid(
                request.hasAcademicYearId(), request.getAcademicYearId(), "academic_year_id"));
        changes.setClassPageId(bridge.uuid(request.hasClassPageId(), request.getClassPageId(), "class_page_id"));
        changes.setBody(ProtoBridge.text(request.hasBody(), request.getBody()));
        changes.setVisibility(bridge.choice(
                request.hasVisibility(), request.getVisibility(), PostUpdate.VisibilityEnum::fromValue));
        changes.setScheduledFor(ProtoBridge.time(request.hasScheduledFor(), request.getScheduledFor()));
        return bridge.patch(changes);
    }
}
