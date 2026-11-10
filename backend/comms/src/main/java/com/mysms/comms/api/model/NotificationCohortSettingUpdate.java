// Owned API contract of comms (JAVA-CODING-RULES §7); first materialized from api/openapi/comms.yaml on 2026-11-10.
package com.mysms.comms.api.model;

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
 * Fields of NotificationCohortSetting that can be updated (partial update; see updateMask).
 */

public class NotificationCohortSettingUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID cohortId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Boolean popupEnabled;

  public NotificationCohortSettingUpdate cohortId(@Nullable UUID cohortId) {
    this.cohortId = cohortId;
    return this;
  }

  /**
   * Cohort.
   * @return cohortId
   */
  @Valid 
  @JsonProperty("cohortId")
  public @Nullable UUID getCohortId() {
    return cohortId;
  }

  @JsonProperty("cohortId")
  public void setCohortId(@Nullable UUID cohortId) {
    this.cohortId = cohortId;
  }

  public NotificationCohortSettingUpdate popupEnabled(@Nullable Boolean popupEnabled) {
    this.popupEnabled = popupEnabled;
    return this;
  }

  /**
   * Show as popup in this cohort.
   * @return popupEnabled
   */
  
  @JsonProperty("popupEnabled")
  public @Nullable Boolean getPopupEnabled() {
    return popupEnabled;
  }

  @JsonProperty("popupEnabled")
  public void setPopupEnabled(@Nullable Boolean popupEnabled) {
    this.popupEnabled = popupEnabled;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    NotificationCohortSettingUpdate notificationCohortSettingUpdate = (NotificationCohortSettingUpdate) o;
    return Objects.equals(this.cohortId, notificationCohortSettingUpdate.cohortId) &&
        Objects.equals(this.popupEnabled, notificationCohortSettingUpdate.popupEnabled);
  }

  @Override
  public int hashCode() {
    return Objects.hash(cohortId, popupEnabled);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class NotificationCohortSettingUpdate {\n");
    sb.append("    cohortId: ").append(toIndentedString(cohortId)).append("\n");
    sb.append("    popupEnabled: ").append(toIndentedString(popupEnabled)).append("\n");
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

