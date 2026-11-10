// Owned API contract of lms (JAVA-CODING-RULES §7); first materialized from api/openapi/lms.yaml on 2026-11-10.
package com.mysms.lms.api.model;

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
 * Fields of DriveItemShare that can be updated (partial update; see updateMask).
 */

public class DriveItemShareUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID userId;

  /**
   * The user's reaction (NULL = none).
   */
  public enum ReactionEnum {
    LIKE("like"),
    
    DISLIKE("dislike");

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

  public DriveItemShareUpdate userId(@Nullable UUID userId) {
    this.userId = userId;
    return this;
  }

  /**
   * User the item is shared with.
   * @return userId
   */
  @Valid 
  @JsonProperty("userId")
  public @Nullable UUID getUserId() {
    return userId;
  }

  @JsonProperty("userId")
  public void setUserId(@Nullable UUID userId) {
    this.userId = userId;
  }

  public DriveItemShareUpdate reaction(@Nullable ReactionEnum reaction) {
    this.reaction = reaction;
    return this;
  }

  /**
   * The user's reaction (NULL = none).
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

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    DriveItemShareUpdate driveItemShareUpdate = (DriveItemShareUpdate) o;
    return Objects.equals(this.userId, driveItemShareUpdate.userId) &&
        Objects.equals(this.reaction, driveItemShareUpdate.reaction);
  }

  @Override
  public int hashCode() {
    return Objects.hash(userId, reaction);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class DriveItemShareUpdate {\n");
    sb.append("    userId: ").append(toIndentedString(userId)).append("\n");
    sb.append("    reaction: ").append(toIndentedString(reaction)).append("\n");
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

