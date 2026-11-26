// Owned API contract of sis.
package com.mysms.students.sis.api.model;

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
 * Fields of Promotion that can be updated (PATCH: a property left out keeps its value, null clears it, a value sets it).
 */

public class PromotionUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull UUID> studentId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull UUID> institutionId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull UUID> fromAcademicYearId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull UUID> fromGradeLevelId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<UUID> fromSectionId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<UUID> toAcademicYearId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<UUID> toGradeLevelId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<UUID> toSectionId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<UUID> basedOnExamSessionId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<String> remarks;

  public PromotionUpdate studentId(@Nullable Optional<UUID> studentId) {
    this.studentId = studentId;
    return this;
  }

  /**
   * Student.
   * @return studentId
   */
  
  @JsonProperty("studentId")
  public @Nullable Optional<UUID> getStudentId() {
    return studentId;
  }

  @JsonProperty("studentId")
  public void setStudentId(@Nullable Optional<UUID> studentId) {
    this.studentId = studentId;
  }

  public PromotionUpdate institutionId(@Nullable Optional<UUID> institutionId) {
    this.institutionId = institutionId;
    return this;
  }

  /**
   * Institution.
   * @return institutionId
   */
  
  @JsonProperty("institutionId")
  public @Nullable Optional<UUID> getInstitutionId() {
    return institutionId;
  }

  @JsonProperty("institutionId")
  public void setInstitutionId(@Nullable Optional<UUID> institutionId) {
    this.institutionId = institutionId;
  }

  public PromotionUpdate fromAcademicYearId(@Nullable Optional<UUID> fromAcademicYearId) {
    this.fromAcademicYearId = fromAcademicYearId;
    return this;
  }

  /**
   * Current year.
   * @return fromAcademicYearId
   */
  
  @JsonProperty("fromAcademicYearId")
  public @Nullable Optional<UUID> getFromAcademicYearId() {
    return fromAcademicYearId;
  }

  @JsonProperty("fromAcademicYearId")
  public void setFromAcademicYearId(@Nullable Optional<UUID> fromAcademicYearId) {
    this.fromAcademicYearId = fromAcademicYearId;
  }

  public PromotionUpdate fromGradeLevelId(@Nullable Optional<UUID> fromGradeLevelId) {
    this.fromGradeLevelId = fromGradeLevelId;
    return this;
  }

  /**
   * Current grade.
   * @return fromGradeLevelId
   */
  
  @JsonProperty("fromGradeLevelId")
  public @Nullable Optional<UUID> getFromGradeLevelId() {
    return fromGradeLevelId;
  }

  @JsonProperty("fromGradeLevelId")
  public void setFromGradeLevelId(@Nullable Optional<UUID> fromGradeLevelId) {
    this.fromGradeLevelId = fromGradeLevelId;
  }

  public PromotionUpdate fromSectionId(@Nullable Optional<UUID> fromSectionId) {
    this.fromSectionId = fromSectionId;
    return this;
  }

  /**
   * Current section.
   * @return fromSectionId
   */
  
  @JsonProperty("fromSectionId")
  public @Nullable Optional<UUID> getFromSectionId() {
    return fromSectionId;
  }

  @JsonProperty("fromSectionId")
  public void setFromSectionId(@Nullable Optional<UUID> fromSectionId) {
    this.fromSectionId = fromSectionId;
  }

  public PromotionUpdate toAcademicYearId(@Nullable Optional<UUID> toAcademicYearId) {
    this.toAcademicYearId = toAcademicYearId;
    return this;
  }

  /**
   * Next year.
   * @return toAcademicYearId
   */
  
  @JsonProperty("toAcademicYearId")
  public @Nullable Optional<UUID> getToAcademicYearId() {
    return toAcademicYearId;
  }

  @JsonProperty("toAcademicYearId")
  public void setToAcademicYearId(@Nullable Optional<UUID> toAcademicYearId) {
    this.toAcademicYearId = toAcademicYearId;
  }

  public PromotionUpdate toGradeLevelId(@Nullable Optional<UUID> toGradeLevelId) {
    this.toGradeLevelId = toGradeLevelId;
    return this;
  }

  /**
   * Next grade.
   * @return toGradeLevelId
   */
  
  @JsonProperty("toGradeLevelId")
  public @Nullable Optional<UUID> getToGradeLevelId() {
    return toGradeLevelId;
  }

  @JsonProperty("toGradeLevelId")
  public void setToGradeLevelId(@Nullable Optional<UUID> toGradeLevelId) {
    this.toGradeLevelId = toGradeLevelId;
  }

  public PromotionUpdate toSectionId(@Nullable Optional<UUID> toSectionId) {
    this.toSectionId = toSectionId;
    return this;
  }

  /**
   * Next section.
   * @return toSectionId
   */
  
  @JsonProperty("toSectionId")
  public @Nullable Optional<UUID> getToSectionId() {
    return toSectionId;
  }

  @JsonProperty("toSectionId")
  public void setToSectionId(@Nullable Optional<UUID> toSectionId) {
    this.toSectionId = toSectionId;
  }

  public PromotionUpdate basedOnExamSessionId(@Nullable Optional<UUID> basedOnExamSessionId) {
    this.basedOnExamSessionId = basedOnExamSessionId;
    return this;
  }

  /**
   * Exam series the decision is based on.
   * @return basedOnExamSessionId
   */
  
  @JsonProperty("basedOnExamSessionId")
  public @Nullable Optional<UUID> getBasedOnExamSessionId() {
    return basedOnExamSessionId;
  }

  @JsonProperty("basedOnExamSessionId")
  public void setBasedOnExamSessionId(@Nullable Optional<UUID> basedOnExamSessionId) {
    this.basedOnExamSessionId = basedOnExamSessionId;
  }

  public PromotionUpdate remarks(@Nullable Optional<String> remarks) {
    this.remarks = remarks;
    return this;
  }

  /**
   * Comments.
   * @return remarks
   */
  
  @JsonProperty("remarks")
  public @Nullable Optional<String> getRemarks() {
    return remarks;
  }

  @JsonProperty("remarks")
  public void setRemarks(@Nullable Optional<String> remarks) {
    this.remarks = remarks;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    PromotionUpdate promotionUpdate = (PromotionUpdate) o;
    return Objects.equals(this.studentId, promotionUpdate.studentId) &&
        Objects.equals(this.institutionId, promotionUpdate.institutionId) &&
        Objects.equals(this.fromAcademicYearId, promotionUpdate.fromAcademicYearId) &&
        Objects.equals(this.fromGradeLevelId, promotionUpdate.fromGradeLevelId) &&
        Objects.equals(this.fromSectionId, promotionUpdate.fromSectionId) &&
        Objects.equals(this.toAcademicYearId, promotionUpdate.toAcademicYearId) &&
        Objects.equals(this.toGradeLevelId, promotionUpdate.toGradeLevelId) &&
        Objects.equals(this.toSectionId, promotionUpdate.toSectionId) &&
        Objects.equals(this.basedOnExamSessionId, promotionUpdate.basedOnExamSessionId) &&
        Objects.equals(this.remarks, promotionUpdate.remarks);
  }

  @Override
  public int hashCode() {
    return Objects.hash(studentId, institutionId, fromAcademicYearId, fromGradeLevelId, fromSectionId, toAcademicYearId, toGradeLevelId, toSectionId, basedOnExamSessionId, remarks);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class PromotionUpdate {\n");
    sb.append("    studentId: ").append(toIndentedString(studentId)).append("\n");
    sb.append("    institutionId: ").append(toIndentedString(institutionId)).append("\n");
    sb.append("    fromAcademicYearId: ").append(toIndentedString(fromAcademicYearId)).append("\n");
    sb.append("    fromGradeLevelId: ").append(toIndentedString(fromGradeLevelId)).append("\n");
    sb.append("    fromSectionId: ").append(toIndentedString(fromSectionId)).append("\n");
    sb.append("    toAcademicYearId: ").append(toIndentedString(toAcademicYearId)).append("\n");
    sb.append("    toGradeLevelId: ").append(toIndentedString(toGradeLevelId)).append("\n");
    sb.append("    toSectionId: ").append(toIndentedString(toSectionId)).append("\n");
    sb.append("    basedOnExamSessionId: ").append(toIndentedString(basedOnExamSessionId)).append("\n");
    sb.append("    remarks: ").append(toIndentedString(remarks)).append("\n");
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

