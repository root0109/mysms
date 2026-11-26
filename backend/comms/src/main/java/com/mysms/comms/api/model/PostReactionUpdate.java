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
 * Fields of PostReaction that can be updated (PATCH: a property left out keeps its value, null clears it, a value sets it).
 */

public class PostReactionUpdate {

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
  private @Nullable Optional<ReactionEnum> reaction;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<Boolean> isSaved;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<Boolean> isActive;

  public PostReactionUpdate reaction(@Nullable Optional<ReactionEnum> reaction) {
    this.reaction = reaction;
    return this;
  }

  /**
   * Reaction.
   * @return reaction
   */
  
  @JsonProperty("reaction")
  public @Nullable Optional<ReactionEnum> getReaction() {
    return reaction;
  }

  @JsonProperty("reaction")
  public void setReaction(@Nullable Optional<ReactionEnum> reaction) {
    this.reaction = reaction;
  }

  public PostReactionUpdate isSaved(@Nullable Optional<Boolean> isSaved) {
    this.isSaved = isSaved;
    return this;
  }

  /**
   * Saved / bookmarked.
   * @return isSaved
   */
  
  @JsonProperty("isSaved")
  public @Nullable Optional<Boolean> getIsSaved() {
    return isSaved;
  }

  @JsonProperty("isSaved")
  public void setIsSaved(@Nullable Optional<Boolean> isSaved) {
    this.isSaved = isSaved;
  }

  public PostReactionUpdate isActive(@Nullable Optional<Boolean> isActive) {
    this.isActive = isActive;
    return this;
  }

  /**
   * Active.
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
    PostReactionUpdate postReactionUpdate = (PostReactionUpdate) o;
    return Objects.equals(this.reaction, postReactionUpdate.reaction) &&
        Objects.equals(this.isSaved, postReactionUpdate.isSaved) &&
        Objects.equals(this.isActive, postReactionUpdate.isActive);
  }

  @Override
  public int hashCode() {
    return Objects.hash(reaction, isSaved, isActive);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class PostReactionUpdate {\n");
    sb.append("    reaction: ").append(toIndentedString(reaction)).append("\n");
    sb.append("    isSaved: ").append(toIndentedString(isSaved)).append("\n");
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

