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
 * Fields of DisbursementAccount that can be updated (PATCH: a property left out keeps its value, null clears it, a value sets it).
 */

public class DisbursementAccountUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull UUID> institutionId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull UUID> managedByUserId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull @Size(max = 120) String> name;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull @Size(max = 40) String> accountKey;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull String> accountNumber;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull @Size(max = 20) String> bankRoutingCode;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@Size(max = 150) String> branchName;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@Size(max = 120) String> extraReference;

  public DisbursementAccountUpdate institutionId(@Nullable Optional<UUID> institutionId) {
    this.institutionId = institutionId;
    return this;
  }

  /**
   * Institution.
   * @return institutionId
   */
  
  @JsonProperty("institutionId")
  public @Nullable Optional<UUID> getInstitutionId() {
    return institutionId;
  }

  @JsonProperty("institutionId")
  public void setInstitutionId(@Nullable Optional<UUID> institutionId) {
    this.institutionId = institutionId;
  }

  public DisbursementAccountUpdate managedByUserId(@Nullable Optional<UUID> managedByUserId) {
    this.managedByUserId = managedByUserId;
    return this;
  }

  /**
   * User responsible for the account.
   * @return managedByUserId
   */
  
  @JsonProperty("managedByUserId")
  public @Nullable Optional<UUID> getManagedByUserId() {
    return managedByUserId;
  }

  @JsonProperty("managedByUserId")
  public void setManagedByUserId(@Nullable Optional<UUID> managedByUserId) {
    this.managedByUserId = managedByUserId;
  }

  public DisbursementAccountUpdate name(@Nullable Optional<String> name) {
    this.name = name;
    return this;
  }

  /**
   * Account label.
   * @return name
   */
  
  @JsonProperty("name")
  public @Nullable Optional<String> getName() {
    return name;
  }

  @JsonProperty("name")
  public void setName(@Nullable Optional<String> name) {
    this.name = name;
  }

  public DisbursementAccountUpdate accountKey(@Nullable Optional<String> accountKey) {
    this.accountKey = accountKey;
    return this;
  }

  /**
   * Short key.
   * @return accountKey
   */
  
  @JsonProperty("accountKey")
  public @Nullable Optional<String> getAccountKey() {
    return accountKey;
  }

  @JsonProperty("accountKey")
  public void setAccountKey(@Nullable Optional<String> accountKey) {
    this.accountKey = accountKey;
  }

  public DisbursementAccountUpdate accountNumber(@Nullable Optional<String> accountNumber) {
    this.accountNumber = accountNumber;
    return this;
  }

  /**
   * Plain value, input only: stored encrypted and never returned. Bank account number, encrypted.
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

  public DisbursementAccountUpdate bankRoutingCode(@Nullable Optional<String> bankRoutingCode) {
    this.bankRoutingCode = bankRoutingCode;
    return this;
  }

  /**
   * Bank routing code (IFSC / ABA / sort code).
   * @return bankRoutingCode
   */
  
  @JsonProperty("bankRoutingCode")
  public @Nullable Optional<String> getBankRoutingCode() {
    return bankRoutingCode;
  }

  @JsonProperty("bankRoutingCode")
  public void setBankRoutingCode(@Nullable Optional<String> bankRoutingCode) {
    this.bankRoutingCode = bankRoutingCode;
  }

  public DisbursementAccountUpdate branchName(@Nullable Optional<String> branchName) {
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

  public DisbursementAccountUpdate extraReference(@Nullable Optional<String> extraReference) {
    this.extraReference = extraReference;
    return this;
  }

  /**
   * Additional bank parameter (e.g. SWIFT).
   * @return extraReference
   */
  
  @JsonProperty("extraReference")
  public @Nullable Optional<String> getExtraReference() {
    return extraReference;
  }

  @JsonProperty("extraReference")
  public void setExtraReference(@Nullable Optional<String> extraReference) {
    this.extraReference = extraReference;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    DisbursementAccountUpdate disbursementAccountUpdate = (DisbursementAccountUpdate) o;
    return Objects.equals(this.institutionId, disbursementAccountUpdate.institutionId) &&
        Objects.equals(this.managedByUserId, disbursementAccountUpdate.managedByUserId) &&
        Objects.equals(this.name, disbursementAccountUpdate.name) &&
        Objects.equals(this.accountKey, disbursementAccountUpdate.accountKey) &&
        Objects.equals(this.accountNumber, disbursementAccountUpdate.accountNumber) &&
        Objects.equals(this.bankRoutingCode, disbursementAccountUpdate.bankRoutingCode) &&
        Objects.equals(this.branchName, disbursementAccountUpdate.branchName) &&
        Objects.equals(this.extraReference, disbursementAccountUpdate.extraReference);
  }

  @Override
  public int hashCode() {
    return Objects.hash(institutionId, managedByUserId, name, accountKey, accountNumber, bankRoutingCode, branchName, extraReference);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class DisbursementAccountUpdate {\n");
    sb.append("    institutionId: ").append(toIndentedString(institutionId)).append("\n");
    sb.append("    managedByUserId: ").append(toIndentedString(managedByUserId)).append("\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    accountKey: ").append(toIndentedString(accountKey)).append("\n");
    sb.append("    accountNumber: ").append(toIndentedString(accountNumber)).append("\n");
    sb.append("    bankRoutingCode: ").append(toIndentedString(bankRoutingCode)).append("\n");
    sb.append("    branchName: ").append(toIndentedString(branchName)).append("\n");
    sb.append("    extraReference: ").append(toIndentedString(extraReference)).append("\n");
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

