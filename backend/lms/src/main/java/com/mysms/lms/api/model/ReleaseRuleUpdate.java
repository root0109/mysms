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
 * Fields of ReleaseRule that can be updated (partial update; see updateMask).
 */

public class ReleaseRuleUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID courseId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID cohortId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID sourceContentItemId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String title;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Boolean stopIfTrue;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Boolean isDisabled;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Boolean reassignSource;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Integer sortOrder;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID copiedFromId;

  public ReleaseRuleUpdate courseId(@Nullable UUID courseId) {
    this.courseId = courseId;
    return this;
  }

  /**
   * Course template.
   * @return courseId
   */
  @Valid 
  @JsonProperty("courseId")
  public @Nullable UUID getCourseId() {
    return courseId;
  }

  @JsonProperty("courseId")
  public void setCourseId(@Nullable UUID courseId) {
    this.courseId = courseId;
  }

  public ReleaseRuleUpdate cohortId(@Nullable UUID cohortId) {
    this.cohortId = cohortId;
    return this;
  }

  /**
   * Cohort copy.
   * @return cohortId
   */
  @Valid 
  @JsonProperty("cohortId")
  public @Nullable UUID getCohortId() {
    return cohortId;
  }

  @JsonProperty("cohortId")
  public void setCohortId(@Nullable UUID cohortId) {
    this.cohortId = cohortId;
  }

  public ReleaseRuleUpdate sourceContentItemId(@Nullable UUID sourceContentItemId) {
    this.sourceContentItemId = sourceContentItemId;
    return this;
  }

  /**
   * Item whose outcome is tested.
   * @return sourceContentItemId
   */
  @Valid 
  @JsonProperty("sourceContentItemId")
  public @Nullable UUID getSourceContentItemId() {
    return sourceContentItemId;
  }

  @JsonProperty("sourceContentItemId")
  public void setSourceContentItemId(@Nullable UUID sourceContentItemId) {
    this.sourceContentItemId = sourceContentItemId;
  }

  public ReleaseRuleUpdate title(@Nullable String title) {
    this.title = title;
    return this;
  }

  /**
   * Rule title.
   * @return title
   */
  @Size(max = 200) 
  @JsonProperty("title")
  public @Nullable String getTitle() {
    return title;
  }

  @JsonProperty("title")
  public void setTitle(@Nullable String title) {
    this.title = title;
  }

  public ReleaseRuleUpdate stopIfTrue(@Nullable Boolean stopIfTrue) {
    this.stopIfTrue = stopIfTrue;
    return this;
  }

  /**
   * Do not evaluate later rules when this one matches.
   * @return stopIfTrue
   */
  
  @JsonProperty("stopIfTrue")
  public @Nullable Boolean getStopIfTrue() {
    return stopIfTrue;
  }

  @JsonProperty("stopIfTrue")
  public void setStopIfTrue(@Nullable Boolean stopIfTrue) {
    this.stopIfTrue = stopIfTrue;
  }

  public ReleaseRuleUpdate isDisabled(@Nullable Boolean isDisabled) {
    this.isDisabled = isDisabled;
    return this;
  }

  /**
   * Rule disabled.
   * @return isDisabled
   */
  
  @JsonProperty("isDisabled")
  public @Nullable Boolean getIsDisabled() {
    return isDisabled;
  }

  @JsonProperty("isDisabled")
  public void setIsDisabled(@Nullable Boolean isDisabled) {
    this.isDisabled = isDisabled;
  }

  public ReleaseRuleUpdate reassignSource(@Nullable Boolean reassignSource) {
    this.reassignSource = reassignSource;
    return this;
  }

  /**
   * Reassign the source item when the rule matches.
   * @return reassignSource
   */
  
  @JsonProperty("reassignSource")
  public @Nullable Boolean getReassignSource() {
    return reassignSource;
  }

  @JsonProperty("reassignSource")
  public void setReassignSource(@Nullable Boolean reassignSource) {
    this.reassignSource = reassignSource;
  }

  public ReleaseRuleUpdate sortOrder(@Nullable Integer sortOrder) {
    this.sortOrder = sortOrder;
    return this;
  }

  /**
   * Evaluation order.
   * @return sortOrder
   */
  
  @JsonProperty("sortOrder")
  public @Nullable Integer getSortOrder() {
    return sortOrder;
  }

  @JsonProperty("sortOrder")
  public void setSortOrder(@Nullable Integer sortOrder) {
    this.sortOrder = sortOrder;
  }

  public ReleaseRuleUpdate copiedFromId(@Nullable UUID copiedFromId) {
    this.copiedFromId = copiedFromId;
    return this;
  }

  /**
   * Template rule this cohort copy was made from.
   * @return copiedFromId
   */
  @Valid 
  @JsonProperty("copiedFromId")
  public @Nullable UUID getCopiedFromId() {
    return copiedFromId;
  }

  @JsonProperty("copiedFromId")
  public void setCopiedFromId(@Nullable UUID copiedFromId) {
    this.copiedFromId = copiedFromId;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ReleaseRuleUpdate releaseRuleUpdate = (ReleaseRuleUpdate) o;
    return Objects.equals(this.courseId, releaseRuleUpdate.courseId) &&
        Objects.equals(this.cohortId, releaseRuleUpdate.cohortId) &&
        Objects.equals(this.sourceContentItemId, releaseRuleUpdate.sourceContentItemId) &&
        Objects.equals(this.title, releaseRuleUpdate.title) &&
        Objects.equals(this.stopIfTrue, releaseRuleUpdate.stopIfTrue) &&
        Objects.equals(this.isDisabled, releaseRuleUpdate.isDisabled) &&
        Objects.equals(this.reassignSource, releaseRuleUpdate.reassignSource) &&
        Objects.equals(this.sortOrder, releaseRuleUpdate.sortOrder) &&
        Objects.equals(this.copiedFromId, releaseRuleUpdate.copiedFromId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(courseId, cohortId, sourceContentItemId, title, stopIfTrue, isDisabled, reassignSource, sortOrder, copiedFromId);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ReleaseRuleUpdate {\n");
    sb.append("    courseId: ").append(toIndentedString(courseId)).append("\n");
    sb.append("    cohortId: ").append(toIndentedString(cohortId)).append("\n");
    sb.append("    sourceContentItemId: ").append(toIndentedString(sourceContentItemId)).append("\n");
    sb.append("    title: ").append(toIndentedString(title)).append("\n");
    sb.append("    stopIfTrue: ").append(toIndentedString(stopIfTrue)).append("\n");
    sb.append("    isDisabled: ").append(toIndentedString(isDisabled)).append("\n");
    sb.append("    reassignSource: ").append(toIndentedString(reassignSource)).append("\n");
    sb.append("    sortOrder: ").append(toIndentedString(sortOrder)).append("\n");
    sb.append("    copiedFromId: ").append(toIndentedString(copiedFromId)).append("\n");
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

