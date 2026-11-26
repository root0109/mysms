// Owned API contract of lms.
package com.mysms.learning.lms.api.model;

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
 * Fields of CourseInterest that can be updated (PATCH: a property left out keeps its value, null clears it, a value sets it).
 */

public class CourseInterestUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull Boolean> emailSent;

  public CourseInterestUpdate emailSent(@Nullable Optional<Boolean> emailSent) {
    this.emailSent = emailSent;
    return this;
  }

  /**
   * Launch notification sent.
   * @return emailSent
   */
  
  @JsonProperty("emailSent")
  public @Nullable Optional<Boolean> getEmailSent() {
    return emailSent;
  }

  @JsonProperty("emailSent")
  public void setEmailSent(@Nullable Optional<Boolean> emailSent) {
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
    return Objects.equals(this.emailSent, courseInterestUpdate.emailSent);
  }

  @Override
  public int hashCode() {
    return Objects.hash(emailSent);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class CourseInterestUpdate {\n");
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

