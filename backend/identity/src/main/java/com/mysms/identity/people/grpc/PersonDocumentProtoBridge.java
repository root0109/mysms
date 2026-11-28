package com.mysms.identity.people.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.identity.people.api.model.PersonDocumentUpdate;

/**
 * UpdatePersonDocument over gRPC (JAVA-CODING-RULES §7.2): the {@code people.v1.PersonDocumentUpdate} message
 * as REST's {@link PersonDocumentUpdate} body, one explicit line per field: unset keeps the value, null
 * clears it, a value sets it.
 */
final class PersonDocumentProtoBridge {

    private PersonDocumentProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static PersonDocumentUpdate changes(ProtoBridge bridge, com.mysms.people.v1.PersonDocumentUpdate request) {
        PersonDocumentUpdate changes = new PersonDocumentUpdate();
        changes.setInstitutionId(bridge.uuid(request.hasInstitutionId(), request.getInstitutionId(), "institution_id"));
        changes.setDocumentType(bridge.choice(
                request.hasDocumentType(), request.getDocumentType(), PersonDocumentUpdate.DocumentTypeEnum::fromValue));
        changes.setName(ProtoBridge.text(request.hasName(), request.getName()));
        changes.setFileId(bridge.uuid(request.hasFileId(), request.getFileId(), "file_id"));
        changes.setDocumentVersion(ProtoBridge.number(request.hasDocumentVersion(), request.getDocumentVersion()));
        changes.setStatus(bridge.choice(
                request.hasStatus(), request.getStatus(), PersonDocumentUpdate.StatusEnum::fromValue));
        return bridge.patch(changes);
    }
}
