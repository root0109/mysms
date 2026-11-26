// Owned API contract of lms.
package com.mysms.learning.lms.api.model;

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
 * Fields of LessonNote that can be updated (PATCH: a property left out keeps its value, null clears it, a value sets it).
 */

public class LessonNoteUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull UUID> userId;

  /**
   * Highlight colour.
   */
  public enum ColorEnum {
    RED("red"),
    
    BLUE("blue"),
    
    GREEN("green"),
    
    YELLOW("yellow"),
    
    PURPLE("purple");

    private final String value;

    ColorEnum(String value) {
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
    public static ColorEnum fromValue(String value) {
      for (ColorEnum b : ColorEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull ColorEnum> color;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<String> highlightedText;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<String> note;

  public LessonNoteUpdate userId(@Nullable Optional<UUID> userId) {
    this.userId = userId;
    return this;
  }

  /**
   * Student.
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

  public LessonNoteUpdate color(@Nullable Optional<ColorEnum> color) {
    this.color = color;
    return this;
  }

  /**
   * Highlight colour.
   * @return color
   */
  
  @JsonProperty("color")
  public @Nullable Optional<ColorEnum> getColor() {
    return color;
  }

  @JsonProperty("color")
  public void setColor(@Nullable Optional<ColorEnum> color) {
    this.color = color;
  }

  public LessonNoteUpdate highlightedText(@Nullable Optional<String> highlightedText) {
    this.highlightedText = highlightedText;
    return this;
  }

  /**
   * Highlighted passage.
   * @return highlightedText
   */
  
  @JsonProperty("highlightedText")
  public @Nullable Optional<String> getHighlightedText() {
    return highlightedText;
  }

  @JsonProperty("highlightedText")
  public void setHighlightedText(@Nullable Optional<String> highlightedText) {
    this.highlightedText = highlightedText;
  }

  public LessonNoteUpdate note(@Nullable Optional<String> note) {
    this.note = note;
    return this;
  }

  /**
   * Note.
   * @return note
   */
  
  @JsonProperty("note")
  public @Nullable Optional<String> getNote() {
    return note;
  }

  @JsonProperty("note")
  public void setNote(@Nullable Optional<String> note) {
    this.note = note;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    LessonNoteUpdate lessonNoteUpdate = (LessonNoteUpdate) o;
    return Objects.equals(this.userId, lessonNoteUpdate.userId) &&
        Objects.equals(this.color, lessonNoteUpdate.color) &&
        Objects.equals(this.highlightedText, lessonNoteUpdate.highlightedText) &&
        Objects.equals(this.note, lessonNoteUpdate.note);
  }

  @Override
  public int hashCode() {
    return Objects.hash(userId, color, highlightedText, note);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class LessonNoteUpdate {\n");
    sb.append("    userId: ").append(toIndentedString(userId)).append("\n");
    sb.append("    color: ").append(toIndentedString(color)).append("\n");
    sb.append("    highlightedText: ").append(toIndentedString(highlightedText)).append("\n");
    sb.append("    note: ").append(toIndentedString(note)).append("\n");
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

