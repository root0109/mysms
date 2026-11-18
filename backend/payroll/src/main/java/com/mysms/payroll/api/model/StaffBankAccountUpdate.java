// Owned API contract of payroll.
package com.mysms.payroll.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import java.util.UUID;
import org.springframework.lang.Nullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;

/**
 * Fields of StaffBankAccount that can be updated (partial update; see updateMask).
 */

public class StaffBankAccountUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String accountNumber;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID staffId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String accountHolderName;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String ifscCode;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String bankName;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String branchName;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Boolean isPrimary;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Boolean isActive;

  public StaffBankAccountUpdate accountNumber(@Nullable String accountNumber) {
    this.accountNumber = accountNumber;
    return this;
  }

  /**
   * Account number; stored encrypted, never returned.
   * @return accountNumber
   */
  
  @JsonProperty("accountNumber")
  public @Nullable String getAccountNumber() {
    return accountNumber;
  }

  @JsonProperty("accountNumber")
  public void setAccountNumber(@Nullable String accountNumber) {
    this.accountNumber = accountNumber;
  }

  public StaffBankAccountUpdate staffId(@Nullable UUID staffId) {
    this.staffId = staffId;
    return this;
  }

  /**
   * Staff member.
   * @return staffId
   */
  @Valid 
  @JsonProperty("staffId")
  public @Nullable UUID getStaffId() {
    return staffId;
  }

  @JsonProperty("staffId")
  public void setStaffId(@Nullable UUID staffId) {
    this.staffId = staffId;
  }

  public StaffBankAccountUpdate accountHolderName(@Nullable String accountHolderName) {
    this.accountHolderName = accountHolderName;
    return this;
  }

  /**
   * Name on the account.
   * @return accountHolderName
   */
  @Size(max = 200) 
  @JsonProperty("accountHolderName")
  public @Nullable String getAccountHolderName() {
    return accountHolderName;
  }

  @JsonProperty("accountHolderName")
  public void setAccountHolderName(@Nullable String accountHolderName) {
    this.accountHolderName = accountHolderName;
  }

  public StaffBankAccountUpdate ifscCode(@Nullable String ifscCode) {
    this.ifscCode = ifscCode;
    return this;
  }

  /**
   * Bank routing code.
   * @return ifscCode
   */
  @Size(max = 20) 
  @JsonProperty("ifscCode")
  public @Nullable String getIfscCode() {
    return ifscCode;
  }

  @JsonProperty("ifscCode")
  public void setIfscCode(@Nullable String ifscCode) {
    this.ifscCode = ifscCode;
  }

  public StaffBankAccountUpdate bankName(@Nullable String bankName) {
    this.bankName = bankName;
    return this;
  }

  /**
   * Bank.
   * @return bankName
   */
  @Size(max = 120) 
  @JsonProperty("bankName")
  public @Nullable String getBankName() {
    return bankName;
  }

  @JsonProperty("bankName")
  public void setBankName(@Nullable String bankName) {
    this.bankName = bankName;
  }

  public StaffBankAccountUpdate branchName(@Nullable String branchName) {
    this.branchName = branchName;
    return this;
  }

  /**
   * Branch.
   * @return branchName
   */
  @Size(max = 150) 
  @JsonProperty("branchName")
  public @Nullable String getBranchName() {
    return branchName;
  }

  @JsonProperty("branchName")
  public void setBranchName(@Nullable String branchName) {
    this.branchName = branchName;
  }

  public StaffBankAccountUpdate isPrimary(@Nullable Boolean isPrimary) {
    this.isPrimary = isPrimary;
    return this;
  }

  /**
   * Account currently used for disbursement.
   * @return isPrimary
   */
  
  @JsonProperty("isPrimary")
  public @Nullable Boolean getIsPrimary() {
    return isPrimary;
  }

  @JsonProperty("isPrimary")
  public void setIsPrimary(@Nullable Boolean isPrimary) {
    this.isPrimary = isPrimary;
  }

  public StaffBankAccountUpdate isActive(@Nullable Boolean isActive) {
    this.isActive = isActive;
    return this;
  }

  /**
   * Whether usable.
   * @return isActive
   */
  
  @JsonProperty("isActive")
  public @Nullable Boolean getIsActive() {
    return isActive;
  }

  @JsonProperty("isActive")
  public void setIsActive(@Nullable Boolean isActive) {
    this.isActive = isActive;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    StaffBankAccountUpdate staffBankAccountUpdate = (StaffBankAccountUpdate) o;
    return Objects.equals(this.accountNumber, staffBankAccountUpdate.accountNumber) &&
        Objects.equals(this.staffId, staffBankAccountUpdate.staffId) &&
        Objects.equals(this.accountHolderName, staffBankAccountUpdate.accountHolderName) &&
        Objects.equals(this.ifscCode, staffBankAccountUpdate.ifscCode) &&
        Objects.equals(this.bankName, staffBankAccountUpdate.bankName) &&
        Objects.equals(this.branchName, staffBankAccountUpdate.branchName) &&
        Objects.equals(this.isPrimary, staffBankAccountUpdate.isPrimary) &&
        Objects.equals(this.isActive, staffBankAccountUpdate.isActive);
  }

  @Override
  public int hashCode() {
    return Objects.hash(accountNumber, staffId, accountHolderName, ifscCode, bankName, branchName, isPrimary, isActive);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class StaffBankAccountUpdate {\n");
    sb.append("    accountNumber: ").append(toIndentedString(accountNumber)).append("\n");
    sb.append("    staffId: ").append(toIndentedString(staffId)).append("\n");
    sb.append("    accountHolderName: ").append(toIndentedString(accountHolderName)).append("\n");
    sb.append("    ifscCode: ").append(toIndentedString(ifscCode)).append("\n");
    sb.append("    bankName: ").append(toIndentedString(bankName)).append("\n");
    sb.append("    branchName: ").append(toIndentedString(branchName)).append("\n");
    sb.append("    isPrimary: ").append(toIndentedString(isPrimary)).append("\n");
    sb.append("    isActive: ").append(toIndentedString(isActive)).append("\n");
    sb.append("}");
    return sb.toString();
  }

  /**
   * Convert the given object to string with each line indented by 4 spaces
   * (except the first line).
   */
  private String toIndentedString(@Nullable Object o) {
    return o == null ? "null" : o.toString().replace("\n", "\n    ");
  }
}

