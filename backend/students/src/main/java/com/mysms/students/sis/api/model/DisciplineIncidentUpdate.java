// Owned API contract of sis.
package com.mysms.students.sis.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.lang.Nullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;

/**
 * Fields of DisciplineIncident that can be updated (PATCH: a property left out keeps its value, null clears it, a value sets it).
 */

public class DisciplineIncidentUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull UUID> institutionId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull UUID> academicYearId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<UUID> classSectionId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull UUID> studentId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private @Nullable Optional<@NotNull OffsetDateTime> incidentAt;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull UUID> reportedByUserId;

  /**
   * Type.
   */
  public enum IncidentTypeEnum {
    DISCIPLINE("discipline"),
    
    PERFORMANCE("performance"),
    
    OTHER("other");

    private final String value;

    IncidentTypeEnum(String value) {
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
    public static IncidentTypeEnum fromValue(String value) {
      for (IncidentTypeEnum b : IncidentTypeEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull IncidentTypeEnum> incidentType;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull String> details;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<String> response;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull Boolean> actionTaken;

  /**
   * Type of evidence attached.
   */
  public enum EvidenceMediaTypeEnum {
    VIDEO("video"),
    
    AUDIO("audio"),
    
    IMAGE("image");

    private final String value;

    EvidenceMediaTypeEnum(String value) {
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
    public static EvidenceMediaTypeEnum fromValue(String value) {
      for (EvidenceMediaTypeEnum b : EvidenceMediaTypeEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<EvidenceMediaTypeEnum> evidenceMediaType;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<UUID> evidenceFileId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull Boolean> notifyGuardians;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull Boolean> seenByGuardian;

  public DisciplineIncidentUpdate institutionId(@Nullable Optional<UUID> institutionId) {
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

  public DisciplineIncidentUpdate academicYearId(@Nullable Optional<UUID> academicYearId) {
    this.academicYearId = academicYearId;
    return this;
  }

  /**
   * Academic year.
   * @return academicYearId
   */
  
  @JsonProperty("academicYearId")
  public @Nullable Optional<UUID> getAcademicYearId() {
    return academicYearId;
  }

  @JsonProperty("academicYearId")
  public void setAcademicYearId(@Nullable Optional<UUID> academicYearId) {
    this.academicYearId = academicYearId;
  }

  public DisciplineIncidentUpdate classSectionId(@Nullable Optional<UUID> classSectionId) {
    this.classSectionId = classSectionId;
    return this;
  }

  /**
   * Student's class at the time.
   * @return classSectionId
   */
  
  @JsonProperty("classSectionId")
  public @Nullable Optional<UUID> getClassSectionId() {
    return classSectionId;
  }

  @JsonProperty("classSectionId")
  public void setClassSectionId(@Nullable Optional<UUID> classSectionId) {
    this.classSectionId = classSectionId;
  }

  public DisciplineIncidentUpdate studentId(@Nullable Optional<UUID> studentId) {
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

  public DisciplineIncidentUpdate incidentAt(@Nullable Optional<OffsetDateTime> incidentAt) {
    this.incidentAt = incidentAt;
    return this;
  }

  /**
   * When it happened.
   * @return incidentAt
   */
  
  @JsonProperty("incidentAt")
  public @Nullable Optional<OffsetDateTime> getIncidentAt() {
    return incidentAt;
  }

  @JsonProperty("incidentAt")
  public void setIncidentAt(@Nullable Optional<OffsetDateTime> incidentAt) {
    this.incidentAt = incidentAt;
  }

  public DisciplineIncidentUpdate reportedByUserId(@Nullable Optional<UUID> reportedByUserId) {
    this.reportedByUserId = reportedByUserId;
    return this;
  }

  /**
   * Reporter.
   * @return reportedByUserId
   */
  
  @JsonProperty("reportedByUserId")
  public @Nullable Optional<UUID> getReportedByUserId() {
    return reportedByUserId;
  }

  @JsonProperty("reportedByUserId")
  public void setReportedByUserId(@Nullable Optional<UUID> reportedByUserId) {
    this.reportedByUserId = reportedByUserId;
  }

  public DisciplineIncidentUpdate incidentType(@Nullable Optional<IncidentTypeEnum> incidentType) {
    this.incidentType = incidentType;
    return this;
  }

  /**
   * Type.
   * @return incidentType
   */
  
  @JsonProperty("incidentType")
  public @Nullable Optional<IncidentTypeEnum> getIncidentType() {
    return incidentType;
  }

  @JsonProperty("incidentType")
  public void setIncidentType(@Nullable Optional<IncidentTypeEnum> incidentType) {
    this.incidentType = incidentType;
  }

  public DisciplineIncidentUpdate details(@Nullable Optional<String> details) {
    this.details = details;
    return this;
  }

  /**
   * Description.
   * @return details
   */
  
  @JsonProperty("details")
  public @Nullable Optional<String> getDetails() {
    return details;
  }

  @JsonProperty("details")
  public void setDetails(@Nullable Optional<String> details) {
    this.details = details;
  }

  public DisciplineIncidentUpdate response(@Nullable Optional<String> response) {
    this.response = response;
    return this;
  }

  /**
   * Student / guardian response.
   * @return response
   */
  
  @JsonProperty("response")
  public @Nullable Optional<String> getResponse() {
    return response;
  }

  @JsonProperty("response")
  public void setResponse(@Nullable Optional<String> response) {
    this.response = response;
  }

  public DisciplineIncidentUpdate actionTaken(@Nullable Optional<Boolean> actionTaken) {
    this.actionTaken = actionTaken;
    return this;
  }

  /**
   * Whether action has been taken.
   * @return actionTaken
   */
  
  @JsonProperty("actionTaken")
  public @Nullable Optional<Boolean> getActionTaken() {
    return actionTaken;
  }

  @JsonProperty("actionTaken")
  public void setActionTaken(@Nullable Optional<Boolean> actionTaken) {
    this.actionTaken = actionTaken;
  }

  public DisciplineIncidentUpdate evidenceMediaType(@Nullable Optional<EvidenceMediaTypeEnum> evidenceMediaType) {
    this.evidenceMediaType = evidenceMediaType;
    return this;
  }

  /**
   * Type of evidence attached.
   * @return evidenceMediaType
   */
  
  @JsonProperty("evidenceMediaType")
  public @Nullable Optional<EvidenceMediaTypeEnum> getEvidenceMediaType() {
    return evidenceMediaType;
  }

  @JsonProperty("evidenceMediaType")
  public void setEvidenceMediaType(@Nullable Optional<EvidenceMediaTypeEnum> evidenceMediaType) {
    this.evidenceMediaType = evidenceMediaType;
  }

  public DisciplineIncidentUpdate evidenceFileId(@Nullable Optional<UUID> evidenceFileId) {
    this.evidenceFileId = evidenceFileId;
    return this;
  }

  /**
   * Evidence file.
   * @return evidenceFileId
   */
  
  @JsonProperty("evidenceFileId")
  public @Nullable Optional<UUID> getEvidenceFileId() {
    return evidenceFileId;
  }

  @JsonProperty("evidenceFileId")
  public void setEvidenceFileId(@Nullable Optional<UUID> evidenceFileId) {
    this.evidenceFileId = evidenceFileId;
  }

  public DisciplineIncidentUpdate notifyGuardians(@Nullable Optional<Boolean> notifyGuardians) {
    this.notifyGuardians = notifyGuardians;
    return this;
  }

  /**
   * Guardians are notified.
   * @return notifyGuardians
   */
  
  @JsonProperty("notifyGuardians")
  public @Nullable Optional<Boolean> getNotifyGuardians() {
    return notifyGuardians;
  }

  @JsonProperty("notifyGuardians")
  public void setNotifyGuardians(@Nullable Optional<Boolean> notifyGuardians) {
    this.notifyGuardians = notifyGuardians;
  }

  public DisciplineIncidentUpdate seenByGuardian(@Nullable Optional<Boolean> seenByGuardian) {
    this.seenByGuardian = seenByGuardian;
    return this;
  }

  /**
   * Guardian has viewed it.
   * @return seenByGuardian
   */
  
  @JsonProperty("seenByGuardian")
  public @Nullable Optional<Boolean> getSeenByGuardian() {
    return seenByGuardian;
  }

  @JsonProperty("seenByGuardian")
  public void setSeenByGuardian(@Nullable Optional<Boolean> seenByGuardian) {
    this.seenByGuardian = seenByGuardian;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    DisciplineIncidentUpdate disciplineIncidentUpdate = (DisciplineIncidentUpdate) o;
    return Objects.equals(this.institutionId, disciplineIncidentUpdate.institutionId) &&
        Objects.equals(this.academicYearId, disciplineIncidentUpdate.academicYearId) &&
        Objects.equals(this.classSectionId, disciplineIncidentUpdate.classSectionId) &&
        Objects.equals(this.studentId, disciplineIncidentUpdate.studentId) &&
        Objects.equals(this.incidentAt, disciplineIncidentUpdate.incidentAt) &&
        Objects.equals(this.reportedByUserId, disciplineIncidentUpdate.reportedByUserId) &&
        Objects.equals(this.incidentType, disciplineIncidentUpdate.incidentType) &&
        Objects.equals(this.details, disciplineIncidentUpdate.details) &&
        Objects.equals(this.response, disciplineIncidentUpdate.response) &&
        Objects.equals(this.actionTaken, disciplineIncidentUpdate.actionTaken) &&
        Objects.equals(this.evidenceMediaType, disciplineIncidentUpdate.evidenceMediaType) &&
        Objects.equals(this.evidenceFileId, disciplineIncidentUpdate.evidenceFileId) &&
        Objects.equals(this.notifyGuardians, disciplineIncidentUpdate.notifyGuardians) &&
        Objects.equals(this.seenByGuardian, disciplineIncidentUpdate.seenByGuardian);
  }

  @Override
  public int hashCode() {
    return Objects.hash(institutionId, academicYearId, classSectionId, studentId, incidentAt, reportedByUserId, incidentType, details, response, actionTaken, evidenceMediaType, evidenceFileId, notifyGuardians, seenByGuardian);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class DisciplineIncidentUpdate {\n");
    sb.append("    institutionId: ").append(toIndentedString(institutionId)).append("\n");
    sb.append("    academicYearId: ").append(toIndentedString(academicYearId)).append("\n");
    sb.append("    classSectionId: ").append(toIndentedString(classSectionId)).append("\n");
    sb.append("    studentId: ").append(toIndentedString(studentId)).append("\n");
    sb.append("    incidentAt: ").append(toIndentedString(incidentAt)).append("\n");
    sb.append("    reportedByUserId: ").append(toIndentedString(reportedByUserId)).append("\n");
    sb.append("    incidentType: ").append(toIndentedString(incidentType)).append("\n");
    sb.append("    details: ").append(toIndentedString(details)).append("\n");
    sb.append("    response: ").append(toIndentedString(response)).append("\n");
    sb.append("    actionTaken: ").append(toIndentedString(actionTaken)).append("\n");
    sb.append("    evidenceMediaType: ").append(toIndentedString(evidenceMediaType)).append("\n");
    sb.append("    evidenceFileId: ").append(toIndentedString(evidenceFileId)).append("\n");
    sb.append("    notifyGuardians: ").append(toIndentedString(notifyGuardians)).append("\n");
    sb.append("    seenByGuardian: ").append(toIndentedString(seenByGuardian)).append("\n");
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

