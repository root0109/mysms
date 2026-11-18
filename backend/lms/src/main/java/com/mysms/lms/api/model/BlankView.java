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
 * BlankView
 */

public class BlankView {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private UUID blankId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private String label;

  public BlankView() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public BlankView(UUID blankId, String label) {
    this.blankId = blankId;
    this.label = label;
  }

  public BlankView blankId(UUID blankId) {
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

  public BlankView label(String label) {
    this.label = label;
    return this;
  }

  /**
   * Blank label / placeholder.
   * @return label
   */
  @NotNull 
  @JsonProperty("label")
  public String getLabel() {
    return label;
  }

  @JsonProperty("label")
  public void setLabel(String label) {
    this.label = label;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    BlankView blankView = (BlankView) o;
    return Objects.equals(this.blankId, blankView.blankId) &&
        Objects.equals(this.label, blankView.label);
  }

  @Override
  public int hashCode() {
    return Objects.hash(blankId, label);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class BlankView {\n");
    sb.append("    blankId: ").append(toIndentedString(blankId)).append("\n");
    sb.append("    label: ").append(toIndentedString(label)).append("\n");
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

