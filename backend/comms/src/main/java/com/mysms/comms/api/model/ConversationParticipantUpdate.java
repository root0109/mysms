// Owned API contract of comms.
package com.mysms.comms.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.lang.Nullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;

/**
 * Fields of ConversationParticipant that can be updated (PATCH: a property left out keeps its value, null clears it, a value sets it).
 */

public class ConversationParticipantUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull UUID> userId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private @Nullable Optional<OffsetDateTime> lastReadAt;

  public ConversationParticipantUpdate userId(@Nullable Optional<UUID> userId) {
    this.userId = userId;
    return this;
  }

  /**
   * Participant.
   * @return userId
   */
  
  @JsonProperty("userId")
  public @Nullable Optional<UUID> getUserId() {
    return userId;
  }

  @JsonProperty("userId")
  public void setUserId(@Nullable Optional<UUID> userId) {
    this.userId = userId;
  }

  public ConversationParticipantUpdate lastReadAt(@Nullable Optional<OffsetDateTime> lastReadAt) {
    this.lastReadAt = lastReadAt;
    return this;
  }

  /**
   * Read up to.
   * @return lastReadAt
   */
  
  @JsonProperty("lastReadAt")
  public @Nullable Optional<OffsetDateTime> getLastReadAt() {
    return lastReadAt;
  }

  @JsonProperty("lastReadAt")
  public void setLastReadAt(@Nullable Optional<OffsetDateTime> lastReadAt) {
    this.lastReadAt = lastReadAt;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ConversationParticipantUpdate conversationParticipantUpdate = (ConversationParticipantUpdate) o;
    return Objects.equals(this.userId, conversationParticipantUpdate.userId) &&
        Objects.equals(this.lastReadAt, conversationParticipantUpdate.lastReadAt);
  }

  @Override
  public int hashCode() {
    return Objects.hash(userId, lastReadAt);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ConversationParticipantUpdate {\n");
    sb.append("    userId: ").append(toIndentedString(userId)).append("\n");
    sb.append("    lastReadAt: ").append(toIndentedString(lastReadAt)).append("\n");
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

