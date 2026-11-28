package com.mysms.hrms.payroll.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.hrms.payroll.api.model.DisbursementAccountUpdate;

/**
 * UpdateDisbursementAccount over gRPC (JAVA-CODING-RULES §7.2): the {@code payroll.v1.DisbursementAccountUpdate}
 * message as REST's {@link DisbursementAccountUpdate} body, one explicit line per field: unset keeps the value, null
 * clears it, a value sets it.
 */
final class DisbursementAccountProtoBridge {

    private DisbursementAccountProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static DisbursementAccountUpdate changes(
            ProtoBridge bridge, com.mysms.payroll.v1.DisbursementAccountUpdate request) {
        DisbursementAccountUpdate changes = new DisbursementAccountUpdate();
        changes.setInstitutionId(bridge.uuid(request.hasInstitutionId(), request.getInstitutionId(), "institution_id"));
        changes.setManagedByUserId(bridge.uuid(
                request.hasManagedByUserId(), request.getManagedByUserId(), "managed_by_user_id"));
        changes.setName(ProtoBridge.text(request.hasName(), request.getName()));
        changes.setAccountKey(ProtoBridge.text(request.hasAccountKey(), request.getAccountKey()));
        changes.setAccountNumber(ProtoBridge.text(request.hasAccountNumber(), request.getAccountNumber()));
        changes.setBankRoutingCode(ProtoBridge.text(request.hasBankRoutingCode(), request.getBankRoutingCode()));
        changes.setBranchName(ProtoBridge.text(request.hasBranchName(), request.getBranchName()));
        changes.setExtraReference(ProtoBridge.text(request.hasExtraReference(), request.getExtraReference()));
        return bridge.patch(changes);
    }
}
