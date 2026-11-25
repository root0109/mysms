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
 * Fields of CommentReaction that can be updated (partial update; see updateMask).
 */

public class CommentReactionUpdate {

  /**
   * Reaction.
   */
  public enum ReactionEnum {
    LIKE("like"),
    
    UNLIKE("unlike");

    private final String value;

    ReactionEnum(String value) {
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
    public static ReactionEnum fromValue(String value) {
      for (ReactionEnum b : ReactionEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable ReactionEnum reaction;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Boolean isActive;

  public CommentReactionUpdate reaction(@Nullable ReactionEnum reaction) {
    this.reaction = reaction;
    return this;
  }

  /**
   * Reaction.
   * @return reaction
   */
  
  @JsonProperty("reaction")
  public @Nullable ReactionEnum getReaction() {
    return reaction;
  }

  @JsonProperty("reaction")
  public void setReaction(@Nullable ReactionEnum reaction) {
    this.reaction = reaction;
  }

  public CommentReactionUpdate isActive(@Nullable Boolean isActive) {
    this.isActive = isActive;
    return this;
  }

  /**
   * Active.
   * @return isActive
   */
  
  @JsonProperty("isActive")
  public @Nullable Boolean getIsActive() {
    return isActive;
  }

  @JsonProperty("isActive")
  public void setIsActive(@Nullable Boolean isActive) {
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
    CommentReactionUpdate commentReactionUpdate = (CommentReactionUpdate) o;
    return Objects.equals(this.reaction, commentReactionUpdate.reaction) &&
        Objects.equals(this.isActive, commentReactionUpdate.isActive);
  }

  @Override
  public int hashCode() {
    return Objects.hash(reaction, isActive);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class CommentReactionUpdate {\n");
    sb.append("    reaction: ").append(toIndentedString(reaction)).append("\n");
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

