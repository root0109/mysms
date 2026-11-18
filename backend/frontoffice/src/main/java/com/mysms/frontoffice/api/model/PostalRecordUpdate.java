// Owned API contract of frontoffice.
package com.mysms.frontoffice.api.model;

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
 * Fields of PostalRecord that can be updated (partial update; see updateMask).
 */

public class PostalRecordUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID institutionId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID academicYearId;

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
  private @Nullable DirectionEnum direction;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String postType;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String referenceNumber;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Boolean isConfidential;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String senderName;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String senderAddress;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String receiverName;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String receiverAddress;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  private @Nullable LocalDate postalDate;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String description;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID attachmentFileId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID loggedByUserId;

  public PostalRecordUpdate institutionId(@Nullable UUID institutionId) {
    this.institutionId = institutionId;
    return this;
  }

  /**
   * Institution.
   * @return institutionId
   */
  @Valid 
  @JsonProperty("institutionId")
  public @Nullable UUID getInstitutionId() {
    return institutionId;
  }

  @JsonProperty("institutionId")
  public void setInstitutionId(@Nullable UUID institutionId) {
    this.institutionId = institutionId;
  }

  public PostalRecordUpdate academicYearId(@Nullable UUID academicYearId) {
    this.academicYearId = academicYearId;
    return this;
  }

  /**
   * Academic year.
   * @return academicYearId
   */
  @Valid 
  @JsonProperty("academicYearId")
  public @Nullable UUID getAcademicYearId() {
    return academicYearId;
  }

  @JsonProperty("academicYearId")
  public void setAcademicYearId(@Nullable UUID academicYearId) {
    this.academicYearId = academicYearId;
  }

  public PostalRecordUpdate direction(@Nullable DirectionEnum direction) {
    this.direction = direction;
    return this;
  }

  /**
   * Dispatch or receipt.
   * @return direction
   */
  
  @JsonProperty("direction")
  public @Nullable DirectionEnum getDirection() {
    return direction;
  }

  @JsonProperty("direction")
  public void setDirection(@Nullable DirectionEnum direction) {
    this.direction = direction;
  }

  public PostalRecordUpdate postType(@Nullable String postType) {
    this.postType = postType;
    return this;
  }

  /**
   * Post type (courier, registered, speed post...).
   * @return postType
   */
  @Size(max = 60) 
  @JsonProperty("postType")
  public @Nullable String getPostType() {
    return postType;
  }

  @JsonProperty("postType")
  public void setPostType(@Nullable String postType) {
    this.postType = postType;
  }

  public PostalRecordUpdate referenceNumber(@Nullable String referenceNumber) {
    this.referenceNumber = referenceNumber;
    return this;
  }

  /**
   * Tracking / reference number.
   * @return referenceNumber
   */
  @Size(max = 80) 
  @JsonProperty("referenceNumber")
  public @Nullable String getReferenceNumber() {
    return referenceNumber;
  }

  @JsonProperty("referenceNumber")
  public void setReferenceNumber(@Nullable String referenceNumber) {
    this.referenceNumber = referenceNumber;
  }

  public PostalRecordUpdate isConfidential(@Nullable Boolean isConfidential) {
    this.isConfidential = isConfidential;
    return this;
  }

  /**
   * Confidential.
   * @return isConfidential
   */
  
  @JsonProperty("isConfidential")
  public @Nullable Boolean getIsConfidential() {
    return isConfidential;
  }

  @JsonProperty("isConfidential")
  public void setIsConfidential(@Nullable Boolean isConfidential) {
    this.isConfidential = isConfidential;
  }

  public PostalRecordUpdate senderName(@Nullable String senderName) {
    this.senderName = senderName;
    return this;
  }

  /**
   * Sender.
   * @return senderName
   */
  @Size(max = 200) 
  @JsonProperty("senderName")
  public @Nullable String getSenderName() {
    return senderName;
  }

  @JsonProperty("senderName")
  public void setSenderName(@Nullable String senderName) {
    this.senderName = senderName;
  }

  public PostalRecordUpdate senderAddress(@Nullable String senderAddress) {
    this.senderAddress = senderAddress;
    return this;
  }

  /**
   * Sender address.
   * @return senderAddress
   */
  
  @JsonProperty("senderAddress")
  public @Nullable String getSenderAddress() {
    return senderAddress;
  }

  @JsonProperty("senderAddress")
  public void setSenderAddress(@Nullable String senderAddress) {
    this.senderAddress = senderAddress;
  }

  public PostalRecordUpdate receiverName(@Nullable String receiverName) {
    this.receiverName = receiverName;
    return this;
  }

  /**
   * Receiver.
   * @return receiverName
   */
  @Size(max = 200) 
  @JsonProperty("receiverName")
  public @Nullable String getReceiverName() {
    return receiverName;
  }

  @JsonProperty("receiverName")
  public void setReceiverName(@Nullable String receiverName) {
    this.receiverName = receiverName;
  }

  public PostalRecordUpdate receiverAddress(@Nullable String receiverAddress) {
    this.receiverAddress = receiverAddress;
    return this;
  }

  /**
   * Receiver address.
   * @return receiverAddress
   */
  
  @JsonProperty("receiverAddress")
  public @Nullable String getReceiverAddress() {
    return receiverAddress;
  }

  @JsonProperty("receiverAddress")
  public void setReceiverAddress(@Nullable String receiverAddress) {
    this.receiverAddress = receiverAddress;
  }

  public PostalRecordUpdate postalDate(@Nullable LocalDate postalDate) {
    this.postalDate = postalDate;
    return this;
  }

  /**
   * Date.
   * @return postalDate
   */
  @Valid 
  @JsonProperty("postalDate")
  public @Nullable LocalDate getPostalDate() {
    return postalDate;
  }

  @JsonProperty("postalDate")
  public void setPostalDate(@Nullable LocalDate postalDate) {
    this.postalDate = postalDate;
  }

  public PostalRecordUpdate description(@Nullable String description) {
    this.description = description;
    return this;
  }

  /**
   * Contents / description.
   * @return description
   */
  
  @JsonProperty("description")
  public @Nullable String getDescription() {
    return description;
  }

  @JsonProperty("description")
  public void setDescription(@Nullable String description) {
    this.description = description;
  }

  public PostalRecordUpdate attachmentFileId(@Nullable UUID attachmentFileId) {
    this.attachmentFileId = attachmentFileId;
    return this;
  }

  /**
   * Scan.
   * @return attachmentFileId
   */
  @Valid 
  @JsonProperty("attachmentFileId")
  public @Nullable UUID getAttachmentFileId() {
    return attachmentFileId;
  }

  @JsonProperty("attachmentFileId")
  public void setAttachmentFileId(@Nullable UUID attachmentFileId) {
    this.attachmentFileId = attachmentFileId;
  }

  public PostalRecordUpdate loggedByUserId(@Nullable UUID loggedByUserId) {
    this.loggedByUserId = loggedByUserId;
    return this;
  }

  /**
   * Staff who logged it.
   * @return loggedByUserId
   */
  @Valid 
  @JsonProperty("loggedByUserId")
  public @Nullable UUID getLoggedByUserId() {
    return loggedByUserId;
  }

  @JsonProperty("loggedByUserId")
  public void setLoggedByUserId(@Nullable UUID loggedByUserId) {
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

