// Owned API contract of comms.
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
 * Fields of Conversation that can be updated (PATCH: a property left out keeps its value, null clears it, a value sets it).
 */

public class ConversationUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<UUID> institutionId;

  /**
   * Direct or group.
   */
  public enum ConversationTypeEnum {
    DIRECT("direct"),
    
    GROUP("group");

    private final String value;

    ConversationTypeEnum(String value) {
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
    public static ConversationTypeEnum fromValue(String value) {
      for (ConversationTypeEnum b : ConversationTypeEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull ConversationTypeEnum> conversationType;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@Size(max = 150) String> title;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private @Nullable Optional<OffsetDateTime> lastMessageAt;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull Boolean> isActive;

  public ConversationUpdate institutionId(@Nullable Optional<UUID> institutionId) {
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

  public ConversationUpdate conversationType(@Nullable Optional<ConversationTypeEnum> conversationType) {
    this.conversationType = conversationType;
    return this;
  }

  /**
   * Direct or group.
   * @return conversationType
   */
  
  @JsonProperty("conversationType")
  public @Nullable Optional<ConversationTypeEnum> getConversationType() {
    return conversationType;
  }

  @JsonProperty("conversationType")
  public void setConversationType(@Nullable Optional<ConversationTypeEnum> conversationType) {
    this.conversationType = conversationType;
  }

  public ConversationUpdate title(@Nullable Optional<String> title) {
    this.title = title;
    return this;
  }

  /**
   * Group title.
   * @return title
   */
  
  @JsonProperty("title")
  public @Nullable Optional<String> getTitle() {
    return title;
  }

  @JsonProperty("title")
  public void setTitle(@Nullable Optional<String> title) {
    this.title = title;
  }

  public ConversationUpdate lastMessageAt(@Nullable Optional<OffsetDateTime> lastMessageAt) {
    this.lastMessageAt = lastMessageAt;
    return this;
  }

  /**
   * Last activity.
   * @return lastMessageAt
   */
  
  @JsonProperty("lastMessageAt")
  public @Nullable Optional<OffsetDateTime> getLastMessageAt() {
    return lastMessageAt;
  }

  @JsonProperty("lastMessageAt")
  public void setLastMessageAt(@Nullable Optional<OffsetDateTime> lastMessageAt) {
    this.lastMessageAt = lastMessageAt;
  }

  public ConversationUpdate isActive(@Nullable Optional<Boolean> isActive) {
    this.isActive = isActive;
    return this;
  }

  /**
   * Open.
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
    ConversationUpdate conversationUpdate = (ConversationUpdate) o;
    return Objects.equals(this.institutionId, conversationUpdate.institutionId) &&
        Objects.equals(this.conversationType, conversationUpdate.conversationType) &&
        Objects.equals(this.title, conversationUpdate.title) &&
        Objects.equals(this.lastMessageAt, conversationUpdate.lastMessageAt) &&
        Objects.equals(this.isActive, conversationUpdate.isActive);
  }

  @Override
  public int hashCode() {
    return Objects.hash(institutionId, conversationType, title, lastMessageAt, isActive);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ConversationUpdate {\n");
    sb.append("    institutionId: ").append(toIndentedString(institutionId)).append("\n");
    sb.append("    conversationType: ").append(toIndentedString(conversationType)).append("\n");
    sb.append("    title: ").append(toIndentedString(title)).append("\n");
    sb.append("    lastMessageAt: ").append(toIndentedString(lastMessageAt)).append("\n");
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

