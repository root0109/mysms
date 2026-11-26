// Owned API contract of frontoffice.
package com.mysms.campus.frontoffice.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.lang.Nullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;

/**
 * Fields of PostalRecord that can be updated (PATCH: a property left out keeps its value, null clears it, a value sets it).
 */

public class PostalRecordUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull UUID> institutionId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<UUID> academicYearId;

  /**
   * Dispatch or receipt.
   */
  public enum DirectionEnum {
    DISPATCH("dispatch"),
    
    RECEIVE("receive");

    private final String value;

    DirectionEnum(String value) {
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
    public static DirectionEnum fromValue(String value) {
      for (DirectionEnum b : DirectionEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull DirectionEnum> direction;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull @Size(max = 60) String> postType;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@Size(max = 80) String> referenceNumber;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull Boolean> isConfidential;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@Size(max = 200) String> senderName;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<String> senderAddress;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@Size(max = 200) String> receiverName;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<String> receiverAddress;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  private @Nullable Optional<@NotNull LocalDate> postalDate;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull String> description;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<UUID> attachmentFileId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<UUID> loggedByUserId;

  public PostalRecordUpdate institutionId(@Nullable Optional<UUID> institutionId) {
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

  public PostalRecordUpdate academicYearId(@Nullable Optional<UUID> academicYearId) {
    this.academicYearId = academicYearId;
    return this;
  }

  /**
   * Academic year.
   * @return academicYearId
   */
  
  @JsonProperty("academicYearId")
  public @Nullable Optional<UUID> getAcademicYearId() {
    return academicYearId;
  }

  @JsonProperty("academicYearId")
  public void setAcademicYearId(@Nullable Optional<UUID> academicYearId) {
    this.academicYearId = academicYearId;
  }

  public PostalRecordUpdate direction(@Nullable Optional<DirectionEnum> direction) {
    this.direction = direction;
    return this;
  }

  /**
   * Dispatch or receipt.
   * @return direction
   */
  
  @JsonProperty("direction")
  public @Nullable Optional<DirectionEnum> getDirection() {
    return direction;
  }

  @JsonProperty("direction")
  public void setDirection(@Nullable Optional<DirectionEnum> direction) {
    this.direction = direction;
  }

  public PostalRecordUpdate postType(@Nullable Optional<String> postType) {
    this.postType = postType;
    return this;
  }

  /**
   * Post type (courier, registered, speed post...).
   * @return postType
   */
  
  @JsonProperty("postType")
  public @Nullable Optional<String> getPostType() {
    return postType;
  }

  @JsonProperty("postType")
  public void setPostType(@Nullable Optional<String> postType) {
    this.postType = postType;
  }

  public PostalRecordUpdate referenceNumber(@Nullable Optional<String> referenceNumber) {
    this.referenceNumber = referenceNumber;
    return this;
  }

  /**
   * Tracking / reference number.
   * @return referenceNumber
   */
  
  @JsonProperty("referenceNumber")
  public @Nullable Optional<String> getReferenceNumber() {
    return referenceNumber;
  }

  @JsonProperty("referenceNumber")
  public void setReferenceNumber(@Nullable Optional<String> referenceNumber) {
    this.referenceNumber = referenceNumber;
  }

  public PostalRecordUpdate isConfidential(@Nullable Optional<Boolean> isConfidential) {
    this.isConfidential = isConfidential;
    return this;
  }

  /**
   * Confidential.
   * @return isConfidential
   */
  
  @JsonProperty("isConfidential")
  public @Nullable Optional<Boolean> getIsConfidential() {
    return isConfidential;
  }

  @JsonProperty("isConfidential")
  public void setIsConfidential(@Nullable Optional<Boolean> isConfidential) {
    this.isConfidential = isConfidential;
  }

  public PostalRecordUpdate senderName(@Nullable Optional<String> senderName) {
    this.senderName = senderName;
    return this;
  }

  /**
   * Sender.
   * @return senderName
   */
  
  @JsonProperty("senderName")
  public @Nullable Optional<String> getSenderName() {
    return senderName;
  }

  @JsonProperty("senderName")
  public void setSenderName(@Nullable Optional<String> senderName) {
    this.senderName = senderName;
  }

  public PostalRecordUpdate senderAddress(@Nullable Optional<String> senderAddress) {
    this.senderAddress = senderAddress;
    return this;
  }

  /**
   * Sender address.
   * @return senderAddress
   */
  
  @JsonProperty("senderAddress")
  public @Nullable Optional<String> getSenderAddress() {
    return senderAddress;
  }

  @JsonProperty("senderAddress")
  public void setSenderAddress(@Nullable Optional<String> senderAddress) {
    this.senderAddress = senderAddress;
  }

  public PostalRecordUpdate receiverName(@Nullable Optional<String> receiverName) {
    this.receiverName = receiverName;
    return this;
  }

  /**
   * Receiver.
   * @return receiverName
   */
  
  @JsonProperty("receiverName")
  public @Nullable Optional<String> getReceiverName() {
    return receiverName;
  }

  @JsonProperty("receiverName")
  public void setReceiverName(@Nullable Optional<String> receiverName) {
    this.receiverName = receiverName;
  }

  public PostalRecordUpdate receiverAddress(@Nullable Optional<String> receiverAddress) {
    this.receiverAddress = receiverAddress;
    return this;
  }

  /**
   * Receiver address.
   * @return receiverAddress
   */
  
  @JsonProperty("receiverAddress")
  public @Nullable Optional<String> getReceiverAddress() {
    return receiverAddress;
  }

  @JsonProperty("receiverAddress")
  public void setReceiverAddress(@Nullable Optional<String> receiverAddress) {
    this.receiverAddress = receiverAddress;
  }

  public PostalRecordUpdate postalDate(@Nullable Optional<LocalDate> postalDate) {
    this.postalDate = postalDate;
    return this;
  }

  /**
   * Date.
   * @return postalDate
   */
  
  @JsonProperty("postalDate")
  public @Nullable Optional<LocalDate> getPostalDate() {
    return postalDate;
  }

  @JsonProperty("postalDate")
  public void setPostalDate(@Nullable Optional<LocalDate> postalDate) {
    this.postalDate = postalDate;
  }

  public PostalRecordUpdate description(@Nullable Optional<String> description) {
    this.description = description;
    return this;
  }

  /**
   * Contents / description.
   * @return description
   */
  
  @JsonProperty("description")
  public @Nullable Optional<String> getDescription() {
    return description;
  }

  @JsonProperty("description")
  public void setDescription(@Nullable Optional<String> description) {
    this.description = description;
  }

  public PostalRecordUpdate attachmentFileId(@Nullable Optional<UUID> attachmentFileId) {
    this.attachmentFileId = attachmentFileId;
    return this;
  }

  /**
   * Scan.
   * @return attachmentFileId
   */
  
  @JsonProperty("attachmentFileId")
  public @Nullable Optional<UUID> getAttachmentFileId() {
    return attachmentFileId;
  }

  @JsonProperty("attachmentFileId")
  public void setAttachmentFileId(@Nullable Optional<UUID> attachmentFileId) {
    this.attachmentFileId = attachmentFileId;
  }

  public PostalRecordUpdate loggedByUserId(@Nullable Optional<UUID> loggedByUserId) {
    this.loggedByUserId = loggedByUserId;
    return this;
  }

  /**
   * Staff who logged it.
   * @return loggedByUserId
   */
  
  @JsonProperty("loggedByUserId")
  public @Nullable Optional<UUID> getLoggedByUserId() {
    return loggedByUserId;
  }

  @JsonProperty("loggedByUserId")
  public void setLoggedByUserId(@Nullable Optional<UUID> loggedByUserId) {
    this.loggedByUserId = loggedByUserId;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    PostalRecordUpdate postalRecordUpdate = (PostalRecordUpdate) o;
    return Objects.equals(this.institutionId, postalRecordUpdate.institutionId) &&
        Objects.equals(this.academicYearId, postalRecordUpdate.academicYearId) &&
        Objects.equals(this.direction, postalRecordUpdate.direction) &&
        Objects.equals(this.postType, postalRecordUpdate.postType) &&
        Objects.equals(this.referenceNumber, postalRecordUpdate.referenceNumber) &&
        Objects.equals(this.isConfidential, postalRecordUpdate.isConfidential) &&
        Objects.equals(this.senderName, postalRecordUpdate.senderName) &&
        Objects.equals(this.senderAddress, postalRecordUpdate.senderAddress) &&
        Objects.equals(this.receiverName, postalRecordUpdate.receiverName) &&
        Objects.equals(this.receiverAddress, postalRecordUpdate.receiverAddress) &&
        Objects.equals(this.postalDate, postalRecordUpdate.postalDate) &&
        Objects.equals(this.description, postalRecordUpdate.description) &&
        Objects.equals(this.attachmentFileId, postalRecordUpdate.attachmentFileId) &&
        Objects.equals(this.loggedByUserId, postalRecordUpdate.loggedByUserId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(institutionId, academicYearId, direction, postType, referenceNumber, isConfidential, senderName, senderAddress, receiverName, receiverAddress, postalDate, description, attachmentFileId, loggedByUserId);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class PostalRecordUpdate {\n");
    sb.append("    institutionId: ").append(toIndentedString(institutionId)).append("\n");
    sb.append("    academicYearId: ").append(toIndentedString(academicYearId)).append("\n");
    sb.append("    direction: ").append(toIndentedString(direction)).append("\n");
    sb.append("    postType: ").append(toIndentedString(postType)).append("\n");
    sb.append("    referenceNumber: ").append(toIndentedString(referenceNumber)).append("\n");
    sb.append("    isConfidential: ").append(toIndentedString(isConfidential)).append("\n");
    sb.append("    senderName: ").append(toIndentedString(senderName)).append("\n");
    sb.append("    senderAddress: ").append(toIndentedString(senderAddress)).append("\n");
    sb.append("    receiverName: ").append(toIndentedString(receiverName)).append("\n");
    sb.append("    receiverAddress: ").append(toIndentedString(receiverAddress)).append("\n");
    sb.append("    postalDate: ").append(toIndentedString(postalDate)).append("\n");
    sb.append("    description: ").append(toIndentedString(description)).append("\n");
    sb.append("    attachmentFileId: ").append(toIndentedString(attachmentFileId)).append("\n");
    sb.append("    loggedByUserId: ").append(toIndentedString(loggedByUserId)).append("\n");
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

