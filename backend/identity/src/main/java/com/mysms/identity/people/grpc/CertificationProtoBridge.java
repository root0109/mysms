package com.mysms.identity.people.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.identity.people.api.model.CertificationUpdate;

/**
 * UpdateCertification over gRPC (JAVA-CODING-RULES §7.2): the {@code people.v1.CertificationUpdate} message
 * as REST's {@link CertificationUpdate} body, one explicit line per field: unset keeps the value, null
 * clears it, a value sets it.
 */
final class CertificationProtoBridge {

    private CertificationProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static CertificationUpdate changes(ProtoBridge bridge, com.mysms.people.v1.CertificationUpdate request) {
        CertificationUpdate changes = new CertificationUpdate();
        changes.setName(ProtoBridge.text(request.hasName(), request.getName()));
        changes.setIssuingOrganization(ProtoBridge.text(
                request.hasIssuingOrganization(), request.getIssuingOrganization()));
        changes.setIssueDate(bridge.date(request.hasIssueDate(), request.getIssueDate(), "issue_date"));
        changes.setExpires(ProtoBridge.flag(request.hasExpires(), request.getExpires()));
        changes.setExpirationDate(bridge.date(
                request.hasExpirationDate(), request.getExpirationDate(), "expiration_date"));
        changes.setDescription(ProtoBridge.text(request.hasDescription(), request.getDescription()));
        return bridge.patch(changes);
    }
}
