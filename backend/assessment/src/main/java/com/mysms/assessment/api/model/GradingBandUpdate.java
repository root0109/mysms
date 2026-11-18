// Owned API contract of assessment.
package com.mysms.assessment.api.model;

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
 * Fields of GradingBand that can be updated (partial update; see updateMask).
 */

public class GradingBandUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID gradingSchemeId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String minPercent;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String maxPercent;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String gradeLabel;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String gradePoint;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Boolean isPass;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String description;

  public GradingBandUpdate gradingSchemeId(@Nullable UUID gradingSchemeId) {
    this.gradingSchemeId = gradingSchemeId;
    return this;
  }

  /**
   * Scheme the band belongs to (NULL = standalone band selected per result template).
   * @return gradingSchemeId
   */
  @Valid 
  @JsonProperty("gradingSchemeId")
  public @Nullable UUID getGradingSchemeId() {
    return gradingSchemeId;
  }

  @JsonProperty("gradingSchemeId")
  public void setGradingSchemeId(@Nullable UUID gradingSchemeId) {
    this.gradingSchemeId = gradingSchemeId;
  }

  public GradingBandUpdate minPercent(@Nullable String minPercent) {
    this.minPercent = minPercent;
    return this;
  }

  /**
   * Lower bound (inclusive).
   * @return minPercent
   */
  @Pattern(regexp = "^-?\\d+(\\.\\d+)?$") 
  @JsonProperty("minPercent")
  public @Nullable String getMinPercent() {
    return minPercent;
  }

  @JsonProperty("minPercent")
  public void setMinPercent(@Nullable String minPercent) {
    this.minPercent = minPercent;
  }

  public GradingBandUpdate maxPercent(@Nullable String maxPercent) {
    this.maxPercent = maxPercent;
    return this;
  }

  /**
   * Upper bound (inclusive).
   * @return maxPercent
   */
  @Pattern(regexp = "^-?\\d+(\\.\\d+)?$") 
  @JsonProperty("maxPercent")
  public @Nullable String getMaxPercent() {
    return maxPercent;
  }

  @JsonProperty("maxPercent")
  public void setMaxPercent(@Nullable String maxPercent) {
    this.maxPercent = maxPercent;
  }

  public GradingBandUpdate gradeLabel(@Nullable String gradeLabel) {
    this.gradeLabel = gradeLabel;
    return this;
  }

  /**
   * Grade / result label.
   * @return gradeLabel
   */
  @Size(max = 30) 
  @JsonProperty("gradeLabel")
  public @Nullable String getGradeLabel() {
    return gradeLabel;
  }

  @JsonProperty("gradeLabel")
  public void setGradeLabel(@Nullable String gradeLabel) {
    this.gradeLabel = gradeLabel;
  }

  public GradingBandUpdate gradePoint(@Nullable String gradePoint) {
    this.gradePoint = gradePoint;
    return this;
  }

  /**
   * Grade point.
   * @return gradePoint
   */
  @Pattern(regexp = "^-?\\d+(\\.\\d+)?$") 
  @JsonProperty("gradePoint")
  public @Nullable String getGradePoint() {
    return gradePoint;
  }

  @JsonProperty("gradePoint")
  public void setGradePoint(@Nullable String gradePoint) {
    this.gradePoint = gradePoint;
  }

  public GradingBandUpdate isPass(@Nullable Boolean isPass) {
    this.isPass = isPass;
    return this;
  }

  /**
   * Whether the band is a pass.
   * @return isPass
   */
  
  @JsonProperty("isPass")
  public @Nullable Boolean getIsPass() {
    return isPass;
  }

  @JsonProperty("isPass")
  public void setIsPass(@Nullable Boolean isPass) {
    this.isPass = isPass;
  }

  public GradingBandUpdate description(@Nullable String description) {
    this.description = description;
    return this;
  }

  /**
   * Descriptor.
   * @return description
   */
  
  @JsonProperty("description")
  public @Nullable String getDescription() {
    return description;
  }

  @JsonProperty("description")
  public void setDescription(@Nullable String description) {
    this.description = description;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    GradingBandUpdate gradingBandUpdate = (GradingBandUpdate) o;
    return Objects.equals(this.gradingSchemeId, gradingBandUpdate.gradingSchemeId) &&
        Objects.equals(this.minPercent, gradingBandUpdate.minPercent) &&
        Objects.equals(this.maxPercent, gradingBandUpdate.maxPercent) &&
        Objects.equals(this.gradeLabel, gradingBandUpdate.gradeLabel) &&
        Objects.equals(this.gradePoint, gradingBandUpdate.gradePoint) &&
        Objects.equals(this.isPass, gradingBandUpdate.isPass) &&
        Objects.equals(this.description, gradingBandUpdate.description);
  }

  @Override
  public int hashCode() {
    return Objects.hash(gradingSchemeId, minPercent, maxPercent, gradeLabel, gradePoint, isPass, description);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class GradingBandUpdate {\n");
    sb.append("    gradingSchemeId: ").append(toIndentedString(gradingSchemeId)).append("\n");
    sb.append("    minPercent: ").append(toIndentedString(minPercent)).append("\n");
    sb.append("    maxPercent: ").append(toIndentedString(maxPercent)).append("\n");
    sb.append("    gradeLabel: ").append(toIndentedString(gradeLabel)).append("\n");
    sb.append("    gradePoint: ").append(toIndentedString(gradePoint)).append("\n");
    sb.append("    isPass: ").append(toIndentedString(isPass)).append("\n");
    sb.append("    description: ").append(toIndentedString(description)).append("\n");
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

