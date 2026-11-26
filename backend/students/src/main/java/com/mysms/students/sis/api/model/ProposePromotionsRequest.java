// Owned API contract of sis.
package com.mysms.students.sis.api.model;

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
 * ProposePromotionsRequest
 */

public class ProposePromotionsRequest {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private UUID fromClassSectionId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private UUID toAcademicYearId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private UUID toGradeLevelId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID toSectionId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID basedOnExamSessionId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable List<UUID> studentIds = new ArrayList<>();

  public ProposePromotionsRequest() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public ProposePromotionsRequest(UUID fromClassSectionId, UUID toAcademicYearId, UUID toGradeLevelId) {
    this.fromClassSectionId = fromClassSectionId;
    this.toAcademicYearId = toAcademicYearId;
    this.toGradeLevelId = toGradeLevelId;
  }

  public ProposePromotionsRequest fromClassSectionId(UUID fromClassSectionId) {
    this.fromClassSectionId = fromClassSectionId;
    return this;
  }

  /**
   * Class being promoted.
   * @return fromClassSectionId
   */
  @NotNull @Valid 
  @JsonProperty("fromClassSectionId")
  public UUID getFromClassSectionId() {
    return fromClassSectionId;
  }

  @JsonProperty("fromClassSectionId")
  public void setFromClassSectionId(UUID fromClassSectionId) {
    this.fromClassSectionId = fromClassSectionId;
  }

  public ProposePromotionsRequest toAcademicYearId(UUID toAcademicYearId) {
    this.toAcademicYearId = toAcademicYearId;
    return this;
  }

  /**
   * Next year.
   * @return toAcademicYearId
   */
  @NotNull @Valid 
  @JsonProperty("toAcademicYearId")
  public UUID getToAcademicYearId() {
    return toAcademicYearId;
  }

  @JsonProperty("toAcademicYearId")
  public void setToAcademicYearId(UUID toAcademicYearId) {
    this.toAcademicYearId = toAcademicYearId;
  }

  public ProposePromotionsRequest toGradeLevelId(UUID toGradeLevelId) {
    this.toGradeLevelId = toGradeLevelId;
    return this;
  }

  /**
   * Next grade.
   * @return toGradeLevelId
   */
  @NotNull @Valid 
  @JsonProperty("toGradeLevelId")
  public UUID getToGradeLevelId() {
    return toGradeLevelId;
  }

  @JsonProperty("toGradeLevelId")
  public void setToGradeLevelId(UUID toGradeLevelId) {
    this.toGradeLevelId = toGradeLevelId;
  }

  public ProposePromotionsRequest toSectionId(@Nullable UUID toSectionId) {
    this.toSectionId = toSectionId;
    return this;
  }

  /**
   * Next section (default same name).
   * @return toSectionId
   */
  @Valid 
  @JsonProperty("toSectionId")
  public @Nullable UUID getToSectionId() {
    return toSectionId;
  }

  @JsonProperty("toSectionId")
  public void setToSectionId(@Nullable UUID toSectionId) {
    this.toSectionId = toSectionId;
  }

  public ProposePromotionsRequest basedOnExamSessionId(@Nullable UUID basedOnExamSessionId) {
    this.basedOnExamSessionId = basedOnExamSessionId;
    return this;
  }

  /**
   * Exam series used.
   * @return basedOnExamSessionId
   */
  @Valid 
  @JsonProperty("basedOnExamSessionId")
  public @Nullable UUID getBasedOnExamSessionId() {
    return basedOnExamSessionId;
  }

  @JsonProperty("basedOnExamSessionId")
  public void setBasedOnExamSessionId(@Nullable UUID basedOnExamSessionId) {
    this.basedOnExamSessionId = basedOnExamSessionId;
  }

  public ProposePromotionsRequest studentIds(List<UUID> studentIds) {
    this.studentIds = studentIds;
    return this;
  }

  public ProposePromotionsRequest addStudentIdsItem(UUID studentIdsItem) {
    if (this.studentIds == null) {
      this.studentIds = new ArrayList<>();
    }
    this.studentIds.add(studentIdsItem);
    return this;
  }

  /**
   * Subset (default whole class).
   * @return studentIds
   */
  
  @JsonProperty("studentIds")
  public List<UUID> getStudentIds() {
    return studentIds;
  }

  @JsonProperty("studentIds")
  public void setStudentIds(List<UUID> studentIds) {
    this.studentIds = studentIds;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ProposePromotionsRequest proposePromotionsRequest = (ProposePromotionsRequest) o;
    return Objects.equals(this.fromClassSectionId, proposePromotionsRequest.fromClassSectionId) &&
        Objects.equals(this.toAcademicYearId, proposePromotionsRequest.toAcademicYearId) &&
        Objects.equals(this.toGradeLevelId, proposePromotionsRequest.toGradeLevelId) &&
        Objects.equals(this.toSectionId, proposePromotionsRequest.toSectionId) &&
        Objects.equals(this.basedOnExamSessionId, proposePromotionsRequest.basedOnExamSessionId) &&
        Objects.equals(this.studentIds, proposePromotionsRequest.studentIds);
  }

  @Override
  public int hashCode() {
    return Objects.hash(fromClassSectionId, toAcademicYearId, toGradeLevelId, toSectionId, basedOnExamSessionId, studentIds);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ProposePromotionsRequest {\n");
    sb.append("    fromClassSectionId: ").append(toIndentedString(fromClassSectionId)).append("\n");
    sb.append("    toAcademicYearId: ").append(toIndentedString(toAcademicYearId)).append("\n");
    sb.append("    toGradeLevelId: ").append(toIndentedString(toGradeLevelId)).append("\n");
    sb.append("    toSectionId: ").append(toIndentedString(toSectionId)).append("\n");
    sb.append("    basedOnExamSessionId: ").append(toIndentedString(basedOnExamSessionId)).append("\n");
    sb.append("    studentIds: ").append(toIndentedString(studentIds)).append("\n");
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

