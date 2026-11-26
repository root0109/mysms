// Owned API contract of comms.
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
 * Fields of NotificationPreference that can be updated (PATCH: a property left out keeps its value, null clears it, a value sets it).
 */

public class NotificationPreferenceUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<UUID> notificationTypeId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull @Size(max = 100) String> propertyName;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@Size(max = 200) String> propertyValue;

  public NotificationPreferenceUpdate notificationTypeId(@Nullable Optional<UUID> notificationTypeId) {
    this.notificationTypeId = notificationTypeId;
    return this;
  }

  /**
   * Notification type (NULL = all types).
   * @return notificationTypeId
   */
  
  @JsonProperty("notificationTypeId")
  public @Nullable Optional<UUID> getNotificationTypeId() {
    return notificationTypeId;
  }

  @JsonProperty("notificationTypeId")
  public void setNotificationTypeId(@Nullable Optional<UUID> notificationTypeId) {
    this.notificationTypeId = notificationTypeId;
  }

  public NotificationPreferenceUpdate propertyName(@Nullable Optional<String> propertyName) {
    this.propertyName = propertyName;
    return this;
  }

  /**
   * Property (e.g. notification.email.frequency).
   * @return propertyName
   */
  
  @JsonProperty("propertyName")
  public @Nullable Optional<String> getPropertyName() {
    return propertyName;
  }

  @JsonProperty("propertyName")
  public void setPropertyName(@Nullable Optional<String> propertyName) {
    this.propertyName = propertyName;
  }

  public NotificationPreferenceUpdate propertyValue(@Nullable Optional<String> propertyValue) {
    this.propertyValue = propertyValue;
    return this;
  }

  /**
   * Value.
   * @return propertyValue
   */
  
  @JsonProperty("propertyValue")
  public @Nullable Optional<String> getPropertyValue() {
    return propertyValue;
  }

  @JsonProperty("propertyValue")
  public void setPropertyValue(@Nullable Optional<String> propertyValue) {
    this.propertyValue = propertyValue;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    NotificationPreferenceUpdate notificationPreferenceUpdate = (NotificationPreferenceUpdate) o;
    return Objects.equals(this.notificationTypeId, notificationPreferenceUpdate.notificationTypeId) &&
        Objects.equals(this.propertyName, notificationPreferenceUpdate.propertyName) &&
        Objects.equals(this.propertyValue, notificationPreferenceUpdate.propertyValue);
  }

  @Override
  public int hashCode() {
    return Objects.hash(notificationTypeId, propertyName, propertyValue);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class NotificationPreferenceUpdate {\n");
    sb.append("    notificationTypeId: ").append(toIndentedString(notificationTypeId)).append("\n");
    sb.append("    propertyName: ").append(toIndentedString(propertyName)).append("\n");
    sb.append("    propertyValue: ").append(toIndentedString(propertyValue)).append("\n");
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

