// Owned API contract of admission (JAVA-CODING-RULES §7); first materialized from api/openapi/admission.yaml on 2026-11-10.
package com.mysms.admission.api.model;

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
 * Fields of ApplicationEmergencyContact that can be updated (partial update; see updateMask).
 */

public class ApplicationEmergencyContactUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Integer priority;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String contactNumber;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String relationship;

  public ApplicationEmergencyContactUpdate priority(@Nullable Integer priority) {
    this.priority = priority;
    return this;
  }

  /**
   * Call order.
   * @return priority
   */
  
  @JsonProperty("priority")
  public @Nullable Integer getPriority() {
    return priority;
  }

  @JsonProperty("priority")
  public void setPriority(@Nullable Integer priority) {
    this.priority = priority;
  }

  public ApplicationEmergencyContactUpdate contactNumber(@Nullable String contactNumber) {
    this.contactNumber = contactNumber;
    return this;
  }

  /**
   * Phone number.
   * @return contactNumber
   */
  @Size(max = 20) 
  @JsonProperty("contactNumber")
  public @Nullable String getContactNumber() {
    return contactNumber;
  }

  @JsonProperty("contactNumber")
  public void setContactNumber(@Nullable String contactNumber) {
    this.contactNumber = contactNumber;
  }

  public ApplicationEmergencyContactUpdate relationship(@Nullable String relationship) {
    this.relationship = relationship;
    return this;
  }

  /**
   * Relationship to the applicant.
   * @return relationship
   */
  @Size(max = 60) 
  @JsonProperty("relationship")
  public @Nullable String getRelationship() {
    return relationship;
  }

  @JsonProperty("relationship")
  public void setRelationship(@Nullable String relationship) {
    this.relationship = relationship;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ApplicationEmergencyContactUpdate applicationEmergencyContactUpdate = (ApplicationEmergencyContactUpdate) o;
    return Objects.equals(this.priority, applicationEmergencyContactUpdate.priority) &&
        Objects.equals(this.contactNumber, applicationEmergencyContactUpdate.contactNumber) &&
        Objects.equals(this.relationship, applicationEmergencyContactUpdate.relationship);
  }

  @Override
  public int hashCode() {
    return Objects.hash(priority, contactNumber, relationship);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ApplicationEmergencyContactUpdate {\n");
    sb.append("    priority: ").append(toIndentedString(priority)).append("\n");
    sb.append("    contactNumber: ").append(toIndentedString(contactNumber)).append("\n");
    sb.append("    relationship: ").append(toIndentedString(relationship)).append("\n");
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

