// Owned API contract of lms.
package com.mysms.lms.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.lang.Nullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;

/**
 * CopyCourseOutlineToCohortRequest
 */

public class CopyCourseOutlineToCohortRequest {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID courseId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable List<UUID> chapterIds = new ArrayList<>();

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  private @Nullable LocalDate shiftDatesFrom;

  public CopyCourseOutlineToCohortRequest() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public CopyCourseOutlineToCohortRequest(UUID courseId) {
    this.courseId = courseId;
  }

  public CopyCourseOutlineToCohortRequest courseId(UUID courseId) {
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
  public void setCourseId(UUID courseId) {
    this.courseId = courseId;
  }

  public CopyCourseOutlineToCohortRequest chapterIds(List<UUID> chapterIds) {
    this.chapterIds = chapterIds;
    return this;
  }

  public CopyCourseOutlineToCohortRequest addChapterIdsItem(UUID chapterIdsItem) {
    if (this.chapterIds == null) {
      this.chapterIds = new ArrayList<>();
    }
    this.chapterIds.add(chapterIdsItem);
    return this;
  }

  /**
   * Only these chapters (default all).
   * @return chapterIds
   */
  
  @JsonProperty("chapterIds")
  public List<UUID> getChapterIds() {
    return chapterIds;
  }

  @JsonProperty("chapterIds")
  public void setChapterIds(List<UUID> chapterIds) {
    this.chapterIds = chapterIds;
  }

  public CopyCourseOutlineToCohortRequest shiftDatesFrom(@Nullable LocalDate shiftDatesFrom) {
    this.shiftDatesFrom = shiftDatesFrom;
    return this;
  }

  /**
   * Cohort day 1 used to turn template day offsets into dates (default start_date).
   * @return shiftDatesFrom
   */
  @Valid 
  @JsonProperty("shiftDatesFrom")
  public @Nullable LocalDate getShiftDatesFrom() {
    return shiftDatesFrom;
  }

  @JsonProperty("shiftDatesFrom")
  public void setShiftDatesFrom(@Nullable LocalDate shiftDatesFrom) {
    this.shiftDatesFrom = shiftDatesFrom;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    CopyCourseOutlineToCohortRequest copyCourseOutlineToCohortRequest = (CopyCourseOutlineToCohortRequest) o;
    return Objects.equals(this.courseId, copyCourseOutlineToCohortRequest.courseId) &&
        Objects.equals(this.chapterIds, copyCourseOutlineToCohortRequest.chapterIds) &&
        Objects.equals(this.shiftDatesFrom, copyCourseOutlineToCohortRequest.shiftDatesFrom);
  }

  @Override
  public int hashCode() {
    return Objects.hash(courseId, chapterIds, shiftDatesFrom);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class CopyCourseOutlineToCohortRequest {\n");
    sb.append("    courseId: ").append(toIndentedString(courseId)).append("\n");
    sb.append("    chapterIds: ").append(toIndentedString(chapterIds)).append("\n");
    sb.append("    shiftDatesFrom: ").append(toIndentedString(shiftDatesFrom)).append("\n");
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

