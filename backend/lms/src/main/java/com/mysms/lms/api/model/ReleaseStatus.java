// Owned API contract of lms.
package com.mysms.lms.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.springframework.lang.Nullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;

/**
 * ReleaseStatus
 */

public class ReleaseStatus {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private UUID contentItemId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private Boolean isReleased;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String reason;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private List<String> pendingConditions = new ArrayList<>();

  public ReleaseStatus() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public ReleaseStatus(UUID contentItemId, Boolean isReleased) {
    this.contentItemId = contentItemId;
    this.isReleased = isReleased;
  }

  public ReleaseStatus contentItemId(UUID contentItemId) {
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

  public ReleaseStatus isReleased(Boolean isReleased) {
    this.isReleased = isReleased;
    return this;
  }

  /**
   * Released to the student.
   * @return isReleased
   */
  @NotNull 
  @JsonProperty("isReleased")
  public Boolean getIsReleased() {
    return isReleased;
  }

  @JsonProperty("isReleased")
  public void setIsReleased(Boolean isReleased) {
    this.isReleased = isReleased;
  }

  public ReleaseStatus reason(@Nullable String reason) {
    this.reason = reason;
    return this;
  }

  /**
   * Rule outcome or manual grant.
   * @return reason
   */
  
  @JsonProperty("reason")
  public @Nullable String getReason() {
    return reason;
  }

  @JsonProperty("reason")
  public void setReason(@Nullable String reason) {
    this.reason = reason;
  }

  public ReleaseStatus pendingConditions(List<String> pendingConditions) {
    this.pendingConditions = pendingConditions;
    return this;
  }

  public ReleaseStatus addPendingConditionsItem(String pendingConditionsItem) {
    if (this.pendingConditions == null) {
      this.pendingConditions = new ArrayList<>();
    }
    this.pendingConditions.add(pendingConditionsItem);
    return this;
  }

  /**
   * Conditions not yet met.
   * @return pendingConditions
   */
  
  @JsonProperty("pendingConditions")
  public List<String> getPendingConditions() {
    return pendingConditions;
  }

  @JsonProperty("pendingConditions")
  public void setPendingConditions(List<String> pendingConditions) {
    this.pendingConditions = pendingConditions;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ReleaseStatus releaseStatus = (ReleaseStatus) o;
    return Objects.equals(this.contentItemId, releaseStatus.contentItemId) &&
        Objects.equals(this.isReleased, releaseStatus.isReleased) &&
        Objects.equals(this.reason, releaseStatus.reason) &&
        Objects.equals(this.pendingConditions, releaseStatus.pendingConditions);
  }

  @Override
  public int hashCode() {
    return Objects.hash(contentItemId, isReleased, reason, pendingConditions);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ReleaseStatus {\n");
    sb.append("    contentItemId: ").append(toIndentedString(contentItemId)).append("\n");
    sb.append("    isReleased: ").append(toIndentedString(isReleased)).append("\n");
    sb.append("    reason: ").append(toIndentedString(reason)).append("\n");
    sb.append("    pendingConditions: ").append(toIndentedString(pendingConditions)).append("\n");
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

