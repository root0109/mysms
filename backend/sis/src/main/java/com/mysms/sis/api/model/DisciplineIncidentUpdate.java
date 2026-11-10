// Owned API contract of sis (JAVA-CODING-RULES §7); first materialized from api/openapi/sis.yaml on 2026-11-10.
package com.mysms.sis.api.model;

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
 * Fields of DisciplineIncident that can be updated (partial update; see updateMask).
 */

public class DisciplineIncidentUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID institutionId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID academicYearId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID classSectionId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID studentId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private @Nullable OffsetDateTime incidentAt;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID reportedByUserId;

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
  private @Nullable IncidentTypeEnum incidentType;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String details;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String response;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Boolean actionTaken;

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
  private @Nullable EvidenceMediaTypeEnum evidenceMediaType;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID evidenceFileId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Boolean notifyGuardians;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Boolean seenByGuardian;

  public DisciplineIncidentUpdate institutionId(@Nullable UUID institutionId) {
    this.institutionId = institutionId;
    return this;
  }

  /**
   * Institution.
   * @return institutionId
   */
  @Valid 
  @JsonProperty("institutionId")
  public @Nullable UUID getInstitutionId() {
    return institutionId;
  }

  @JsonProperty("institutionId")
  public void setInstitutionId(@Nullable UUID institutionId) {
    this.institutionId = institutionId;
  }

  public DisciplineIncidentUpdate academicYearId(@Nullable UUID academicYearId) {
    this.academicYearId = academicYearId;
    return this;
  }

  /**
   * Academic year.
   * @return academicYearId
   */
  @Valid 
  @JsonProperty("academicYearId")
  public @Nullable UUID getAcademicYearId() {
    return academicYearId;
  }

  @JsonProperty("academicYearId")
  public void setAcademicYearId(@Nullable UUID academicYearId) {
    this.academicYearId = academicYearId;
  }

  public DisciplineIncidentUpdate classSectionId(@Nullable UUID classSectionId) {
    this.classSectionId = classSectionId;
    return this;
  }

  /**
   * Student's class at the time.
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

  public DisciplineIncidentUpdate studentId(@Nullable UUID studentId) {
    this.studentId = studentId;
    return this;
  }

  /**
   * Student.
   * @return studentId
   */
  @Valid 
  @JsonProperty("studentId")
  public @Nullable UUID getStudentId() {
    return studentId;
  }

  @JsonProperty("studentId")
  public void setStudentId(@Nullable UUID studentId) {
    this.studentId = studentId;
  }

  public DisciplineIncidentUpdate incidentAt(@Nullable OffsetDateTime incidentAt) {
    this.incidentAt = incidentAt;
    return this;
  }

  /**
   * When it happened.
   * @return incidentAt
   */
  @Valid 
  @JsonProperty("incidentAt")
  public @Nullable OffsetDateTime getIncidentAt() {
    return incidentAt;
  }

  @JsonProperty("incidentAt")
  public void setIncidentAt(@Nullable OffsetDateTime incidentAt) {
    this.incidentAt = incidentAt;
  }

  public DisciplineIncidentUpdate reportedByUserId(@Nullable UUID reportedByUserId) {
    this.reportedByUserId = reportedByUserId;
    return this;
  }

  /**
   * Reporter.
   * @return reportedByUserId
   */
  @Valid 
  @JsonProperty("reportedByUserId")
  public @Nullable UUID getReportedByUserId() {
    return reportedByUserId;
  }

  @JsonProperty("reportedByUserId")
  public void setReportedByUserId(@Nullable UUID reportedByUserId) {
    this.reportedByUserId = reportedByUserId;
  }

  public DisciplineIncidentUpdate incidentType(@Nullable IncidentTypeEnum incidentType) {
    this.incidentType = incidentType;
    return this;
  }

  /**
   * Type.
   * @return incidentType
   */
  
  @JsonProperty("incidentType")
  public @Nullable IncidentTypeEnum getIncidentType() {
    return incidentType;
  }

  @JsonProperty("incidentType")
  public void setIncidentType(@Nullable IncidentTypeEnum incidentType) {
    this.incidentType = incidentType;
  }

  public DisciplineIncidentUpdate details(@Nullable String details) {
    this.details = details;
    return this;
  }

  /**
   * Description.
   * @return details
   */
  
  @JsonProperty("details")
  public @Nullable String getDetails() {
    return details;
  }

  @JsonProperty("details")
  public void setDetails(@Nullable String details) {
    this.details = details;
  }

  public DisciplineIncidentUpdate response(@Nullable String response) {
    this.response = response;
    return this;
  }

  /**
   * Student / guardian response.
   * @return response
   */
  
  @JsonProperty("response")
  public @Nullable String getResponse() {
    return response;
  }

  @JsonProperty("response")
  public void setResponse(@Nullable String response) {
    this.response = response;
  }

  public DisciplineIncidentUpdate actionTaken(@Nullable Boolean actionTaken) {
    this.actionTaken = actionTaken;
    return this;
  }

  /**
   * Whether action has been taken.
   * @return actionTaken
   */
  
  @JsonProperty("actionTaken")
  public @Nullable Boolean getActionTaken() {
    return actionTaken;
  }

  @JsonProperty("actionTaken")
  public void setActionTaken(@Nullable Boolean actionTaken) {
    this.actionTaken = actionTaken;
  }

  public DisciplineIncidentUpdate evidenceMediaType(@Nullable EvidenceMediaTypeEnum evidenceMediaType) {
    this.evidenceMediaType = evidenceMediaType;
    return this;
  }

  /**
   * Type of evidence attached.
   * @return evidenceMediaType
   */
  
  @JsonProperty("evidenceMediaType")
  public @Nullable EvidenceMediaTypeEnum getEvidenceMediaType() {
    return evidenceMediaType;
  }

  @JsonProperty("evidenceMediaType")
  public void setEvidenceMediaType(@Nullable EvidenceMediaTypeEnum evidenceMediaType) {
    this.evidenceMediaType = evidenceMediaType;
  }

  public DisciplineIncidentUpdate evidenceFileId(@Nullable UUID evidenceFileId) {
    this.evidenceFileId = evidenceFileId;
    return this;
  }

  /**
   * Evidence file.
   * @return evidenceFileId
   */
  @Valid 
  @JsonProperty("evidenceFileId")
  public @Nullable UUID getEvidenceFileId() {
    return evidenceFileId;
  }

  @JsonProperty("evidenceFileId")
  public void setEvidenceFileId(@Nullable UUID evidenceFileId) {
    this.evidenceFileId = evidenceFileId;
  }

  public DisciplineIncidentUpdate notifyGuardians(@Nullable Boolean notifyGuardians) {
    this.notifyGuardians = notifyGuardians;
    return this;
  }

  /**
   * Guardians are notified.
   * @return notifyGuardians
   */
  
  @JsonProperty("notifyGuardians")
  public @Nullable Boolean getNotifyGuardians() {
    return notifyGuardians;
  }

  @JsonProperty("notifyGuardians")
  public void setNotifyGuardians(@Nullable Boolean notifyGuardians) {
    this.notifyGuardians = notifyGuardians;
  }

  public DisciplineIncidentUpdate seenByGuardian(@Nullable Boolean seenByGuardian) {
    this.seenByGuardian = seenByGuardian;
    return this;
  }

  /**
   * Guardian has viewed it.
   * @return seenByGuardian
   */
  
  @JsonProperty("seenByGuardian")
  public @Nullable Boolean getSeenByGuardian() {
    return seenByGuardian;
  }

  @JsonProperty("seenByGuardian")
  public void setSeenByGuardian(@Nullable Boolean seenByGuardian) {
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

