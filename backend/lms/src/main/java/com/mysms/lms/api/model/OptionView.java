// Owned API contract of lms (JAVA-CODING-RULES §7); first materialized from api/openapi/lms.yaml on 2026-11-10.
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
 * OptionView
 */

public class OptionView {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private UUID optionId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private String optionText;

  public OptionView() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public OptionView(UUID optionId, String optionText) {
    this.optionId = optionId;
    this.optionText = optionText;
  }

  public OptionView optionId(UUID optionId) {
    this.optionId = optionId;
    return this;
  }

  /**
   * Choice id.
   * @return optionId
   */
  @NotNull @Valid 
  @JsonProperty("optionId")
  public UUID getOptionId() {
    return optionId;
  }

  @JsonProperty("optionId")
  public void setOptionId(UUID optionId) {
    this.optionId = optionId;
  }

  public OptionView optionText(String optionText) {
    this.optionText = optionText;
    return this;
  }

  /**
   * Choice.
   * @return optionText
   */
  @NotNull 
  @JsonProperty("optionText")
  public String getOptionText() {
    return optionText;
  }

  @JsonProperty("optionText")
  public void setOptionText(String optionText) {
    this.optionText = optionText;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    OptionView optionView = (OptionView) o;
    return Objects.equals(this.optionId, optionView.optionId) &&
        Objects.equals(this.optionText, optionView.optionText);
  }

  @Override
  public int hashCode() {
    return Objects.hash(optionId, optionText);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class OptionView {\n");
    sb.append("    optionId: ").append(toIndentedString(optionId)).append("\n");
    sb.append("    optionText: ").append(toIndentedString(optionText)).append("\n");
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

