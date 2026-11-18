// Owned API contract of lms.
package com.mysms.lms.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import org.springframework.lang.Nullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;

/**
 * Fields of CohortEnrollment that can be updated (partial update; see updateMask).
 */

public class CohortEnrollmentUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Boolean isActive;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Boolean hasAccommodation;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String accommodationRef;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Integer surveyStatus;

  public CohortEnrollmentUpdate isActive(@Nullable Boolean isActive) {
    this.isActive = isActive;
    return this;
  }

  /**
   * Membership active.
   * @return isActive
   */
  
  @JsonProperty("isActive")
  public @Nullable Boolean getIsActive() {
    return isActive;
  }

  @JsonProperty("isActive")
  public void setIsActive(@Nullable Boolean isActive) {
    this.isActive = isActive;
  }

  public CohortEnrollmentUpdate hasAccommodation(@Nullable Boolean hasAccommodation) {
    this.hasAccommodation = hasAccommodation;
    return this;
  }

  /**
   * Student has an accommodation plan (extra time etc., applied through lms.item_override).
   * @return hasAccommodation
   */
  
  @JsonProperty("hasAccommodation")
  public @Nullable Boolean getHasAccommodation() {
    return hasAccommodation;
  }

  @JsonProperty("hasAccommodation")
  public void setHasAccommodation(@Nullable Boolean hasAccommodation) {
    this.hasAccommodation = hasAccommodation;
  }

  public CohortEnrollmentUpdate accommodationRef(@Nullable String accommodationRef) {
    this.accommodationRef = accommodationRef;
    return this;
  }

  /**
   * External reference of the accommodation plan.
   * @return accommodationRef
   */
  @Size(max = 100) 
  @JsonProperty("accommodationRef")
  public @Nullable String getAccommodationRef() {
    return accommodationRef;
  }

  @JsonProperty("accommodationRef")
  public void setAccommodationRef(@Nullable String accommodationRef) {
    this.accommodationRef = accommodationRef;
  }

  public CohortEnrollmentUpdate surveyStatus(@Nullable Integer surveyStatus) {
    this.surveyStatus = surveyStatus;
    return this;
  }

  /**
   * Course-evaluation survey state (0 = pending, 1 = completed).
   * @return surveyStatus
   */
  
  @JsonProperty("surveyStatus")
  public @Nullable Integer getSurveyStatus() {
    return surveyStatus;
  }

  @JsonProperty("surveyStatus")
  public void setSurveyStatus(@Nullable Integer surveyStatus) {
    this.surveyStatus = surveyStatus;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    CohortEnrollmentUpdate cohortEnrollmentUpdate = (CohortEnrollmentUpdate) o;
    return Objects.equals(this.isActive, cohortEnrollmentUpdate.isActive) &&
        Objects.equals(this.hasAccommodation, cohortEnrollmentUpdate.hasAccommodation) &&
        Objects.equals(this.accommodationRef, cohortEnrollmentUpdate.accommodationRef) &&
        Objects.equals(this.surveyStatus, cohortEnrollmentUpdate.surveyStatus);
  }

  @Override
  public int hashCode() {
    return Objects.hash(isActive, hasAccommodation, accommodationRef, surveyStatus);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class CohortEnrollmentUpdate {\n");
    sb.append("    isActive: ").append(toIndentedString(isActive)).append("\n");
    sb.append("    hasAccommodation: ").append(toIndentedString(hasAccommodation)).append("\n");
    sb.append("    accommodationRef: ").append(toIndentedString(accommodationRef)).append("\n");
    sb.append("    surveyStatus: ").append(toIndentedString(surveyStatus)).append("\n");
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

