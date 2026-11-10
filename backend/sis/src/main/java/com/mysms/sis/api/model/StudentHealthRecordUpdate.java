// Owned API contract of sis (JAVA-CODING-RULES §7); first materialized from api/openapi/sis.yaml on 2026-11-10.
package com.mysms.sis.api.model;

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
 * Fields of StudentHealthRecord that can be updated (partial update; see updateMask).
 */

public class StudentHealthRecordUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID studentId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID academicYearId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String heightCm;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String weightKg;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String medicalConditions;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String medicationNeeds;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String medicationAllergies;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String foodAllergies;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String otherAllergies;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String otherMedicalInfo;

  public StudentHealthRecordUpdate studentId(@Nullable UUID studentId) {
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

  public StudentHealthRecordUpdate academicYearId(@Nullable UUID academicYearId) {
    this.academicYearId = academicYearId;
    return this;
  }

  /**
   * Year recorded.
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

  public StudentHealthRecordUpdate heightCm(@Nullable String heightCm) {
    this.heightCm = heightCm;
    return this;
  }

  /**
   * Height (cm).
   * @return heightCm
   */
  @Pattern(regexp = "^-?\\d+(\\.\\d+)?$") 
  @JsonProperty("heightCm")
  public @Nullable String getHeightCm() {
    return heightCm;
  }

  @JsonProperty("heightCm")
  public void setHeightCm(@Nullable String heightCm) {
    this.heightCm = heightCm;
  }

  public StudentHealthRecordUpdate weightKg(@Nullable String weightKg) {
    this.weightKg = weightKg;
    return this;
  }

  /**
   * Weight (kg).
   * @return weightKg
   */
  @Pattern(regexp = "^-?\\d+(\\.\\d+)?$") 
  @JsonProperty("weightKg")
  public @Nullable String getWeightKg() {
    return weightKg;
  }

  @JsonProperty("weightKg")
  public void setWeightKg(@Nullable String weightKg) {
    this.weightKg = weightKg;
  }

  public StudentHealthRecordUpdate medicalConditions(@Nullable String medicalConditions) {
    this.medicalConditions = medicalConditions;
    return this;
  }

  /**
   * Known medical problems.
   * @return medicalConditions
   */
  
  @JsonProperty("medicalConditions")
  public @Nullable String getMedicalConditions() {
    return medicalConditions;
  }

  @JsonProperty("medicalConditions")
  public void setMedicalConditions(@Nullable String medicalConditions) {
    this.medicalConditions = medicalConditions;
  }

  public StudentHealthRecordUpdate medicationNeeds(@Nullable String medicationNeeds) {
    this.medicationNeeds = medicationNeeds;
    return this;
  }

  /**
   * Medication needs.
   * @return medicationNeeds
   */
  
  @JsonProperty("medicationNeeds")
  public @Nullable String getMedicationNeeds() {
    return medicationNeeds;
  }

  @JsonProperty("medicationNeeds")
  public void setMedicationNeeds(@Nullable String medicationNeeds) {
    this.medicationNeeds = medicationNeeds;
  }

  public StudentHealthRecordUpdate medicationAllergies(@Nullable String medicationAllergies) {
    this.medicationAllergies = medicationAllergies;
    return this;
  }

  /**
   * Drug allergies.
   * @return medicationAllergies
   */
  
  @JsonProperty("medicationAllergies")
  public @Nullable String getMedicationAllergies() {
    return medicationAllergies;
  }

  @JsonProperty("medicationAllergies")
  public void setMedicationAllergies(@Nullable String medicationAllergies) {
    this.medicationAllergies = medicationAllergies;
  }

  public StudentHealthRecordUpdate foodAllergies(@Nullable String foodAllergies) {
    this.foodAllergies = foodAllergies;
    return this;
  }

  /**
   * Food allergies.
   * @return foodAllergies
   */
  
  @JsonProperty("foodAllergies")
  public @Nullable String getFoodAllergies() {
    return foodAllergies;
  }

  @JsonProperty("foodAllergies")
  public void setFoodAllergies(@Nullable String foodAllergies) {
    this.foodAllergies = foodAllergies;
  }

  public StudentHealthRecordUpdate otherAllergies(@Nullable String otherAllergies) {
    this.otherAllergies = otherAllergies;
    return this;
  }

  /**
   * Other allergies.
   * @return otherAllergies
   */
  
  @JsonProperty("otherAllergies")
  public @Nullable String getOtherAllergies() {
    return otherAllergies;
  }

  @JsonProperty("otherAllergies")
  public void setOtherAllergies(@Nullable String otherAllergies) {
    this.otherAllergies = otherAllergies;
  }

  public StudentHealthRecordUpdate otherMedicalInfo(@Nullable String otherMedicalInfo) {
    this.otherMedicalInfo = otherMedicalInfo;
    return this;
  }

  /**
   * Other medical information.
   * @return otherMedicalInfo
   */
  
  @JsonProperty("otherMedicalInfo")
  public @Nullable String getOtherMedicalInfo() {
    return otherMedicalInfo;
  }

  @JsonProperty("otherMedicalInfo")
  public void setOtherMedicalInfo(@Nullable String otherMedicalInfo) {
    this.otherMedicalInfo = otherMedicalInfo;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    StudentHealthRecordUpdate studentHealthRecordUpdate = (StudentHealthRecordUpdate) o;
    return Objects.equals(this.studentId, studentHealthRecordUpdate.studentId) &&
        Objects.equals(this.academicYearId, studentHealthRecordUpdate.academicYearId) &&
        Objects.equals(this.heightCm, studentHealthRecordUpdate.heightCm) &&
        Objects.equals(this.weightKg, studentHealthRecordUpdate.weightKg) &&
        Objects.equals(this.medicalConditions, studentHealthRecordUpdate.medicalConditions) &&
        Objects.equals(this.medicationNeeds, studentHealthRecordUpdate.medicationNeeds) &&
        Objects.equals(this.medicationAllergies, studentHealthRecordUpdate.medicationAllergies) &&
        Objects.equals(this.foodAllergies, studentHealthRecordUpdate.foodAllergies) &&
        Objects.equals(this.otherAllergies, studentHealthRecordUpdate.otherAllergies) &&
        Objects.equals(this.otherMedicalInfo, studentHealthRecordUpdate.otherMedicalInfo);
  }

  @Override
  public int hashCode() {
    return Objects.hash(studentId, academicYearId, heightCm, weightKg, medicalConditions, medicationNeeds, medicationAllergies, foodAllergies, otherAllergies, otherMedicalInfo);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class StudentHealthRecordUpdate {\n");
    sb.append("    studentId: ").append(toIndentedString(studentId)).append("\n");
    sb.append("    academicYearId: ").append(toIndentedString(academicYearId)).append("\n");
    sb.append("    heightCm: ").append(toIndentedString(heightCm)).append("\n");
    sb.append("    weightKg: ").append(toIndentedString(weightKg)).append("\n");
    sb.append("    medicalConditions: ").append(toIndentedString(medicalConditions)).append("\n");
    sb.append("    medicationNeeds: ").append(toIndentedString(medicationNeeds)).append("\n");
    sb.append("    medicationAllergies: ").append(toIndentedString(medicationAllergies)).append("\n");
    sb.append("    foodAllergies: ").append(toIndentedString(foodAllergies)).append("\n");
    sb.append("    otherAllergies: ").append(toIndentedString(otherAllergies)).append("\n");
    sb.append("    otherMedicalInfo: ").append(toIndentedString(otherMedicalInfo)).append("\n");
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

