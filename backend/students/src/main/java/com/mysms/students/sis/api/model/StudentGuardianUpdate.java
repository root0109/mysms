// Owned API contract of sis.
package com.mysms.students.sis.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.UUID;
import org.springframework.lang.Nullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;

/**
 * Fields of StudentGuardian that can be updated (PATCH: a property left out keeps its value, null clears it, a value sets it).
 */

public class StudentGuardianUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull Boolean> isLegalGuardian;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull Boolean> hasCustody;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull UUID> guardianId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<UUID> relationshipTypeId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull Boolean> isPrimaryContact;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull Boolean> receivesCommunication;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull Boolean> canPickUp;

  /**
   * Link state.
   */
  public enum StatusEnum {
    ACTIVE("active"),
    
    INACTIVE("inactive");

    private final String value;

    StatusEnum(String value) {
      this.value = value;
    }

    @JsonValue
    public String getValue() {
      return value;
    }

    @Override
    public String toString() {
      return String.valueOf(value);
    }

    @JsonCreator
    public static StatusEnum fromValue(String value) {
      for (StatusEnum b : StatusEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull StatusEnum> status;

  public StudentGuardianUpdate isLegalGuardian(@Nullable Optional<Boolean> isLegalGuardian) {
    this.isLegalGuardian = isLegalGuardian;
    return this;
  }

  /**
   * Whether this guardian is a legal guardian (consent authority).
   * @return isLegalGuardian
   */
  
  @JsonProperty("isLegalGuardian")
  public @Nullable Optional<Boolean> getIsLegalGuardian() {
    return isLegalGuardian;
  }

  @JsonProperty("isLegalGuardian")
  public void setIsLegalGuardian(@Nullable Optional<Boolean> isLegalGuardian) {
    this.isLegalGuardian = isLegalGuardian;
  }

  public StudentGuardianUpdate hasCustody(@Nullable Optional<Boolean> hasCustody) {
    this.hasCustody = hasCustody;
    return this;
  }

  /**
   * Whether this guardian has custody; drives pickup and visibility rules.
   * @return hasCustody
   */
  
  @JsonProperty("hasCustody")
  public @Nullable Optional<Boolean> getHasCustody() {
    return hasCustody;
  }

  @JsonProperty("hasCustody")
  public void setHasCustody(@Nullable Optional<Boolean> hasCustody) {
    this.hasCustody = hasCustody;
  }

  public StudentGuardianUpdate guardianId(@Nullable Optional<UUID> guardianId) {
    this.guardianId = guardianId;
    return this;
  }

  /**
   * Guardian.
   * @return guardianId
   */
  
  @JsonProperty("guardianId")
  public @Nullable Optional<UUID> getGuardianId() {
    return guardianId;
  }

  @JsonProperty("guardianId")
  public void setGuardianId(@Nullable Optional<UUID> guardianId) {
    this.guardianId = guardianId;
  }

  public StudentGuardianUpdate relationshipTypeId(@Nullable Optional<UUID> relationshipTypeId) {
    this.relationshipTypeId = relationshipTypeId;
    return this;
  }

  /**
   * Relationship.
   * @return relationshipTypeId
   */
  
  @JsonProperty("relationshipTypeId")
  public @Nullable Optional<UUID> getRelationshipTypeId() {
    return relationshipTypeId;
  }

  @JsonProperty("relationshipTypeId")
  public void setRelationshipTypeId(@Nullable Optional<UUID> relationshipTypeId) {
    this.relationshipTypeId = relationshipTypeId;
  }

  public StudentGuardianUpdate isPrimaryContact(@Nullable Optional<Boolean> isPrimaryContact) {
    this.isPrimaryContact = isPrimaryContact;
    return this;
  }

  /**
   * First contact for the student.
   * @return isPrimaryContact
   */
  
  @JsonProperty("isPrimaryContact")
  public @Nullable Optional<Boolean> getIsPrimaryContact() {
    return isPrimaryContact;
  }

  @JsonProperty("isPrimaryContact")
  public void setIsPrimaryContact(@Nullable Optional<Boolean> isPrimaryContact) {
    this.isPrimaryContact = isPrimaryContact;
  }

  public StudentGuardianUpdate receivesCommunication(@Nullable Optional<Boolean> receivesCommunication) {
    this.receivesCommunication = receivesCommunication;
    return this;
  }

  /**
   * Receives notices and alerts.
   * @return receivesCommunication
   */
  
  @JsonProperty("receivesCommunication")
  public @Nullable Optional<Boolean> getReceivesCommunication() {
    return receivesCommunication;
  }

  @JsonProperty("receivesCommunication")
  public void setReceivesCommunication(@Nullable Optional<Boolean> receivesCommunication) {
    this.receivesCommunication = receivesCommunication;
  }

  public StudentGuardianUpdate canPickUp(@Nullable Optional<Boolean> canPickUp) {
    this.canPickUp = canPickUp;
    return this;
  }

  /**
   * Authorised to collect the student.
   * @return canPickUp
   */
  
  @JsonProperty("canPickUp")
  public @Nullable Optional<Boolean> getCanPickUp() {
    return canPickUp;
  }

  @JsonProperty("canPickUp")
  public void setCanPickUp(@Nullable Optional<Boolean> canPickUp) {
    this.canPickUp = canPickUp;
  }

  public StudentGuardianUpdate status(@Nullable Optional<StatusEnum> status) {
    this.status = status;
    return this;
  }

  /**
   * Link state.
   * @return status
   */
  
  @JsonProperty("status")
  public @Nullable Optional<StatusEnum> getStatus() {
    return status;
  }

  @JsonProperty("status")
  public void setStatus(@Nullable Optional<StatusEnum> status) {
    this.status = status;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    StudentGuardianUpdate studentGuardianUpdate = (StudentGuardianUpdate) o;
    return Objects.equals(this.isLegalGuardian, studentGuardianUpdate.isLegalGuardian) &&
        Objects.equals(this.hasCustody, studentGuardianUpdate.hasCustody) &&
        Objects.equals(this.guardianId, studentGuardianUpdate.guardianId) &&
        Objects.equals(this.relationshipTypeId, studentGuardianUpdate.relationshipTypeId) &&
        Objects.equals(this.isPrimaryContact, studentGuardianUpdate.isPrimaryContact) &&
        Objects.equals(this.receivesCommunication, studentGuardianUpdate.receivesCommunication) &&
        Objects.equals(this.canPickUp, studentGuardianUpdate.canPickUp) &&
        Objects.equals(this.status, studentGuardianUpdate.status);
  }

  @Override
  public int hashCode() {
    return Objects.hash(isLegalGuardian, hasCustody, guardianId, relationshipTypeId, isPrimaryContact, receivesCommunication, canPickUp, status);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class StudentGuardianUpdate {\n");
    sb.append("    isLegalGuardian: ").append(toIndentedString(isLegalGuardian)).append("\n");
    sb.append("    hasCustody: ").append(toIndentedString(hasCustody)).append("\n");
    sb.append("    guardianId: ").append(toIndentedString(guardianId)).append("\n");
    sb.append("    relationshipTypeId: ").append(toIndentedString(relationshipTypeId)).append("\n");
    sb.append("    isPrimaryContact: ").append(toIndentedString(isPrimaryContact)).append("\n");
    sb.append("    receivesCommunication: ").append(toIndentedString(receivesCommunication)).append("\n");
    sb.append("    canPickUp: ").append(toIndentedString(canPickUp)).append("\n");
    sb.append("    status: ").append(toIndentedString(status)).append("\n");
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

