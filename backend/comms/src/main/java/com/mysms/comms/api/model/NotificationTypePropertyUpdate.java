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
 * Fields of NotificationTypeProperty that can be updated (partial update; see updateMask).
 */

public class NotificationTypePropertyUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String moduleCode;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String propertyValue;

  public NotificationTypePropertyUpdate moduleCode(@Nullable String moduleCode) {
    this.moduleCode = moduleCode;
    return this;
  }

  /**
   * Module.
   * @return moduleCode
   */
  @Size(max = 60) 
  @JsonProperty("moduleCode")
  public @Nullable String getModuleCode() {
    return moduleCode;
  }

  @JsonProperty("moduleCode")
  public void setModuleCode(@Nullable String moduleCode) {
    this.moduleCode = moduleCode;
  }

  public NotificationTypePropertyUpdate propertyValue(@Nullable String propertyValue) {
    this.propertyValue = propertyValue;
    return this;
  }

  /**
   * Property value.
   * @return propertyValue
   */
  @Size(max = 500) 
  @JsonProperty("propertyValue")
  public @Nullable String getPropertyValue() {
    return propertyValue;
  }

  @JsonProperty("propertyValue")
  public void setPropertyValue(@Nullable String propertyValue) {
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
    NotificationTypePropertyUpdate notificationTypePropertyUpdate = (NotificationTypePropertyUpdate) o;
    return Objects.equals(this.moduleCode, notificationTypePropertyUpdate.moduleCode) &&
        Objects.equals(this.propertyValue, notificationTypePropertyUpdate.propertyValue);
  }

  @Override
  public int hashCode() {
    return Objects.hash(moduleCode, propertyValue);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class NotificationTypePropertyUpdate {\n");
    sb.append("    moduleCode: ").append(toIndentedString(moduleCode)).append("\n");
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

