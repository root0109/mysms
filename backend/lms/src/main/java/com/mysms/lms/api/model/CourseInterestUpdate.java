// Owned API contract of lms (JAVA-CODING-RULES §7); first materialized from api/openapi/lms.yaml on 2026-11-10.
package com.mysms.lms.api.model;

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
 * Fields of CourseInterest that can be updated (partial update; see updateMask).
 */

public class CourseInterestUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID userId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Boolean emailSent;

  public CourseInterestUpdate userId(@Nullable UUID userId) {
    this.userId = userId;
    return this;
  }

  /**
   * Interested user.
   * @return userId
   */
  @Valid 
  @JsonProperty("userId")
  public @Nullable UUID getUserId() {
    return userId;
  }

  @JsonProperty("userId")
  public void setUserId(@Nullable UUID userId) {
    this.userId = userId;
  }

  public CourseInterestUpdate emailSent(@Nullable Boolean emailSent) {
    this.emailSent = emailSent;
    return this;
  }

  /**
   * Launch notification sent.
   * @return emailSent
   */
  
  @JsonProperty("emailSent")
  public @Nullable Boolean getEmailSent() {
    return emailSent;
  }

  @JsonProperty("emailSent")
  public void setEmailSent(@Nullable Boolean emailSent) {
    this.emailSent = emailSent;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    CourseInterestUpdate courseInterestUpdate = (CourseInterestUpdate) o;
    return Objects.equals(this.userId, courseInterestUpdate.userId) &&
        Objects.equals(this.emailSent, courseInterestUpdate.emailSent);
  }

  @Override
  public int hashCode() {
    return Objects.hash(userId, emailSent);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class CourseInterestUpdate {\n");
    sb.append("    userId: ").append(toIndentedString(userId)).append("\n");
    sb.append("    emailSent: ").append(toIndentedString(emailSent)).append("\n");
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

