// Owned API contract of lms.
package com.mysms.learning.lms.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.lang.Nullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;

/**
 * Fields of Mentorship that can be updated (PATCH: a property left out keeps its value, null clears it, a value sets it).
 */

public class MentorshipUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull UUID> mentorUserId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull UUID> menteeUserId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  private @Nullable Optional<LocalDate> validFrom;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  private @Nullable Optional<LocalDate> validTo;

  public MentorshipUpdate mentorUserId(@Nullable Optional<UUID> mentorUserId) {
    this.mentorUserId = mentorUserId;
    return this;
  }

  /**
   * Mentor.
   * @return mentorUserId
   */
  
  @JsonProperty("mentorUserId")
  public @Nullable Optional<UUID> getMentorUserId() {
    return mentorUserId;
  }

  @JsonProperty("mentorUserId")
  public void setMentorUserId(@Nullable Optional<UUID> mentorUserId) {
    this.mentorUserId = mentorUserId;
  }

  public MentorshipUpdate menteeUserId(@Nullable Optional<UUID> menteeUserId) {
    this.menteeUserId = menteeUserId;
    return this;
  }

  /**
   * Student.
   * @return menteeUserId
   */
  
  @JsonProperty("menteeUserId")
  public @Nullable Optional<UUID> getMenteeUserId() {
    return menteeUserId;
  }

  @JsonProperty("menteeUserId")
  public void setMenteeUserId(@Nullable Optional<UUID> menteeUserId) {
    this.menteeUserId = menteeUserId;
  }

  public MentorshipUpdate validFrom(@Nullable Optional<LocalDate> validFrom) {
    this.validFrom = validFrom;
    return this;
  }

  /**
   * From.
   * @return validFrom
   */
  
  @JsonProperty("validFrom")
  public @Nullable Optional<LocalDate> getValidFrom() {
    return validFrom;
  }

  @JsonProperty("validFrom")
  public void setValidFrom(@Nullable Optional<LocalDate> validFrom) {
    this.validFrom = validFrom;
  }

  public MentorshipUpdate validTo(@Nullable Optional<LocalDate> validTo) {
    this.validTo = validTo;
    return this;
  }

  /**
   * Until.
   * @return validTo
   */
  
  @JsonProperty("validTo")
  public @Nullable Optional<LocalDate> getValidTo() {
    return validTo;
  }

  @JsonProperty("validTo")
  public void setValidTo(@Nullable Optional<LocalDate> validTo) {
    this.validTo = validTo;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    MentorshipUpdate mentorshipUpdate = (MentorshipUpdate) o;
    return Objects.equals(this.mentorUserId, mentorshipUpdate.mentorUserId) &&
        Objects.equals(this.menteeUserId, mentorshipUpdate.menteeUserId) &&
        Objects.equals(this.validFrom, mentorshipUpdate.validFrom) &&
        Objects.equals(this.validTo, mentorshipUpdate.validTo);
  }

  @Override
  public int hashCode() {
    return Objects.hash(mentorUserId, menteeUserId, validFrom, validTo);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class MentorshipUpdate {\n");
    sb.append("    mentorUserId: ").append(toIndentedString(mentorUserId)).append("\n");
    sb.append("    menteeUserId: ").append(toIndentedString(menteeUserId)).append("\n");
    sb.append("    validFrom: ").append(toIndentedString(validFrom)).append("\n");
    sb.append("    validTo: ").append(toIndentedString(validTo)).append("\n");
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

