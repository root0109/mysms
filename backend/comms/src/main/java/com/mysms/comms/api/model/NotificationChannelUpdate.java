// Owned API contract of comms.
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
 * Fields of NotificationChannel that can be updated (PATCH: a property left out keeps its value, null clears it, a value sets it).
 */

public class NotificationChannelUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull @Size(max = 60) String> code;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@Size(max = 200) String> description;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull Boolean> isEnabled;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull Integer> sortOrder;

  public NotificationChannelUpdate code(@Nullable Optional<String> code) {
    this.code = code;
    return this;
  }

  /**
   * Channel code (notification.email, notification.sms, notification.popup).
   * @return code
   */
  
  @JsonProperty("code")
  public @Nullable Optional<String> getCode() {
    return code;
  }

  @JsonProperty("code")
  public void setCode(@Nullable Optional<String> code) {
    this.code = code;
  }

  public NotificationChannelUpdate description(@Nullable Optional<String> description) {
    this.description = description;
    return this;
  }

  /**
   * Description.
   * @return description
   */
  
  @JsonProperty("description")
  public @Nullable Optional<String> getDescription() {
    return description;
  }

  @JsonProperty("description")
  public void setDescription(@Nullable Optional<String> description) {
    this.description = description;
  }

  public NotificationChannelUpdate isEnabled(@Nullable Optional<Boolean> isEnabled) {
    this.isEnabled = isEnabled;
    return this;
  }

  /**
   * Channel enabled.
   * @return isEnabled
   */
  
  @JsonProperty("isEnabled")
  public @Nullable Optional<Boolean> getIsEnabled() {
    return isEnabled;
  }

  @JsonProperty("isEnabled")
  public void setIsEnabled(@Nullable Optional<Boolean> isEnabled) {
    this.isEnabled = isEnabled;
  }

  public NotificationChannelUpdate sortOrder(@Nullable Optional<Integer> sortOrder) {
    this.sortOrder = sortOrder;
    return this;
  }

  /**
   * Display order.
   * @return sortOrder
   */
  
  @JsonProperty("sortOrder")
  public @Nullable Optional<Integer> getSortOrder() {
    return sortOrder;
  }

  @JsonProperty("sortOrder")
  public void setSortOrder(@Nullable Optional<Integer> sortOrder) {
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

