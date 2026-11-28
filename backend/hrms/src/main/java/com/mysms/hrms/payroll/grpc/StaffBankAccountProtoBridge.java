package com.mysms.hrms.payroll.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.hrms.payroll.api.model.StaffBankAccountUpdate;

/**
 * UpdateStaffBankAccount over gRPC (JAVA-CODING-RULES §7.2): the {@code payroll.v1.StaffBankAccountUpdate} message as
 * REST's {@link StaffBankAccountUpdate} body, one explicit line per field: unset keeps the value, null clears it, a
 * value sets it.
 */
final class StaffBankAccountProtoBridge {

    private StaffBankAccountProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static StaffBankAccountUpdate changes(ProtoBridge bridge, com.mysms.payroll.v1.StaffBankAccountUpdate request) {
        StaffBankAccountUpdate changes = new StaffBankAccountUpdate();
        changes.setAccountNumber(ProtoBridge.text(request.hasAccountNumber(), request.getAccountNumber()));
        changes.setStaffId(bridge.uuid(request.hasStaffId(), request.getStaffId(), "staff_id"));
        changes.setAccountHolderName(ProtoBridge.text(request.hasAccountHolderName(), request.getAccountHolderName()));
        changes.setIfscCode(ProtoBridge.text(request.hasIfscCode(), request.getIfscCode()));
        changes.setBankName(ProtoBridge.text(request.hasBankName(), request.getBankName()));
        changes.setBranchName(ProtoBridge.text(request.hasBranchName(), request.getBranchName()));
        changes.setIsPrimary(ProtoBridge.flag(request.hasIsPrimary(), request.getIsPrimary()));
        changes.setIsActive(ProtoBridge.flag(request.hasIsActive(), request.getIsActive()));
        return bridge.patch(changes);
    }
}
