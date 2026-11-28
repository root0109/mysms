package com.mysms.campus.library.grpc;

import com.mysms.campus.library.api.model.AuthorUpdate;
import com.mysms.core.grpc.ProtoBridge;

/**
 * UpdateAuthor over gRPC (JAVA-CODING-RULES §7.2): the {@code library.v1.AuthorUpdate} message as REST's {@link
 * AuthorUpdate} body, one explicit line per field: unset keeps the value, null clears it, a value sets it.
 */
final class AuthorProtoBridge {

    private AuthorProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static AuthorUpdate changes(ProtoBridge bridge, com.mysms.library.v1.AuthorUpdate request) {
        AuthorUpdate changes = new AuthorUpdate();
        changes.setName(ProtoBridge.text(request.hasName(), request.getName()));
        return bridge.patch(changes);
    }
}
