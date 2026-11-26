// Owned API contract of comms.
package com.mysms.comms.api.model;

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
 * Fields of MessageTemplate that can be updated (PATCH: a property left out keeps its value, null clears it, a value sets it).
 */

public class MessageTemplateUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<UUID> institutionId;

  /**
   * Delivery channel.
   */
  public enum ChannelEnum {
    EMAIL("email"),
    
    SMS("sms"),
    
    PUSH("push"),
    
    IN_APP("in_app");

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
  private @Nullable Optional<@NotNull ChannelEnum> channel;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@Size(max = 80) String> code;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull @Size(max = 150) String> name;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<String> subject;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<String> body;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@Size(max = 80) String> externalTemplateId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull Boolean> isActive;

  public MessageTemplateUpdate institutionId(@Nullable Optional<UUID> institutionId) {
    this.institutionId = institutionId;
    return this;
  }

  /**
   * Institution (NULL = tenant-wide).
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

  public MessageTemplateUpdate channel(@Nullable Optional<ChannelEnum> channel) {
    this.channel = channel;
    return this;
  }

  /**
   * Delivery channel.
   * @return channel
   */
  
  @JsonProperty("channel")
  public @Nullable Optional<ChannelEnum> getChannel() {
    return channel;
  }

  @JsonProperty("channel")
  public void setChannel(@Nullable Optional<ChannelEnum> channel) {
    this.channel = channel;
  }

  public MessageTemplateUpdate code(@Nullable Optional<String> code) {
    this.code = code;
    return this;
  }

  /**
   * Template key used by the application.
   * @return code
   */
  
  @JsonProperty("code")
  public @Nullable Optional<String> getCode() {
    return code;
  }

  @JsonProperty("code")
  public void setCode(@Nullable Optional<String> code) {
    this.code = code;
  }

  public MessageTemplateUpdate name(@Nullable Optional<String> name) {
    this.name = name;
    return this;
  }

  /**
   * Template name.
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

  public MessageTemplateUpdate subject(@Nullable Optional<String> subject) {
    this.subject = subject;
    return this;
  }

  /**
   * Subject (e-mail).
   * @return subject
   */
  
  @JsonProperty("subject")
  public @Nullable Optional<String> getSubject() {
    return subject;
  }

  @JsonProperty("subject")
  public void setSubject(@Nullable Optional<String> subject) {
    this.subject = subject;
  }

  public MessageTemplateUpdate body(@Nullable Optional<String> body) {
    this.body = body;
    return this;
  }

  /**
   * Body with placeholders.
   * @return body
   */
  
  @JsonProperty("body")
  public @Nullable Optional<String> getBody() {
    return body;
  }

  @JsonProperty("body")
  public void setBody(@Nullable Optional<String> body) {
    this.body = body;
  }

  public MessageTemplateUpdate externalTemplateId(@Nullable Optional<String> externalTemplateId) {
    this.externalTemplateId = externalTemplateId;
    return this;
  }

  /**
   * External provider-registered template id (e.g. SMS DLT template id).
   * @return externalTemplateId
   */
  
  @JsonProperty("externalTemplateId")
  public @Nullable Optional<String> getExternalTemplateId() {
    return externalTemplateId;
  }

  @JsonProperty("externalTemplateId")
  public void setExternalTemplateId(@Nullable Optional<String> externalTemplateId) {
    this.externalTemplateId = externalTemplateId;
  }

  public MessageTemplateUpdate isActive(@Nullable Optional<Boolean> isActive) {
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
    MessageTemplateUpdate messageTemplateUpdate = (MessageTemplateUpdate) o;
    return Objects.equals(this.institutionId, messageTemplateUpdate.institutionId) &&
        Objects.equals(this.channel, messageTemplateUpdate.channel) &&
        Objects.equals(this.code, messageTemplateUpdate.code) &&
        Objects.equals(this.name, messageTemplateUpdate.name) &&
        Objects.equals(this.subject, messageTemplateUpdate.subject) &&
        Objects.equals(this.body, messageTemplateUpdate.body) &&
        Objects.equals(this.externalTemplateId, messageTemplateUpdate.externalTemplateId) &&
        Objects.equals(this.isActive, messageTemplateUpdate.isActive);
  }

  @Override
  public int hashCode() {
    return Objects.hash(institutionId, channel, code, name, subject, body, externalTemplateId, isActive);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class MessageTemplateUpdate {\n");
    sb.append("    institutionId: ").append(toIndentedString(institutionId)).append("\n");
    sb.append("    channel: ").append(toIndentedString(channel)).append("\n");
    sb.append("    code: ").append(toIndentedString(code)).append("\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    subject: ").append(toIndentedString(subject)).append("\n");
    sb.append("    body: ").append(toIndentedString(body)).append("\n");
    sb.append("    externalTemplateId: ").append(toIndentedString(externalTemplateId)).append("\n");
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

