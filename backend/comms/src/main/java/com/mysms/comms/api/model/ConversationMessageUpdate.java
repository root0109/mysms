// Owned API contract of comms.
package com.mysms.comms.api.model;

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
 * Fields of ConversationMessage that can be updated (PATCH: a property left out keeps its value, null clears it, a value sets it).
 */

public class ConversationMessageUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull String> body;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull Boolean> deletedBySender;

  public ConversationMessageUpdate body(@Nullable Optional<String> body) {
    this.body = body;
    return this;
  }

  /**
   * Message.
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

  public ConversationMessageUpdate deletedBySender(@Nullable Optional<Boolean> deletedBySender) {
    this.deletedBySender = deletedBySender;
    return this;
  }

  /**
   * Hidden for the sender.
   * @return deletedBySender
   */
  
  @JsonProperty("deletedBySender")
  public @Nullable Optional<Boolean> getDeletedBySender() {
    return deletedBySender;
  }

  @JsonProperty("deletedBySender")
  public void setDeletedBySender(@Nullable Optional<Boolean> deletedBySender) {
    this.deletedBySender = deletedBySender;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ConversationMessageUpdate conversationMessageUpdate = (ConversationMessageUpdate) o;
    return Objects.equals(this.body, conversationMessageUpdate.body) &&
        Objects.equals(this.deletedBySender, conversationMessageUpdate.deletedBySender);
  }

  @Override
  public int hashCode() {
    return Objects.hash(body, deletedBySender);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ConversationMessageUpdate {\n");
    sb.append("    body: ").append(toIndentedString(body)).append("\n");
    sb.append("    deletedBySender: ").append(toIndentedString(deletedBySender)).append("\n");
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

