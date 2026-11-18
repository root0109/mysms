// Owned API contract of lms.
package com.mysms.lms.api.model;

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
 * BlankAnswer
 */

public class BlankAnswer {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private UUID blankId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private String text;

  public BlankAnswer() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public BlankAnswer(UUID blankId, String text) {
    this.blankId = blankId;
    this.text = text;
  }

  public BlankAnswer blankId(UUID blankId) {
    this.blankId = blankId;
    return this;
  }

  /**
   * Blank.
   * @return blankId
   */
  @NotNull @Valid 
  @JsonProperty("blankId")
  public UUID getBlankId() {
    return blankId;
  }

  @JsonProperty("blankId")
  public void setBlankId(UUID blankId) {
    this.blankId = blankId;
  }

  public BlankAnswer text(String text) {
    this.text = text;
    return this;
  }

  /**
   * Answer typed in the blank.
   * @return text
   */
  @NotNull 
  @JsonProperty("text")
  public String getText() {
    return text;
  }

  @JsonProperty("text")
  public void setText(String text) {
    this.text = text;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    BlankAnswer blankAnswer = (BlankAnswer) o;
    return Objects.equals(this.blankId, blankAnswer.blankId) &&
        Objects.equals(this.text, blankAnswer.text);
  }

  @Override
  public int hashCode() {
    return Objects.hash(blankId, text);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class BlankAnswer {\n");
    sb.append("    blankId: ").append(toIndentedString(blankId)).append("\n");
    sb.append("    text: ").append(toIndentedString(text)).append("\n");
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

