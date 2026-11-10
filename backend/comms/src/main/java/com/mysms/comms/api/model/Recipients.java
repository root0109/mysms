// Owned API contract of comms (JAVA-CODING-RULES §7); first materialized from api/openapi/comms.yaml on 2026-11-10.
package com.mysms.comms.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.springframework.lang.Nullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;

/**
 * Recipients
 */

public class Recipients {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private List<UUID> userIds = new ArrayList<>();

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private List<UUID> audienceGroupIds = new ArrayList<>();

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private List<UUID> classSectionIds = new ArrayList<>();

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Boolean includeGuardians;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Boolean staffOnly;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private List<UUID> cohortIds = new ArrayList<>();

  public Recipients userIds(List<UUID> userIds) {
    this.userIds = userIds;
    return this;
  }

  public Recipients addUserIdsItem(UUID userIdsItem) {
    if (this.userIds == null) {
      this.userIds = new ArrayList<>();
    }
    this.userIds.add(userIdsItem);
    return this;
  }

  /**
   * Individual users.
   * @return userIds
   */
  
  @JsonProperty("userIds")
  public List<UUID> getUserIds() {
    return userIds;
  }

  @JsonProperty("userIds")
  public void setUserIds(List<UUID> userIds) {
    this.userIds = userIds;
  }

  public Recipients audienceGroupIds(List<UUID> audienceGroupIds) {
    this.audienceGroupIds = audienceGroupIds;
    return this;
  }

  public Recipients addAudienceGroupIdsItem(UUID audienceGroupIdsItem) {
    if (this.audienceGroupIds == null) {
      this.audienceGroupIds = new ArrayList<>();
    }
    this.audienceGroupIds.add(audienceGroupIdsItem);
    return this;
  }

  /**
   * Groups.
   * @return audienceGroupIds
   */
  
  @JsonProperty("audienceGroupIds")
  public List<UUID> getAudienceGroupIds() {
    return audienceGroupIds;
  }

  @JsonProperty("audienceGroupIds")
  public void setAudienceGroupIds(List<UUID> audienceGroupIds) {
    this.audienceGroupIds = audienceGroupIds;
  }

  public Recipients classSectionIds(List<UUID> classSectionIds) {
    this.classSectionIds = classSectionIds;
    return this;
  }

  public Recipients addClassSectionIdsItem(UUID classSectionIdsItem) {
    if (this.classSectionIds == null) {
      this.classSectionIds = new ArrayList<>();
    }
    this.classSectionIds.add(classSectionIdsItem);
    return this;
  }

  /**
   * All students of these classes.
   * @return classSectionIds
   */
  
  @JsonProperty("classSectionIds")
  public List<UUID> getClassSectionIds() {
    return classSectionIds;
  }

  @JsonProperty("classSectionIds")
  public void setClassSectionIds(List<UUID> classSectionIds) {
    this.classSectionIds = classSectionIds;
  }

  public Recipients includeGuardians(@Nullable Boolean includeGuardians) {
    this.includeGuardians = includeGuardians;
    return this;
  }

  /**
   * Also send to the guardians of the selected students.
   * @return includeGuardians
   */
  
  @JsonProperty("includeGuardians")
  public @Nullable Boolean getIncludeGuardians() {
    return includeGuardians;
  }

  @JsonProperty("includeGuardians")
  public void setIncludeGuardians(@Nullable Boolean includeGuardians) {
    this.includeGuardians = includeGuardians;
  }

  public Recipients staffOnly(@Nullable Boolean staffOnly) {
    this.staffOnly = staffOnly;
    return this;
  }

  /**
   * Only staff members of the selected scope.
   * @return staffOnly
   */
  
  @JsonProperty("staffOnly")
  public @Nullable Boolean getStaffOnly() {
    return staffOnly;
  }

  @JsonProperty("staffOnly")
  public void setStaffOnly(@Nullable Boolean staffOnly) {
    this.staffOnly = staffOnly;
  }

  public Recipients cohortIds(List<UUID> cohortIds) {
    this.cohortIds = cohortIds;
    return this;
  }

  public Recipients addCohortIdsItem(UUID cohortIdsItem) {
    if (this.cohortIds == null) {
      this.cohortIds = new ArrayList<>();
    }
    this.cohortIds.add(cohortIdsItem);
    return this;
  }

  /**
   * Students of these LMS cohorts.
   * @return cohortIds
   */
  
  @JsonProperty("cohortIds")
  public List<UUID> getCohortIds() {
    return cohortIds;
  }

  @JsonProperty("cohortIds")
  public void setCohortIds(List<UUID> cohortIds) {
    this.cohortIds = cohortIds;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Recipients recipients = (Recipients) o;
    return Objects.equals(this.userIds, recipients.userIds) &&
        Objects.equals(this.audienceGroupIds, recipients.audienceGroupIds) &&
        Objects.equals(this.classSectionIds, recipients.classSectionIds) &&
        Objects.equals(this.includeGuardians, recipients.includeGuardians) &&
        Objects.equals(this.staffOnly, recipients.staffOnly) &&
        Objects.equals(this.cohortIds, recipients.cohortIds);
  }

  @Override
  public int hashCode() {
    return Objects.hash(userIds, audienceGroupIds, classSectionIds, includeGuardians, staffOnly, cohortIds);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class Recipients {\n");
    sb.append("    userIds: ").append(toIndentedString(userIds)).append("\n");
    sb.append("    audienceGroupIds: ").append(toIndentedString(audienceGroupIds)).append("\n");
    sb.append("    classSectionIds: ").append(toIndentedString(classSectionIds)).append("\n");
    sb.append("    includeGuardians: ").append(toIndentedString(includeGuardians)).append("\n");
    sb.append("    staffOnly: ").append(toIndentedString(staffOnly)).append("\n");
    sb.append("    cohortIds: ").append(toIndentedString(cohortIds)).append("\n");
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

