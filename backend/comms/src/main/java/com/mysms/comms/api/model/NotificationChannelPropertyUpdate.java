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
 * Fields of NotificationChannelProperty that can be updated (PATCH: a property left out keeps its value, null clears it, a value sets it).
 */

public class NotificationChannelPropertyUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull @Size(max = 100) String> propertyKey;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<String> propertyValue;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull String> secretValue;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull Integer> sortOrder;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull Boolean> isMandatory;

  public NotificationChannelPropertyUpdate propertyKey(@Nullable Optional<String> propertyKey) {
    this.propertyKey = propertyKey;
    return this;
  }

  /**
   * Property key.
   * @return propertyKey
   */
  
  @JsonProperty("propertyKey")
  public @Nullable Optional<String> getPropertyKey() {
    return propertyKey;
  }

  @JsonProperty("propertyKey")
  public void setPropertyKey(@Nullable Optional<String> propertyKey) {
    this.propertyKey = propertyKey;
  }

  public NotificationChannelPropertyUpdate propertyValue(@Nullable Optional<String> propertyValue) {
    this.propertyValue = propertyValue;
    return this;
  }

  /**
   * Non-secret value.
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

  public NotificationChannelPropertyUpdate secretValue(@Nullable Optional<String> secretValue) {
    this.secretValue = secretValue;
    return this;
  }

  /**
   * Plain value, input only: stored encrypted and never returned. Secret value (e.g. SMTP password), encrypted by the application; set instead of property_value.
   * @return secretValue
   */
  
  @JsonProperty("secretValue")
  public @Nullable Optional<String> getSecretValue() {
    return secretValue;
  }

  @JsonProperty("secretValue")
  public void setSecretValue(@Nullable Optional<String> secretValue) {
    this.secretValue = secretValue;
  }

  public NotificationChannelPropertyUpdate sortOrder(@Nullable Optional<Integer> sortOrder) {
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

  public NotificationChannelPropertyUpdate isMandatory(@Nullable Optional<Boolean> isMandatory) {
    this.isMandatory = isMandatory;
    return this;
  }

  /**
   * Value must be set for the channel to work.
   * @return isMandatory
   */
  
  @JsonProperty("isMandatory")
  public @Nullable Optional<Boolean> getIsMandatory() {
    return isMandatory;
  }

  @JsonProperty("isMandatory")
  public void setIsMandatory(@Nullable Optional<Boolean> isMandatory) {
    this.isMandatory = isMandatory;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    NotificationChannelPropertyUpdate notificationChannelPropertyUpdate = (NotificationChannelPropertyUpdate) o;
    return Objects.equals(this.propertyKey, notificationChannelPropertyUpdate.propertyKey) &&
        Objects.equals(this.propertyValue, notificationChannelPropertyUpdate.propertyValue) &&
        Objects.equals(this.secretValue, notificationChannelPropertyUpdate.secretValue) &&
        Objects.equals(this.sortOrder, notificationChannelPropertyUpdate.sortOrder) &&
        Objects.equals(this.isMandatory, notificationChannelPropertyUpdate.isMandatory);
  }

  @Override
  public int hashCode() {
    return Objects.hash(propertyKey, propertyValue, secretValue, sortOrder, isMandatory);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class NotificationChannelPropertyUpdate {\n");
    sb.append("    propertyKey: ").append(toIndentedString(propertyKey)).append("\n");
    sb.append("    propertyValue: ").append(toIndentedString(propertyValue)).append("\n");
    sb.append("    secretValue: ").append(toIndentedString(secretValue)).append("\n");
    sb.append("    sortOrder: ").append(toIndentedString(sortOrder)).append("\n");
    sb.append("    isMandatory: ").append(toIndentedString(isMandatory)).append("\n");
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

