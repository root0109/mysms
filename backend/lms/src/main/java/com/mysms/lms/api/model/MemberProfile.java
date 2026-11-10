// Owned API contract of lms (JAVA-CODING-RULES §7); first materialized from api/openapi/lms.yaml on 2026-11-10.
package com.mysms.lms.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.mysms.lms.api.model.BadgeAward;
import com.mysms.lms.api.model.Certificate;
import com.mysms.lms.api.model.Enrollment;
import com.mysms.lms.api.model.MemberCard;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.springframework.lang.Nullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;

/**
 * MemberProfile
 */

public class MemberProfile {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable MemberCard member;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private List<@Valid Certificate> certificates = new ArrayList<>();

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private List<@Valid BadgeAward> badges = new ArrayList<>();

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private List<@Valid Enrollment> enrollments = new ArrayList<>();

  public MemberProfile member(@Nullable MemberCard member) {
    this.member = member;
    return this;
  }

  /**
   * Get member
   * @return member
   */
  @Valid 
  @JsonProperty("member")
  public @Nullable MemberCard getMember() {
    return member;
  }

  @JsonProperty("member")
  public void setMember(@Nullable MemberCard member) {
    this.member = member;
  }

  public MemberProfile certificates(List<@Valid Certificate> certificates) {
    this.certificates = certificates;
    return this;
  }

  public MemberProfile addCertificatesItem(Certificate certificatesItem) {
    if (this.certificates == null) {
      this.certificates = new ArrayList<>();
    }
    this.certificates.add(certificatesItem);
    return this;
  }

  /**
   * Published certificates.
   * @return certificates
   */
  
  @JsonProperty("certificates")
  public List<@Valid Certificate> getCertificates() {
    return certificates;
  }

  @JsonProperty("certificates")
  public void setCertificates(List<@Valid Certificate> certificates) {
    this.certificates = certificates;
  }

  public MemberProfile badges(List<@Valid BadgeAward> badges) {
    this.badges = badges;
    return this;
  }

  public MemberProfile addBadgesItem(BadgeAward badgesItem) {
    if (this.badges == null) {
      this.badges = new ArrayList<>();
    }
    this.badges.add(badgesItem);
    return this;
  }

  /**
   * Badges.
   * @return badges
   */
  
  @JsonProperty("badges")
  public List<@Valid BadgeAward> getBadges() {
    return badges;
  }

  @JsonProperty("badges")
  public void setBadges(List<@Valid BadgeAward> badges) {
    this.badges = badges;
  }

  public MemberProfile enrollments(List<@Valid Enrollment> enrollments) {
    this.enrollments = enrollments;
    return this;
  }

  public MemberProfile addEnrollmentsItem(Enrollment enrollmentsItem) {
    if (this.enrollments == null) {
      this.enrollments = new ArrayList<>();
    }
    this.enrollments.add(enrollmentsItem);
    return this;
  }

  /**
   * Courses.
   * @return enrollments
   */
  
  @JsonProperty("enrollments")
  public List<@Valid Enrollment> getEnrollments() {
    return enrollments;
  }

  @JsonProperty("enrollments")
  public void setEnrollments(List<@Valid Enrollment> enrollments) {
    this.enrollments = enrollments;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    MemberProfile memberProfile = (MemberProfile) o;
    return Objects.equals(this.member, memberProfile.member) &&
        Objects.equals(this.certificates, memberProfile.certificates) &&
        Objects.equals(this.badges, memberProfile.badges) &&
        Objects.equals(this.enrollments, memberProfile.enrollments);
  }

  @Override
  public int hashCode() {
    return Objects.hash(member, certificates, badges, enrollments);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class MemberProfile {\n");
    sb.append("    member: ").append(toIndentedString(member)).append("\n");
    sb.append("    certificates: ").append(toIndentedString(certificates)).append("\n");
    sb.append("    badges: ").append(toIndentedString(badges)).append("\n");
    sb.append("    enrollments: ").append(toIndentedString(enrollments)).append("\n");
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

