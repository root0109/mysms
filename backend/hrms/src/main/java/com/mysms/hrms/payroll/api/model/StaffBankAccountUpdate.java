// Owned API contract of payroll.
package com.mysms.hrms.payroll.api.model;

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
 * Fields of StaffBankAccount that can be updated (PATCH: a property left out keeps its value, null clears it, a value sets it).
 */

public class StaffBankAccountUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull String> accountNumber;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull UUID> staffId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull @Size(max = 200) String> accountHolderName;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@Size(max = 20) String> ifscCode;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@Size(max = 120) String> bankName;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@Size(max = 150) String> branchName;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull Boolean> isPrimary;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull Boolean> isActive;

  public StaffBankAccountUpdate accountNumber(@Nullable Optional<String> accountNumber) {
    this.accountNumber = accountNumber;
    return this;
  }

  /**
   * Account number; stored encrypted, never returned.
   * @return accountNumber
   */
  
  @JsonProperty("accountNumber")
  public @Nullable Optional<String> getAccountNumber() {
    return accountNumber;
  }

  @JsonProperty("accountNumber")
  public void setAccountNumber(@Nullable Optional<String> accountNumber) {
    this.accountNumber = accountNumber;
  }

  public StaffBankAccountUpdate staffId(@Nullable Optional<UUID> staffId) {
    this.staffId = staffId;
    return this;
  }

  /**
   * Staff member.
   * @return staffId
   */
  
  @JsonProperty("staffId")
  public @Nullable Optional<UUID> getStaffId() {
    return staffId;
  }

  @JsonProperty("staffId")
  public void setStaffId(@Nullable Optional<UUID> staffId) {
    this.staffId = staffId;
  }

  public StaffBankAccountUpdate accountHolderName(@Nullable Optional<String> accountHolderName) {
    this.accountHolderName = accountHolderName;
    return this;
  }

  /**
   * Name on the account.
   * @return accountHolderName
   */
  
  @JsonProperty("accountHolderName")
  public @Nullable Optional<String> getAccountHolderName() {
    return accountHolderName;
  }

  @JsonProperty("accountHolderName")
  public void setAccountHolderName(@Nullable Optional<String> accountHolderName) {
    this.accountHolderName = accountHolderName;
  }

  public StaffBankAccountUpdate ifscCode(@Nullable Optional<String> ifscCode) {
    this.ifscCode = ifscCode;
    return this;
  }

  /**
   * Bank routing code.
   * @return ifscCode
   */
  
  @JsonProperty("ifscCode")
  public @Nullable Optional<String> getIfscCode() {
    return ifscCode;
  }

  @JsonProperty("ifscCode")
  public void setIfscCode(@Nullable Optional<String> ifscCode) {
    this.ifscCode = ifscCode;
  }

  public StaffBankAccountUpdate bankName(@Nullable Optional<String> bankName) {
    this.bankName = bankName;
    return this;
  }

  /**
   * Bank.
   * @return bankName
   */
  
  @JsonProperty("bankName")
  public @Nullable Optional<String> getBankName() {
    return bankName;
  }

  @JsonProperty("bankName")
  public void setBankName(@Nullable Optional<String> bankName) {
    this.bankName = bankName;
  }

  public StaffBankAccountUpdate branchName(@Nullable Optional<String> branchName) {
    this.branchName = branchName;
    return this;
  }

  /**
   * Branch.
   * @return branchName
   */
  
  @JsonProperty("branchName")
  public @Nullable Optional<String> getBranchName() {
    return branchName;
  }

  @JsonProperty("branchName")
  public void setBranchName(@Nullable Optional<String> branchName) {
    this.branchName = branchName;
  }

  public StaffBankAccountUpdate isPrimary(@Nullable Optional<Boolean> isPrimary) {
    this.isPrimary = isPrimary;
    return this;
  }

  /**
   * Account currently used for disbursement.
   * @return isPrimary
   */
  
  @JsonProperty("isPrimary")
  public @Nullable Optional<Boolean> getIsPrimary() {
    return isPrimary;
  }

  @JsonProperty("isPrimary")
  public void setIsPrimary(@Nullable Optional<Boolean> isPrimary) {
    this.isPrimary = isPrimary;
  }

  public StaffBankAccountUpdate isActive(@Nullable Optional<Boolean> isActive) {
    this.isActive = isActive;
    return this;
  }

  /**
   * Whether usable.
   * @return isActive
   */
  
  @JsonProperty("isActive")
  public @Nullable Optional<Boolean> getIsActive() {
    return isActive;
  }

  @JsonProperty("isActive")
  public void setIsActive(@Nullable Optional<Boolean> isActive) {
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

