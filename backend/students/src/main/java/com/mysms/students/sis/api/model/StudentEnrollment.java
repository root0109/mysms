// Owned API contract of sis.
package com.mysms.students.sis.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.lang.Nullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;

/**
 * Student&#39;s placement in a class section for an academic year (K-12 placement).
 */

public class StudentEnrollment {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  private @Nullable LocalDate effectiveFrom;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  private @Nullable LocalDate effectiveTo;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID previousEnrollmentId;

  /**
   * Why the row exists.
   */
  public enum ChangeReasonEnum {
    INITIAL("initial"),
    
    PROMOTION("promotion"),
    
    TRANSFER_IN("transfer_in"),
    
    SECTION_CHANGE("section_change"),
    
    RE_ADMISSION("re_admission"),
    
    CORRECTION("correction");

    private final String value;

    ChangeReasonEnum(String value) {
      this.value = value;
    }

    @JsonValue
    public String getValue() {
      return value;
    }

    @Override
    public String toString() {
      return String.valueOf(value);
    }

    @JsonCreator
    public static ChangeReasonEnum fromValue(String value) {
      for (ChangeReasonEnum b : ChangeReasonEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable ChangeReasonEnum changeReason;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID id;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private UUID studentId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private UUID academicYearId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID classSectionId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String rollNumber;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String idCardNumber;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String boardRegistrationNumber;

  /**
   * Placement state.
   */
  public enum EnrollmentStatusEnum {
    ENROLLED("enrolled"),
    
    PROMOTED("promoted"),
    
    DETAINED("detained"),
    
    LEFT("left"),
    
    COMPLETED("completed");

    private final String value;

    EnrollmentStatusEnum(String value) {
      this.value = value;
    }

    @JsonValue
    public String getValue() {
      return value;
    }

    @Override
    public String toString() {
      return String.valueOf(value);
    }

    @JsonCreator
    public static EnrollmentStatusEnum fromValue(String value) {
      for (EnrollmentStatusEnum b : EnrollmentStatusEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable EnrollmentStatusEnum enrollmentStatus;

  /**
   * Year-end result.
   */
  public enum ResultStatusEnum {
    PENDING("pending"),
    
    PASS("pass"),
    
    FAIL("fail");

    private final String value;

    ResultStatusEnum(String value) {
      this.value = value;
    }

    @JsonValue
    public String getValue() {
      return value;
    }

    @Override
    public String toString() {
      return String.valueOf(value);
    }

    @JsonCreator
    public static ResultStatusEnum fromValue(String value) {
      for (ResultStatusEnum b : ResultStatusEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable ResultStatusEnum resultStatus;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  private @Nullable LocalDate enrolledOn;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  private @Nullable LocalDate leftOn;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private @Nullable OffsetDateTime createTime;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID createdBy;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private @Nullable OffsetDateTime updateTime;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID updatedBy;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String etag;

  public StudentEnrollment() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public StudentEnrollment(UUID studentId, UUID academicYearId) {
    this.studentId = studentId;
    this.academicYearId = academicYearId;
  }

  public StudentEnrollment effectiveFrom(@Nullable LocalDate effectiveFrom) {
    this.effectiveFrom = effectiveFrom;
    return this;
  }

  /**
   * Day this placement takes effect.
   * @return effectiveFrom
   */
  @Valid 
  @JsonProperty("effectiveFrom")
  public @Nullable LocalDate getEffectiveFrom() {
    return effectiveFrom;
  }

  @JsonProperty("effectiveFrom")
  public void setEffectiveFrom(@Nullable LocalDate effectiveFrom) {
    this.effectiveFrom = effectiveFrom;
  }

  public StudentEnrollment effectiveTo(@Nullable LocalDate effectiveTo) {
    this.effectiveTo = effectiveTo;
    return this;
  }

  /**
   * Day this placement was superseded or closed (NULL = current placement).
   * @return effectiveTo
   */
  @Valid 
  @JsonProperty("effectiveTo")
  public @Nullable LocalDate getEffectiveTo() {
    return effectiveTo;
  }

  @JsonProperty("effectiveTo")
  public void setEffectiveTo(@Nullable LocalDate effectiveTo) {
    this.effectiveTo = effectiveTo;
  }

  public StudentEnrollment previousEnrollmentId(@Nullable UUID previousEnrollmentId) {
    this.previousEnrollmentId = previousEnrollmentId;
    return this;
  }

  /**
   * Placement this row superseded.
   * @return previousEnrollmentId
   */
  @Valid 
  @JsonProperty("previousEnrollmentId")
  public @Nullable UUID getPreviousEnrollmentId() {
    return previousEnrollmentId;
  }

  @JsonProperty("previousEnrollmentId")
  public void setPreviousEnrollmentId(@Nullable UUID previousEnrollmentId) {
    this.previousEnrollmentId = previousEnrollmentId;
  }

  public StudentEnrollment changeReason(@Nullable ChangeReasonEnum changeReason) {
    this.changeReason = changeReason;
    return this;
  }

  /**
   * Why the row exists.
   * @return changeReason
   */
  
  @JsonProperty("changeReason")
  public @Nullable ChangeReasonEnum getChangeReason() {
    return changeReason;
  }

  @JsonProperty("changeReason")
  public void setChangeReason(@Nullable ChangeReasonEnum changeReason) {
    this.changeReason = changeReason;
  }

  public StudentEnrollment id(@Nullable UUID id) {
    this.id = id;
    return this;
  }

  /**
   * Surrogate primary key (UUIDv7 generated by the application; database default is a v4 fallback).
   * @return id
   */
  @Valid 
  @JsonProperty("id")
  public @Nullable UUID getId() {
    return id;
  }

  @JsonProperty("id")
  public void setId(@Nullable UUID id) {
    this.id = id;
  }

  public StudentEnrollment studentId(UUID studentId) {
    this.studentId = studentId;
    return this;
  }

  /**
   * Student.
   * @return studentId
   */
  @NotNull @Valid 
  @JsonProperty("studentId")
  public UUID getStudentId() {
    return studentId;
  }

  @JsonProperty("studentId")
  public void setStudentId(UUID studentId) {
    this.studentId = studentId;
  }

  public StudentEnrollment academicYearId(UUID academicYearId) {
    this.academicYearId = academicYearId;
    return this;
  }

  /**
   * Academic year.
   * @return academicYearId
   */
  @NotNull @Valid 
  @JsonProperty("academicYearId")
  public UUID getAcademicYearId() {
    return academicYearId;
  }

  @JsonProperty("academicYearId")
  public void setAcademicYearId(UUID academicYearId) {
    this.academicYearId = academicYearId;
  }

  public StudentEnrollment classSectionId(@Nullable UUID classSectionId) {
    this.classSectionId = classSectionId;
    return this;
  }

  /**
   * Class section.
   * @return classSectionId
   */
  @Valid 
  @JsonProperty("classSectionId")
  public @Nullable UUID getClassSectionId() {
    return classSectionId;
  }

  @JsonProperty("classSectionId")
  public void setClassSectionId(@Nullable UUID classSectionId) {
    this.classSectionId = classSectionId;
  }

  public StudentEnrollment rollNumber(@Nullable String rollNumber) {
    this.rollNumber = rollNumber;
    return this;
  }

  /**
   * Roll number in the class.
   * @return rollNumber
   */
  @Size(max = 20) 
  @JsonProperty("rollNumber")
  public @Nullable String getRollNumber() {
    return rollNumber;
  }

  @JsonProperty("rollNumber")
  public void setRollNumber(@Nullable String rollNumber) {
    this.rollNumber = rollNumber;
  }

  public StudentEnrollment idCardNumber(@Nullable String idCardNumber) {
    this.idCardNumber = idCardNumber;
    return this;
  }

  /**
   * Identity-card number for the year.
   * @return idCardNumber
   */
  @Size(max = 40) 
  @JsonProperty("idCardNumber")
  public @Nullable String getIdCardNumber() {
    return idCardNumber;
  }

  @JsonProperty("idCardNumber")
  public void setIdCardNumber(@Nullable String idCardNumber) {
    this.idCardNumber = idCardNumber;
  }

  public StudentEnrollment boardRegistrationNumber(@Nullable String boardRegistrationNumber) {
    this.boardRegistrationNumber = boardRegistrationNumber;
    return this;
  }

  /**
   * Examination-board registration number.
   * @return boardRegistrationNumber
   */
  @Size(max = 40) 
  @JsonProperty("boardRegistrationNumber")
  public @Nullable String getBoardRegistrationNumber() {
    return boardRegistrationNumber;
  }

  @JsonProperty("boardRegistrationNumber")
  public void setBoardRegistrationNumber(@Nullable String boardRegistrationNumber) {
    this.boardRegistrationNumber = boardRegistrationNumber;
  }

  public StudentEnrollment enrollmentStatus(@Nullable EnrollmentStatusEnum enrollmentStatus) {
    this.enrollmentStatus = enrollmentStatus;
    return this;
  }

  /**
   * Placement state.
   * @return enrollmentStatus
   */
  
  @JsonProperty("enrollmentStatus")
  public @Nullable EnrollmentStatusEnum getEnrollmentStatus() {
    return enrollmentStatus;
  }

  @JsonProperty("enrollmentStatus")
  public void setEnrollmentStatus(@Nullable EnrollmentStatusEnum enrollmentStatus) {
    this.enrollmentStatus = enrollmentStatus;
  }

  public StudentEnrollment resultStatus(@Nullable ResultStatusEnum resultStatus) {
    this.resultStatus = resultStatus;
    return this;
  }

  /**
   * Year-end result.
   * @return resultStatus
   */
  
  @JsonProperty("resultStatus")
  public @Nullable ResultStatusEnum getResultStatus() {
    return resultStatus;
  }

  @JsonProperty("resultStatus")
  public void setResultStatus(@Nullable ResultStatusEnum resultStatus) {
    this.resultStatus = resultStatus;
  }

  public StudentEnrollment enrolledOn(@Nullable LocalDate enrolledOn) {
    this.enrolledOn = enrolledOn;
    return this;
  }

  /**
   * Placement date.
   * @return enrolledOn
   */
  @Valid 
  @JsonProperty("enrolledOn")
  public @Nullable LocalDate getEnrolledOn() {
    return enrolledOn;
  }

  @JsonProperty("enrolledOn")
  public void setEnrolledOn(@Nullable LocalDate enrolledOn) {
    this.enrolledOn = enrolledOn;
  }

  public StudentEnrollment leftOn(@Nullable LocalDate leftOn) {
    this.leftOn = leftOn;
    return this;
  }

  /**
   * Date the placement ended.
   * @return leftOn
   */
  @Valid 
  @JsonProperty("leftOn")
  public @Nullable LocalDate getLeftOn() {
    return leftOn;
  }

  @JsonProperty("leftOn")
  public void setLeftOn(@Nullable LocalDate leftOn) {
    this.leftOn = leftOn;
  }

  public StudentEnrollment createTime(@Nullable OffsetDateTime createTime) {
    this.createTime = createTime;
    return this;
  }

  /**
   * Row creation time.
   * @return createTime
   */
  @Valid 
  @JsonProperty("createTime")
  public @Nullable OffsetDateTime getCreateTime() {
    return createTime;
  }

  @JsonProperty("createTime")
  public void setCreateTime(@Nullable OffsetDateTime createTime) {
    this.createTime = createTime;
  }

  public StudentEnrollment createdBy(@Nullable UUID createdBy) {
    this.createdBy = createdBy;
    return this;
  }

  /**
   * User account that created the row (audit reference to iam.user_account; not FK-constrained).
   * @return createdBy
   */
  @Valid 
  @JsonProperty("createdBy")
  public @Nullable UUID getCreatedBy() {
    return createdBy;
  }

  @JsonProperty("createdBy")
  public void setCreatedBy(@Nullable UUID createdBy) {
    this.createdBy = createdBy;
  }

  public StudentEnrollment updateTime(@Nullable OffsetDateTime updateTime) {
    this.updateTime = updateTime;
    return this;
  }

  /**
   * Last update time.
   * @return updateTime
   */
  @Valid 
  @JsonProperty("updateTime")
  public @Nullable OffsetDateTime getUpdateTime() {
    return updateTime;
  }

  @JsonProperty("updateTime")
  public void setUpdateTime(@Nullable OffsetDateTime updateTime) {
    this.updateTime = updateTime;
  }

  public StudentEnrollment updatedBy(@Nullable UUID updatedBy) {
    this.updatedBy = updatedBy;
    return this;
  }

  /**
   * User account that last updated the row (audit reference to iam.user_account; not FK-constrained).
   * @return updatedBy
   */
  @Valid 
  @JsonProperty("updatedBy")
  public @Nullable UUID getUpdatedBy() {
    return updatedBy;
  }

  @JsonProperty("updatedBy")
  public void setUpdatedBy(@Nullable UUID updatedBy) {
    this.updatedBy = updatedBy;
  }

  public StudentEnrollment etag(@Nullable String etag) {
    this.etag = etag;
    return this;
  }

  /**
   * Entity tag (row version); send back in If-Match / etag.
   * @return etag
   */
  
  @JsonProperty("etag")
  public @Nullable String getEtag() {
    return etag;
  }

  @JsonProperty("etag")
  public void setEtag(@Nullable String etag) {
    this.etag = etag;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    StudentEnrollment studentEnrollment = (StudentEnrollment) o;
    return Objects.equals(this.effectiveFrom, studentEnrollment.effectiveFrom) &&
        Objects.equals(this.effectiveTo, studentEnrollment.effectiveTo) &&
        Objects.equals(this.previousEnrollmentId, studentEnrollment.previousEnrollmentId) &&
        Objects.equals(this.changeReason, studentEnrollment.changeReason) &&
        Objects.equals(this.id, studentEnrollment.id) &&
        Objects.equals(this.studentId, studentEnrollment.studentId) &&
        Objects.equals(this.academicYearId, studentEnrollment.academicYearId) &&
        Objects.equals(this.classSectionId, studentEnrollment.classSectionId) &&
        Objects.equals(this.rollNumber, studentEnrollment.rollNumber) &&
        Objects.equals(this.idCardNumber, studentEnrollment.idCardNumber) &&
        Objects.equals(this.boardRegistrationNumber, studentEnrollment.boardRegistrationNumber) &&
        Objects.equals(this.enrollmentStatus, studentEnrollment.enrollmentStatus) &&
        Objects.equals(this.resultStatus, studentEnrollment.resultStatus) &&
        Objects.equals(this.enrolledOn, studentEnrollment.enrolledOn) &&
        Objects.equals(this.leftOn, studentEnrollment.leftOn) &&
        Objects.equals(this.createTime, studentEnrollment.createTime) &&
        Objects.equals(this.createdBy, studentEnrollment.createdBy) &&
        Objects.equals(this.updateTime, studentEnrollment.updateTime) &&
        Objects.equals(this.updatedBy, studentEnrollment.updatedBy) &&
        Objects.equals(this.etag, studentEnrollment.etag);
  }

  @Override
  public int hashCode() {
    return Objects.hash(effectiveFrom, effectiveTo, previousEnrollmentId, changeReason, id, studentId, academicYearId, classSectionId, rollNumber, idCardNumber, boardRegistrationNumber, enrollmentStatus, resultStatus, enrolledOn, leftOn, createTime, createdBy, updateTime, updatedBy, etag);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class StudentEnrollment {\n");
    sb.append("    effectiveFrom: ").append(toIndentedString(effectiveFrom)).append("\n");
    sb.append("    effectiveTo: ").append(toIndentedString(effectiveTo)).append("\n");
    sb.append("    previousEnrollmentId: ").append(toIndentedString(previousEnrollmentId)).append("\n");
    sb.append("    changeReason: ").append(toIndentedString(changeReason)).append("\n");
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    studentId: ").append(toIndentedString(studentId)).append("\n");
    sb.append("    academicYearId: ").append(toIndentedString(academicYearId)).append("\n");
    sb.append("    classSectionId: ").append(toIndentedString(classSectionId)).append("\n");
    sb.append("    rollNumber: ").append(toIndentedString(rollNumber)).append("\n");
    sb.append("    idCardNumber: ").append(toIndentedString(idCardNumber)).append("\n");
    sb.append("    boardRegistrationNumber: ").append(toIndentedString(boardRegistrationNumber)).append("\n");
    sb.append("    enrollmentStatus: ").append(toIndentedString(enrollmentStatus)).append("\n");
    sb.append("    resultStatus: ").append(toIndentedString(resultStatus)).append("\n");
    sb.append("    enrolledOn: ").append(toIndentedString(enrolledOn)).append("\n");
    sb.append("    leftOn: ").append(toIndentedString(leftOn)).append("\n");
    sb.append("    createTime: ").append(toIndentedString(createTime)).append("\n");
    sb.append("    createdBy: ").append(toIndentedString(createdBy)).append("\n");
    sb.append("    updateTime: ").append(toIndentedString(updateTime)).append("\n");
    sb.append("    updatedBy: ").append(toIndentedString(updatedBy)).append("\n");
    sb.append("    etag: ").append(toIndentedString(etag)).append("\n");
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

