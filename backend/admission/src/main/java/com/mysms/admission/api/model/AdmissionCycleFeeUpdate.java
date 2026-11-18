// Owned API contract of admission.
package com.mysms.admission.api.model;

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
 * Fields of AdmissionCycleFee that can be updated (partial update; see updateMask).
 */

public class AdmissionCycleFeeUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID courseId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID feeHeadId;

  public AdmissionCycleFeeUpdate courseId(@Nullable UUID courseId) {
    this.courseId = courseId;
    return this;
  }

  /**
   * Course.
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

  public AdmissionCycleFeeUpdate feeHeadId(@Nullable UUID feeHeadId) {
    this.feeHeadId = feeHeadId;
    return this;
  }

  /**
   * Fee head.
   * @return feeHeadId
   */
  @Valid 
  @JsonProperty("feeHeadId")
  public @Nullable UUID getFeeHeadId() {
    return feeHeadId;
  }

  @JsonProperty("feeHeadId")
  public void setFeeHeadId(@Nullable UUID feeHeadId) {
    this.feeHeadId = feeHeadId;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    AdmissionCycleFeeUpdate admissionCycleFeeUpdate = (AdmissionCycleFeeUpdate) o;
    return Objects.equals(this.courseId, admissionCycleFeeUpdate.courseId) &&
        Objects.equals(this.feeHeadId, admissionCycleFeeUpdate.feeHeadId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(courseId, feeHeadId);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class AdmissionCycleFeeUpdate {\n");
    sb.append("    courseId: ").append(toIndentedString(courseId)).append("\n");
    sb.append("    feeHeadId: ").append(toIndentedString(feeHeadId)).append("\n");
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

