// Owned API contract of comms.
package com.mysms.comms.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.mysms.comms.api.model.Recipients;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.lang.Nullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;

/**
 * SendMessageRequest
 */

public class SendMessageRequest {

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
  private Recipients recipients;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID templateId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String subject;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String body;

  private @Nullable Object payload = null;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private @Nullable OffsetDateTime scheduledAt;

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

  public SendMessageRequest() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public SendMessageRequest(ChannelEnum channel, Recipients recipients, MessageKindEnum messageKind) {
    this.channel = channel;
    this.recipients = recipients;
    this.messageKind = messageKind;
  }

  public SendMessageRequest channel(ChannelEnum channel) {
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

  public SendMessageRequest recipients(Recipients recipients) {
    this.recipients = recipients;
    return this;
  }

  /**
   * Get recipients
   * @return recipients
   */
  @NotNull @Valid 
  @JsonProperty("recipients")
  public Recipients getRecipients() {
    return recipients;
  }

  @JsonProperty("recipients")
  public void setRecipients(Recipients recipients) {
    this.recipients = recipients;
  }

  public SendMessageRequest templateId(@Nullable UUID templateId) {
    this.templateId = templateId;
    return this;
  }

  /**
   * Template.
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

  public SendMessageRequest subject(@Nullable String subject) {
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

  public SendMessageRequest body(@Nullable String body) {
    this.body = body;
    return this;
  }

  /**
   * Body (when no template).
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

  public SendMessageRequest payload(@Nullable Object payload) {
    this.payload = payload;
    return this;
  }

  /**
   * Template variables.
   * @return payload
   */
  
  @JsonProperty("payload")
  public @Nullable Object getPayload() {
    return payload;
  }

  @JsonProperty("payload")
  public void setPayload(@Nullable Object payload) {
    this.payload = payload;
  }

  public SendMessageRequest scheduledAt(@Nullable OffsetDateTime scheduledAt) {
    this.scheduledAt = scheduledAt;
    return this;
  }

  /**
   * Send later.
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

  public SendMessageRequest messageKind(MessageKindEnum messageKind) {
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

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    SendMessageRequest sendMessageRequest = (SendMessageRequest) o;
    return Objects.equals(this.channel, sendMessageRequest.channel) &&
        Objects.equals(this.recipients, sendMessageRequest.recipients) &&
        Objects.equals(this.templateId, sendMessageRequest.templateId) &&
        Objects.equals(this.subject, sendMessageRequest.subject) &&
        Objects.equals(this.body, sendMessageRequest.body) &&
        Objects.equals(this.payload, sendMessageRequest.payload) &&
        Objects.equals(this.scheduledAt, sendMessageRequest.scheduledAt) &&
        Objects.equals(this.messageKind, sendMessageRequest.messageKind);
  }

  @Override
  public int hashCode() {
    return Objects.hash(channel, recipients, templateId, subject, body, payload, scheduledAt, messageKind);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class SendMessageRequest {\n");
    sb.append("    channel: ").append(toIndentedString(channel)).append("\n");
    sb.append("    recipients: ").append(toIndentedString(recipients)).append("\n");
    sb.append("    templateId: ").append(toIndentedString(templateId)).append("\n");
    sb.append("    subject: ").append(toIndentedString(subject)).append("\n");
    sb.append("    body: ").append(toIndentedString(body)).append("\n");
    sb.append("    payload: ").append(toIndentedString(payload)).append("\n");
    sb.append("    scheduledAt: ").append(toIndentedString(scheduledAt)).append("\n");
    sb.append("    messageKind: ").append(toIndentedString(messageKind)).append("\n");
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

