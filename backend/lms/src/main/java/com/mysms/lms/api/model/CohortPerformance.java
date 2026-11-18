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
 * CohortPerformance
 */

public class CohortPerformance {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private UUID contentItemId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String averagePercent;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String medianPercent;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private Integer submitted;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private Integer graded;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private Integer missing;

  public CohortPerformance() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public CohortPerformance(UUID contentItemId, Integer submitted, Integer graded, Integer missing) {
    this.contentItemId = contentItemId;
    this.submitted = submitted;
    this.graded = graded;
    this.missing = missing;
  }

  public CohortPerformance contentItemId(UUID contentItemId) {
    this.contentItemId = contentItemId;
    return this;
  }

  /**
   * Item.
   * @return contentItemId
   */
  @NotNull @Valid 
  @JsonProperty("contentItemId")
  public UUID getContentItemId() {
    return contentItemId;
  }

  @JsonProperty("contentItemId")
  public void setContentItemId(UUID contentItemId) {
    this.contentItemId = contentItemId;
  }

  public CohortPerformance averagePercent(@Nullable String averagePercent) {
    this.averagePercent = averagePercent;
    return this;
  }

  /**
   * Average.
   * @return averagePercent
   */
  @Pattern(regexp = "^-?\\d+(\\.\\d+)?$") 
  @JsonProperty("averagePercent")
  public @Nullable String getAveragePercent() {
    return averagePercent;
  }

  @JsonProperty("averagePercent")
  public void setAveragePercent(@Nullable String averagePercent) {
    this.averagePercent = averagePercent;
  }

  public CohortPerformance medianPercent(@Nullable String medianPercent) {
    this.medianPercent = medianPercent;
    return this;
  }

  /**
   * Median.
   * @return medianPercent
   */
  @Pattern(regexp = "^-?\\d+(\\.\\d+)?$") 
  @JsonProperty("medianPercent")
  public @Nullable String getMedianPercent() {
    return medianPercent;
  }

  @JsonProperty("medianPercent")
  public void setMedianPercent(@Nullable String medianPercent) {
    this.medianPercent = medianPercent;
  }

  public CohortPerformance submitted(Integer submitted) {
    this.submitted = submitted;
    return this;
  }

  /**
   * Submissions.
   * @return submitted
   */
  @NotNull 
  @JsonProperty("submitted")
  public Integer getSubmitted() {
    return submitted;
  }

  @JsonProperty("submitted")
  public void setSubmitted(Integer submitted) {
    this.submitted = submitted;
  }

  public CohortPerformance graded(Integer graded) {
    this.graded = graded;
    return this;
  }

  /**
   * Graded.
   * @return graded
   */
  @NotNull 
  @JsonProperty("graded")
  public Integer getGraded() {
    return graded;
  }

  @JsonProperty("graded")
  public void setGraded(Integer graded) {
    this.graded = graded;
  }

  public CohortPerformance missing(Integer missing) {
    this.missing = missing;
    return this;
  }

  /**
   * Not submitted after the due date.
   * @return missing
   */
  @NotNull 
  @JsonProperty("missing")
  public Integer getMissing() {
    return missing;
  }

  @JsonProperty("missing")
  public void setMissing(Integer missing) {
    this.missing = missing;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    CohortPerformance cohortPerformance = (CohortPerformance) o;
    return Objects.equals(this.contentItemId, cohortPerformance.contentItemId) &&
        Objects.equals(this.averagePercent, cohortPerformance.averagePercent) &&
        Objects.equals(this.medianPercent, cohortPerformance.medianPercent) &&
        Objects.equals(this.submitted, cohortPerformance.submitted) &&
        Objects.equals(this.graded, cohortPerformance.graded) &&
        Objects.equals(this.missing, cohortPerformance.missing);
  }

  @Override
  public int hashCode() {
    return Objects.hash(contentItemId, averagePercent, medianPercent, submitted, graded, missing);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class CohortPerformance {\n");
    sb.append("    contentItemId: ").append(toIndentedString(contentItemId)).append("\n");
    sb.append("    averagePercent: ").append(toIndentedString(averagePercent)).append("\n");
    sb.append("    medianPercent: ").append(toIndentedString(medianPercent)).append("\n");
    sb.append("    submitted: ").append(toIndentedString(submitted)).append("\n");
    sb.append("    graded: ").append(toIndentedString(graded)).append("\n");
    sb.append("    missing: ").append(toIndentedString(missing)).append("\n");
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

