// Owned API contract of comms (JAVA-CODING-RULES §7); first materialized from api/openapi/comms.yaml on 2026-11-10.
package com.mysms.comms.api.model;

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
 * Fields of NotificationChannel that can be updated (partial update; see updateMask).
 */

public class NotificationChannelUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String code;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String description;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Boolean isEnabled;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Integer sortOrder;

  public NotificationChannelUpdate code(@Nullable String code) {
    this.code = code;
    return this;
  }

  /**
   * Channel code (notification.email, notification.sms, notification.popup).
   * @return code
   */
  @Size(max = 60) 
  @JsonProperty("code")
  public @Nullable String getCode() {
    return code;
  }

  @JsonProperty("code")
  public void setCode(@Nullable String code) {
    this.code = code;
  }

  public NotificationChannelUpdate description(@Nullable String description) {
    this.description = description;
    return this;
  }

  /**
   * Description.
   * @return description
   */
  @Size(max = 200) 
  @JsonProperty("description")
  public @Nullable String getDescription() {
    return description;
  }

  @JsonProperty("description")
  public void setDescription(@Nullable String description) {
    this.description = description;
  }

  public NotificationChannelUpdate isEnabled(@Nullable Boolean isEnabled) {
    this.isEnabled = isEnabled;
    return this;
  }

  /**
   * Channel enabled.
   * @return isEnabled
   */
  
  @JsonProperty("isEnabled")
  public @Nullable Boolean getIsEnabled() {
    return isEnabled;
  }

  @JsonProperty("isEnabled")
  public void setIsEnabled(@Nullable Boolean isEnabled) {
    this.isEnabled = isEnabled;
  }

  public NotificationChannelUpdate sortOrder(@Nullable Integer sortOrder) {
    this.sortOrder = sortOrder;
    return this;
  }

  /**
   * Display order.
   * @return sortOrder
   */
  
  @JsonProperty("sortOrder")
  public @Nullable Integer getSortOrder() {
    return sortOrder;
  }

  @JsonProperty("sortOrder")
  public void setSortOrder(@Nullable Integer sortOrder) {
    this.sortOrder = sortOrder;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    NotificationChannelUpdate notificationChannelUpdate = (NotificationChannelUpdate) o;
    return Objects.equals(this.code, notificationChannelUpdate.code) &&
        Objects.equals(this.description, notificationChannelUpdate.description) &&
        Objects.equals(this.isEnabled, notificationChannelUpdate.isEnabled) &&
        Objects.equals(this.sortOrder, notificationChannelUpdate.sortOrder);
  }

  @Override
  public int hashCode() {
    return Objects.hash(code, description, isEnabled, sortOrder);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class NotificationChannelUpdate {\n");
    sb.append("    code: ").append(toIndentedString(code)).append("\n");
    sb.append("    description: ").append(toIndentedString(description)).append("\n");
    sb.append("    isEnabled: ").append(toIndentedString(isEnabled)).append("\n");
    sb.append("    sortOrder: ").append(toIndentedString(sortOrder)).append("\n");
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

