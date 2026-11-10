// Owned API contract of comms (JAVA-CODING-RULES §7); first materialized from api/openapi/comms.yaml on 2026-11-10.
package com.mysms.comms.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.lang.Nullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;

/**
 * Notification shown in a user&#39;s in-app inbox.
 */

public class InAppNotification {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String entityType;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID entityId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID id;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private UUID recipientUserId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private @Nullable OffsetDateTime readAt;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private @Nullable OffsetDateTime createTime;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID notificationTypeId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String title;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String body;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String smsText;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String actionUrl;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID cohortId;

  /**
   * E-mail delivery state.
   */
  public enum EmailStatusEnum {
    NOT_REQUIRED("not_required"),
    
    PENDING("pending"),
    
    SENT("sent"),
    
    FAILED("failed");

    private final String value;

    EmailStatusEnum(String value) {
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
    public static EmailStatusEnum fromValue(String value) {
      for (EmailStatusEnum b : EmailStatusEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable EmailStatusEnum emailStatus;

  /**
   * SMS delivery state.
   */
  public enum SmsStatusEnum {
    NOT_REQUIRED("not_required"),
    
    PENDING("pending"),
    
    SENT("sent"),
    
    FAILED("failed");

    private final String value;

    SmsStatusEnum(String value) {
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
    public static SmsStatusEnum fromValue(String value) {
      for (SmsStatusEnum b : SmsStatusEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable SmsStatusEnum smsStatus;

  /**
   * Popup display state.
   */
  public enum PopupStatusEnum {
    NOT_REQUIRED("not_required"),
    
    PENDING("pending"),
    
    SHOWN("shown");

    private final String value;

    PopupStatusEnum(String value) {
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
    public static PopupStatusEnum fromValue(String value) {
      for (PopupStatusEnum b : PopupStatusEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable PopupStatusEnum popupStatus;

  /**
   * Daily-digest inclusion state.
   */
  public enum DigestStatusEnum {
    NOT_REQUIRED("not_required"),
    
    PENDING("pending"),
    
    SENT("sent");

    private final String value;

    DigestStatusEnum(String value) {
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
    public static DigestStatusEnum fromValue(String value) {
      for (DigestStatusEnum b : DigestStatusEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable DigestStatusEnum digestStatus;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private @Nullable OffsetDateTime archivedAt;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String etag;

  public InAppNotification() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public InAppNotification(UUID recipientUserId) {
    this.recipientUserId = recipientUserId;
  }

  public InAppNotification entityType(@Nullable String entityType) {
    this.entityType = entityType;
    return this;
  }

  /**
   * Kind of record the notification is about (deep-link target).
   * @return entityType
   */
  
  @JsonProperty("entityType")
  public @Nullable String getEntityType() {
    return entityType;
  }

  @JsonProperty("entityType")
  public void setEntityType(@Nullable String entityType) {
    this.entityType = entityType;
  }

  public InAppNotification entityId(@Nullable UUID entityId) {
    this.entityId = entityId;
    return this;
  }

  /**
   * Id of that record.
   * @return entityId
   */
  @Valid 
  @JsonProperty("entityId")
  public @Nullable UUID getEntityId() {
    return entityId;
  }

  @JsonProperty("entityId")
  public void setEntityId(@Nullable UUID entityId) {
    this.entityId = entityId;
  }

  public InAppNotification id(@Nullable UUID id) {
    this.id = id;
    return this;
  }

  /**
   * Surrogate primary key (UUIDv7 generated by the application; database default is a v4 fallback).
   * @return id
   */
  @Valid 
  @JsonProperty("id")
  public @Nullable UUID getId() {
    return id;
  }

  @JsonProperty("id")
  public void setId(@Nullable UUID id) {
    this.id = id;
  }

  public InAppNotification recipientUserId(UUID recipientUserId) {
    this.recipientUserId = recipientUserId;
    return this;
  }

  /**
   * Recipient.
   * @return recipientUserId
   */
  @NotNull @Valid 
  @JsonProperty("recipientUserId")
  public UUID getRecipientUserId() {
    return recipientUserId;
  }

  @JsonProperty("recipientUserId")
  public void setRecipientUserId(UUID recipientUserId) {
    this.recipientUserId = recipientUserId;
  }

  public InAppNotification readAt(@Nullable OffsetDateTime readAt) {
    this.readAt = readAt;
    return this;
  }

  /**
   * When read.
   * @return readAt
   */
  @Valid 
  @JsonProperty("readAt")
  public @Nullable OffsetDateTime getReadAt() {
    return readAt;
  }

  @JsonProperty("readAt")
  public void setReadAt(@Nullable OffsetDateTime readAt) {
    this.readAt = readAt;
  }

  public InAppNotification createTime(@Nullable OffsetDateTime createTime) {
    this.createTime = createTime;
    return this;
  }

  /**
   * When created.
   * @return createTime
   */
  @Valid 
  @JsonProperty("createTime")
  public @Nullable OffsetDateTime getCreateTime() {
    return createTime;
  }

  @JsonProperty("createTime")
  public void setCreateTime(@Nullable OffsetDateTime createTime) {
    this.createTime = createTime;
  }

  public InAppNotification notificationTypeId(@Nullable UUID notificationTypeId) {
    this.notificationTypeId = notificationTypeId;
    return this;
  }

  /**
   * Notification type (catalogue entry).
   * @return notificationTypeId
   */
  @Valid 
  @JsonProperty("notificationTypeId")
  public @Nullable UUID getNotificationTypeId() {
    return notificationTypeId;
  }

  @JsonProperty("notificationTypeId")
  public void setNotificationTypeId(@Nullable UUID notificationTypeId) {
    this.notificationTypeId = notificationTypeId;
  }

  public InAppNotification title(@Nullable String title) {
    this.title = title;
    return this;
  }

  /**
   * Title.
   * @return title
   */
  @Size(max = 255) 
  @JsonProperty("title")
  public @Nullable String getTitle() {
    return title;
  }

  @JsonProperty("title")
  public void setTitle(@Nullable String title) {
    this.title = title;
  }

  public InAppNotification body(@Nullable String body) {
    this.body = body;
    return this;
  }

  /**
   * Message.
   * @return body
   */
  
  @JsonProperty("body")
  public @Nullable String getBody() {
    return body;
  }

  @JsonProperty("body")
  public void setBody(@Nullable String body) {
    this.body = body;
  }

  public InAppNotification smsText(@Nullable String smsText) {
    this.smsText = smsText;
    return this;
  }

  /**
   * Short SMS rendering.
   * @return smsText
   */
  @Size(max = 480) 
  @JsonProperty("smsText")
  public @Nullable String getSmsText() {
    return smsText;
  }

  @JsonProperty("smsText")
  public void setSmsText(@Nullable String smsText) {
    this.smsText = smsText;
  }

  public InAppNotification actionUrl(@Nullable String actionUrl) {
    this.actionUrl = actionUrl;
    return this;
  }

  /**
   * Deep link opened when the notification is tapped: mysms://<module>/<resource-path> (e.g. mysms://lms/cohorts/{id}/gradebook); the web app and universal / app links resolve the same path under https://<tenant host>/app/.
   * @return actionUrl
   */
  @Size(max = 1000) 
  @JsonProperty("actionUrl")
  public @Nullable String getActionUrl() {
    return actionUrl;
  }

  @JsonProperty("actionUrl")
  public void setActionUrl(@Nullable String actionUrl) {
    this.actionUrl = actionUrl;
  }

  public InAppNotification cohortId(@Nullable UUID cohortId) {
    this.cohortId = cohortId;
    return this;
  }

  /**
   * Cohort context.
   * @return cohortId
   */
  @Valid 
  @JsonProperty("cohortId")
  public @Nullable UUID getCohortId() {
    return cohortId;
  }

  @JsonProperty("cohortId")
  public void setCohortId(@Nullable UUID cohortId) {
    this.cohortId = cohortId;
  }

  public InAppNotification emailStatus(@Nullable EmailStatusEnum emailStatus) {
    this.emailStatus = emailStatus;
    return this;
  }

  /**
   * E-mail delivery state.
   * @return emailStatus
   */
  
  @JsonProperty("emailStatus")
  public @Nullable EmailStatusEnum getEmailStatus() {
    return emailStatus;
  }

  @JsonProperty("emailStatus")
  public void setEmailStatus(@Nullable EmailStatusEnum emailStatus) {
    this.emailStatus = emailStatus;
  }

  public InAppNotification smsStatus(@Nullable SmsStatusEnum smsStatus) {
    this.smsStatus = smsStatus;
    return this;
  }

  /**
   * SMS delivery state.
   * @return smsStatus
   */
  
  @JsonProperty("smsStatus")
  public @Nullable SmsStatusEnum getSmsStatus() {
    return smsStatus;
  }

  @JsonProperty("smsStatus")
  public void setSmsStatus(@Nullable SmsStatusEnum smsStatus) {
    this.smsStatus = smsStatus;
  }

  public InAppNotification popupStatus(@Nullable PopupStatusEnum popupStatus) {
    this.popupStatus = popupStatus;
    return this;
  }

  /**
   * Popup display state.
   * @return popupStatus
   */
  
  @JsonProperty("popupStatus")
  public @Nullable PopupStatusEnum getPopupStatus() {
    return popupStatus;
  }

  @JsonProperty("popupStatus")
  public void setPopupStatus(@Nullable PopupStatusEnum popupStatus) {
    this.popupStatus = popupStatus;
  }

  public InAppNotification digestStatus(@Nullable DigestStatusEnum digestStatus) {
    this.digestStatus = digestStatus;
    return this;
  }

  /**
   * Daily-digest inclusion state.
   * @return digestStatus
   */
  
  @JsonProperty("digestStatus")
  public @Nullable DigestStatusEnum getDigestStatus() {
    return digestStatus;
  }

  @JsonProperty("digestStatus")
  public void setDigestStatus(@Nullable DigestStatusEnum digestStatus) {
    this.digestStatus = digestStatus;
  }

  public InAppNotification archivedAt(@Nullable OffsetDateTime archivedAt) {
    this.archivedAt = archivedAt;
    return this;
  }

  /**
   * Hidden from the recipient's inbox (kept for history).
   * @return archivedAt
   */
  @Valid 
  @JsonProperty("archivedAt")
  public @Nullable OffsetDateTime getArchivedAt() {
    return archivedAt;
  }

  @JsonProperty("archivedAt")
  public void setArchivedAt(@Nullable OffsetDateTime archivedAt) {
    this.archivedAt = archivedAt;
  }

  public InAppNotification etag(@Nullable String etag) {
    this.etag = etag;
    return this;
  }

  /**
   * Entity tag (row version); send back in If-Match / etag.
   * @return etag
   */
  
  @JsonProperty("etag")
  public @Nullable String getEtag() {
    return etag;
  }

  @JsonProperty("etag")
  public void setEtag(@Nullable String etag) {
    this.etag = etag;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    InAppNotification inAppNotification = (InAppNotification) o;
    return Objects.equals(this.entityType, inAppNotification.entityType) &&
        Objects.equals(this.entityId, inAppNotification.entityId) &&
        Objects.equals(this.id, inAppNotification.id) &&
        Objects.equals(this.recipientUserId, inAppNotification.recipientUserId) &&
        Objects.equals(this.readAt, inAppNotification.readAt) &&
        Objects.equals(this.createTime, inAppNotification.createTime) &&
        Objects.equals(this.notificationTypeId, inAppNotification.notificationTypeId) &&
        Objects.equals(this.title, inAppNotification.title) &&
        Objects.equals(this.body, inAppNotification.body) &&
        Objects.equals(this.smsText, inAppNotification.smsText) &&
        Objects.equals(this.actionUrl, inAppNotification.actionUrl) &&
        Objects.equals(this.cohortId, inAppNotification.cohortId) &&
        Objects.equals(this.emailStatus, inAppNotification.emailStatus) &&
        Objects.equals(this.smsStatus, inAppNotification.smsStatus) &&
        Objects.equals(this.popupStatus, inAppNotification.popupStatus) &&
        Objects.equals(this.digestStatus, inAppNotification.digestStatus) &&
        Objects.equals(this.archivedAt, inAppNotification.archivedAt) &&
        Objects.equals(this.etag, inAppNotification.etag);
  }

  @Override
  public int hashCode() {
    return Objects.hash(entityType, entityId, id, recipientUserId, readAt, createTime, notificationTypeId, title, body, smsText, actionUrl, cohortId, emailStatus, smsStatus, popupStatus, digestStatus, archivedAt, etag);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class InAppNotification {\n");
    sb.append("    entityType: ").append(toIndentedString(entityType)).append("\n");
    sb.append("    entityId: ").append(toIndentedString(entityId)).append("\n");
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    recipientUserId: ").append(toIndentedString(recipientUserId)).append("\n");
    sb.append("    readAt: ").append(toIndentedString(readAt)).append("\n");
    sb.append("    createTime: ").append(toIndentedString(createTime)).append("\n");
    sb.append("    notificationTypeId: ").append(toIndentedString(notificationTypeId)).append("\n");
    sb.append("    title: ").append(toIndentedString(title)).append("\n");
    sb.append("    body: ").append(toIndentedString(body)).append("\n");
    sb.append("    smsText: ").append(toIndentedString(smsText)).append("\n");
    sb.append("    actionUrl: ").append(toIndentedString(actionUrl)).append("\n");
    sb.append("    cohortId: ").append(toIndentedString(cohortId)).append("\n");
    sb.append("    emailStatus: ").append(toIndentedString(emailStatus)).append("\n");
    sb.append("    smsStatus: ").append(toIndentedString(smsStatus)).append("\n");
    sb.append("    popupStatus: ").append(toIndentedString(popupStatus)).append("\n");
    sb.append("    digestStatus: ").append(toIndentedString(digestStatus)).append("\n");
    sb.append("    archivedAt: ").append(toIndentedString(archivedAt)).append("\n");
    sb.append("    etag: ").append(toIndentedString(etag)).append("\n");
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

