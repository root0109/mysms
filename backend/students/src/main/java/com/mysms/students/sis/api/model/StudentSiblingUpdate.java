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
 * Fields of StudentSibling that can be updated (PATCH: a property left out keeps its value, null clears it, a value sets it).
 */

public class StudentSiblingUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<UUID> siblingStudentId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@Size(max = 150) String> siblingName;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<String> details;

  public StudentSiblingUpdate siblingStudentId(@Nullable Optional<UUID> siblingStudentId) {
    this.siblingStudentId = siblingStudentId;
    return this;
  }

  /**
   * Sibling, when enrolled in the system.
   * @return siblingStudentId
   */
  
  @JsonProperty("siblingStudentId")
  public @Nullable Optional<UUID> getSiblingStudentId() {
    return siblingStudentId;
  }

  @JsonProperty("siblingStudentId")
  public void setSiblingStudentId(@Nullable Optional<UUID> siblingStudentId) {
    this.siblingStudentId = siblingStudentId;
  }

  public StudentSiblingUpdate siblingName(@Nullable Optional<String> siblingName) {
    this.siblingName = siblingName;
    return this;
  }

  /**
   * Sibling name when not enrolled.
   * @return siblingName
   */
  
  @JsonProperty("siblingName")
  public @Nullable Optional<String> getSiblingName() {
    return siblingName;
  }

  @JsonProperty("siblingName")
  public void setSiblingName(@Nullable Optional<String> siblingName) {
    this.siblingName = siblingName;
  }

  public StudentSiblingUpdate details(@Nullable Optional<String> details) {
    this.details = details;
    return this;
  }

  /**
   * Notes (school, grade).
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

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    StudentSiblingUpdate studentSiblingUpdate = (StudentSiblingUpdate) o;
    return Objects.equals(this.siblingStudentId, studentSiblingUpdate.siblingStudentId) &&
        Objects.equals(this.siblingName, studentSiblingUpdate.siblingName) &&
        Objects.equals(this.details, studentSiblingUpdate.details);
  }

  @Override
  public int hashCode() {
    return Objects.hash(siblingStudentId, siblingName, details);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class StudentSiblingUpdate {\n");
    sb.append("    siblingStudentId: ").append(toIndentedString(siblingStudentId)).append("\n");
    sb.append("    siblingName: ").append(toIndentedString(siblingName)).append("\n");
    sb.append("    details: ").append(toIndentedString(details)).append("\n");
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

