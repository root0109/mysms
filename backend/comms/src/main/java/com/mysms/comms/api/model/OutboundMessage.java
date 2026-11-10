// Owned API contract of comms (JAVA-CODING-RULES §7); first materialized from api/openapi/comms.yaml on 2026-11-10.
package com.mysms.comms.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.lang.Nullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;

/**
 * Queued SMS / e-mail / push message (notifications, reminders, broadcasts) and its delivery state.
 */

public class OutboundMessage {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private List<String> templateParams = new ArrayList<>();

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String actionUrl;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID id;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private UUID institutionId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID academicYearId;

  /**
   * Kind.
   */
  public enum MessageKindEnum {
    NOTIFICATION("notification"),
    
    REMINDER("reminder"),
    
    BROADCAST("broadcast");

    private final String value;

    MessageKindEnum(String value) {
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
    public static MessageKindEnum fromValue(String value) {
      for (MessageKindEnum b : MessageKindEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private MessageKindEnum messageKind;

  /**
   * Channel.
   */
  public enum ChannelEnum {
    SMS("sms"),
    
    EMAIL("email"),
    
    PUSH("push"),
    
    WEB_PUSH("web_push");

    private final String value;

    ChannelEnum(String value) {
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
    public static ChannelEnum fromValue(String value) {
      for (ChannelEnum b : ChannelEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private ChannelEnum channel;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private String sender;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private String recipient;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID recipientUserId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String subject;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private String body;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID templateId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String relatedEntityType;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID relatedEntityId;

  /**
   * Delivery state. Changed only through the workflow operations.
   */
  public enum StatusEnum {
    QUEUED("queued"),
    
    PROCESSING("processing"),
    
    DELIVERED("delivered"),
    
    FAILED("failed"),
    
    CANCELLED("cancelled");

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
  private @Nullable StatusEnum status;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String providerResponse;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private @Nullable OffsetDateTime scheduledAt;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private @Nullable OffsetDateTime sentAt;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private @Nullable OffsetDateTime createTime;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID createdBy;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private @Nullable OffsetDateTime updateTime;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID updatedBy;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String etag;

  public OutboundMessage() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public OutboundMessage(UUID institutionId, MessageKindEnum messageKind, ChannelEnum channel, String sender, String recipient, String body) {
    this.institutionId = institutionId;
    this.messageKind = messageKind;
    this.channel = channel;
    this.sender = sender;
    this.recipient = recipient;
    this.body = body;
  }

  public OutboundMessage templateParams(List<String> templateParams) {
    this.templateParams = templateParams;
    return this;
  }

  public OutboundMessage addTemplateParamsItem(String templateParamsItem) {
    if (this.templateParams == null) {
      this.templateParams = new ArrayList<>();
    }
    this.templateParams.add(templateParamsItem);
    return this;
  }

  /**
   * Ordered values for provider-side SMS/WhatsApp templates.
   * @return templateParams
   */
  
  @JsonProperty("templateParams")
  public List<String> getTemplateParams() {
    return templateParams;
  }

  @JsonProperty("templateParams")
  public void setTemplateParams(List<String> templateParams) {
    this.templateParams = templateParams;
  }

  public OutboundMessage actionUrl(@Nullable String actionUrl) {
    this.actionUrl = actionUrl;
    return this;
  }

  /**
   * Deep link the message points at.
   * @return actionUrl
   */
  
  @JsonProperty("actionUrl")
  public @Nullable String getActionUrl() {
    return actionUrl;
  }

  @JsonProperty("actionUrl")
  public void setActionUrl(@Nullable String actionUrl) {
    this.actionUrl = actionUrl;
  }

  public OutboundMessage id(@Nullable UUID id) {
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

  public OutboundMessage institutionId(UUID institutionId) {
    this.institutionId = institutionId;
    return this;
  }

  /**
   * Institution.
   * @return institutionId
   */
  @NotNull @Valid 
  @JsonProperty("institutionId")
  public UUID getInstitutionId() {
    return institutionId;
  }

  @JsonProperty("institutionId")
  public void setInstitutionId(UUID institutionId) {
    this.institutionId = institutionId;
  }

  public OutboundMessage academicYearId(@Nullable UUID academicYearId) {
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

  public OutboundMessage messageKind(MessageKindEnum messageKind) {
    this.messageKind = messageKind;
    return this;
  }

  /**
   * Kind.
   * @return messageKind
   */
  @NotNull 
  @JsonProperty("messageKind")
  public MessageKindEnum getMessageKind() {
    return messageKind;
  }

  @JsonProperty("messageKind")
  public void setMessageKind(MessageKindEnum messageKind) {
    this.messageKind = messageKind;
  }

  public OutboundMessage channel(ChannelEnum channel) {
    this.channel = channel;
    return this;
  }

  /**
   * Channel.
   * @return channel
   */
  @NotNull 
  @JsonProperty("channel")
  public ChannelEnum getChannel() {
    return channel;
  }

  @JsonProperty("channel")
  public void setChannel(ChannelEnum channel) {
    this.channel = channel;
  }

  public OutboundMessage sender(String sender) {
    this.sender = sender;
    return this;
  }

  /**
   * Sender address / id.
   * @return sender
   */
  @NotNull @Size(max = 320) 
  @JsonProperty("sender")
  public String getSender() {
    return sender;
  }

  @JsonProperty("sender")
  public void setSender(String sender) {
    this.sender = sender;
  }

  public OutboundMessage recipient(String recipient) {
    this.recipient = recipient;
    return this;
  }

  /**
   * Recipient address / number.
   * @return recipient
   */
  @NotNull @Size(max = 320) 
  @JsonProperty("recipient")
  public String getRecipient() {
    return recipient;
  }

  @JsonProperty("recipient")
  public void setRecipient(String recipient) {
    this.recipient = recipient;
  }

  public OutboundMessage recipientUserId(@Nullable UUID recipientUserId) {
    this.recipientUserId = recipientUserId;
    return this;
  }

  /**
   * Recipient account when known.
   * @return recipientUserId
   */
  @Valid 
  @JsonProperty("recipientUserId")
  public @Nullable UUID getRecipientUserId() {
    return recipientUserId;
  }

  @JsonProperty("recipientUserId")
  public void setRecipientUserId(@Nullable UUID recipientUserId) {
    this.recipientUserId = recipientUserId;
  }

  public OutboundMessage subject(@Nullable String subject) {
    this.subject = subject;
    return this;
  }

  /**
   * Subject.
   * @return subject
   */
  
  @JsonProperty("subject")
  public @Nullable String getSubject() {
    return subject;
  }

  @JsonProperty("subject")
  public void setSubject(@Nullable String subject) {
    this.subject = subject;
  }

  public OutboundMessage body(String body) {
    this.body = body;
    return this;
  }

  /**
   * Message body.
   * @return body
   */
  @NotNull 
  @JsonProperty("body")
  public String getBody() {
    return body;
  }

  @JsonProperty("body")
  public void setBody(String body) {
    this.body = body;
  }

  public OutboundMessage templateId(@Nullable UUID templateId) {
    this.templateId = templateId;
    return this;
  }

  /**
   * Template used.
   * @return templateId
   */
  @Valid 
  @JsonProperty("templateId")
  public @Nullable UUID getTemplateId() {
    return templateId;
  }

  @JsonProperty("templateId")
  public void setTemplateId(@Nullable UUID templateId) {
    this.templateId = templateId;
  }

  public OutboundMessage relatedEntityType(@Nullable String relatedEntityType) {
    this.relatedEntityType = relatedEntityType;
    return this;
  }

  /**
   * Polymorphic table name of the record the message is about.
   * @return relatedEntityType
   */
  @Size(max = 80) 
  @JsonProperty("relatedEntityType")
  public @Nullable String getRelatedEntityType() {
    return relatedEntityType;
  }

  @JsonProperty("relatedEntityType")
  public void setRelatedEntityType(@Nullable String relatedEntityType) {
    this.relatedEntityType = relatedEntityType;
  }

  public OutboundMessage relatedEntityId(@Nullable UUID relatedEntityId) {
    this.relatedEntityId = relatedEntityId;
    return this;
  }

  /**
   * Polymorphic id of the record the message is about.
   * @return relatedEntityId
   */
  @Valid 
  @JsonProperty("relatedEntityId")
  public @Nullable UUID getRelatedEntityId() {
    return relatedEntityId;
  }

  @JsonProperty("relatedEntityId")
  public void setRelatedEntityId(@Nullable UUID relatedEntityId) {
    this.relatedEntityId = relatedEntityId;
  }

  public OutboundMessage status(@Nullable StatusEnum status) {
    this.status = status;
    return this;
  }

  /**
   * Delivery state. Changed only through the workflow operations.
   * @return status
   */
  
  @JsonProperty("status")
  public @Nullable StatusEnum getStatus() {
    return status;
  }

  @JsonProperty("status")
  public void setStatus(@Nullable StatusEnum status) {
    this.status = status;
  }

  public OutboundMessage providerResponse(@Nullable String providerResponse) {
    this.providerResponse = providerResponse;
    return this;
  }

  /**
   * Gateway response.
   * @return providerResponse
   */
  
  @JsonProperty("providerResponse")
  public @Nullable String getProviderResponse() {
    return providerResponse;
  }

  @JsonProperty("providerResponse")
  public void setProviderResponse(@Nullable String providerResponse) {
    this.providerResponse = providerResponse;
  }

  public OutboundMessage scheduledAt(@Nullable OffsetDateTime scheduledAt) {
    this.scheduledAt = scheduledAt;
    return this;
  }

  /**
   * Send no earlier than.
   * @return scheduledAt
   */
  @Valid 
  @JsonProperty("scheduledAt")
  public @Nullable OffsetDateTime getScheduledAt() {
    return scheduledAt;
  }

  @JsonProperty("scheduledAt")
  public void setScheduledAt(@Nullable OffsetDateTime scheduledAt) {
    this.scheduledAt = scheduledAt;
  }

  public OutboundMessage sentAt(@Nullable OffsetDateTime sentAt) {
    this.sentAt = sentAt;
    return this;
  }

  /**
   * When sent.
   * @return sentAt
   */
  @Valid 
  @JsonProperty("sentAt")
  public @Nullable OffsetDateTime getSentAt() {
    return sentAt;
  }

  @JsonProperty("sentAt")
  public void setSentAt(@Nullable OffsetDateTime sentAt) {
    this.sentAt = sentAt;
  }

  public OutboundMessage createTime(@Nullable OffsetDateTime createTime) {
    this.createTime = createTime;
    return this;
  }

  /**
   * Row creation time.
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

  public OutboundMessage createdBy(@Nullable UUID createdBy) {
    this.createdBy = createdBy;
    return this;
  }

  /**
   * User account that created the row (audit reference to iam.user_account; not FK-constrained).
   * @return createdBy
   */
  @Valid 
  @JsonProperty("createdBy")
  public @Nullable UUID getCreatedBy() {
    return createdBy;
  }

  @JsonProperty("createdBy")
  public void setCreatedBy(@Nullable UUID createdBy) {
    this.createdBy = createdBy;
  }

  public OutboundMessage updateTime(@Nullable OffsetDateTime updateTime) {
    this.updateTime = updateTime;
    return this;
  }

  /**
   * Last update time.
   * @return updateTime
   */
  @Valid 
  @JsonProperty("updateTime")
  public @Nullable OffsetDateTime getUpdateTime() {
    return updateTime;
  }

  @JsonProperty("updateTime")
  public void setUpdateTime(@Nullable OffsetDateTime updateTime) {
    this.updateTime = updateTime;
  }

  public OutboundMessage updatedBy(@Nullable UUID updatedBy) {
    this.updatedBy = updatedBy;
    return this;
  }

  /**
   * User account that last updated the row (audit reference to iam.user_account; not FK-constrained).
   * @return updatedBy
   */
  @Valid 
  @JsonProperty("updatedBy")
  public @Nullable UUID getUpdatedBy() {
    return updatedBy;
  }

  @JsonProperty("updatedBy")
  public void setUpdatedBy(@Nullable UUID updatedBy) {
    this.updatedBy = updatedBy;
  }

  public OutboundMessage etag(@Nullable String etag) {
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
    OutboundMessage outboundMessage = (OutboundMessage) o;
    return Objects.equals(this.templateParams, outboundMessage.templateParams) &&
        Objects.equals(this.actionUrl, outboundMessage.actionUrl) &&
        Objects.equals(this.id, outboundMessage.id) &&
        Objects.equals(this.institutionId, outboundMessage.institutionId) &&
        Objects.equals(this.academicYearId, outboundMessage.academicYearId) &&
        Objects.equals(this.messageKind, outboundMessage.messageKind) &&
        Objects.equals(this.channel, outboundMessage.channel) &&
        Objects.equals(this.sender, outboundMessage.sender) &&
        Objects.equals(this.recipient, outboundMessage.recipient) &&
        Objects.equals(this.recipientUserId, outboundMessage.recipientUserId) &&
        Objects.equals(this.subject, outboundMessage.subject) &&
        Objects.equals(this.body, outboundMessage.body) &&
        Objects.equals(this.templateId, outboundMessage.templateId) &&
        Objects.equals(this.relatedEntityType, outboundMessage.relatedEntityType) &&
        Objects.equals(this.relatedEntityId, outboundMessage.relatedEntityId) &&
        Objects.equals(this.status, outboundMessage.status) &&
        Objects.equals(this.providerResponse, outboundMessage.providerResponse) &&
        Objects.equals(this.scheduledAt, outboundMessage.scheduledAt) &&
        Objects.equals(this.sentAt, outboundMessage.sentAt) &&
        Objects.equals(this.createTime, outboundMessage.createTime) &&
        Objects.equals(this.createdBy, outboundMessage.createdBy) &&
        Objects.equals(this.updateTime, outboundMessage.updateTime) &&
        Objects.equals(this.updatedBy, outboundMessage.updatedBy) &&
        Objects.equals(this.etag, outboundMessage.etag);
  }

  @Override
  public int hashCode() {
    return Objects.hash(templateParams, actionUrl, id, institutionId, academicYearId, messageKind, channel, sender, recipient, recipientUserId, subject, body, templateId, relatedEntityType, relatedEntityId, status, providerResponse, scheduledAt, sentAt, createTime, createdBy, updateTime, updatedBy, etag);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class OutboundMessage {\n");
    sb.append("    templateParams: ").append(toIndentedString(templateParams)).append("\n");
    sb.append("    actionUrl: ").append(toIndentedString(actionUrl)).append("\n");
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    institutionId: ").append(toIndentedString(institutionId)).append("\n");
    sb.append("    academicYearId: ").append(toIndentedString(academicYearId)).append("\n");
    sb.append("    messageKind: ").append(toIndentedString(messageKind)).append("\n");
    sb.append("    channel: ").append(toIndentedString(channel)).append("\n");
    sb.append("    sender: ").append(toIndentedString(sender)).append("\n");
    sb.append("    recipient: ").append(toIndentedString(recipient)).append("\n");
    sb.append("    recipientUserId: ").append(toIndentedString(recipientUserId)).append("\n");
    sb.append("    subject: ").append(toIndentedString(subject)).append("\n");
    sb.append("    body: ").append(toIndentedString(body)).append("\n");
    sb.append("    templateId: ").append(toIndentedString(templateId)).append("\n");
    sb.append("    relatedEntityType: ").append(toIndentedString(relatedEntityType)).append("\n");
    sb.append("    relatedEntityId: ").append(toIndentedString(relatedEntityId)).append("\n");
    sb.append("    status: ").append(toIndentedString(status)).append("\n");
    sb.append("    providerResponse: ").append(toIndentedString(providerResponse)).append("\n");
    sb.append("    scheduledAt: ").append(toIndentedString(scheduledAt)).append("\n");
    sb.append("    sentAt: ").append(toIndentedString(sentAt)).append("\n");
    sb.append("    createTime: ").append(toIndentedString(createTime)).append("\n");
    sb.append("    createdBy: ").append(toIndentedString(createdBy)).append("\n");
    sb.append("    updateTime: ").append(toIndentedString(updateTime)).append("\n");
    sb.append("    updatedBy: ").append(toIndentedString(updatedBy)).append("\n");
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

