// Owned API contract of iam.
package com.mysms.identity.iam.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.time.OffsetDateTime;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.lang.Nullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;

/**
 * Fields of UserDevice that can be updated (PATCH: a property left out keeps its value, null clears it, a value sets it).
 */

public class UserDeviceUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull String> deviceIdentifier;

  /**
   * Client platform.
   */
  public enum PlatformEnum {
    ANDROID("android"),
    
    IOS("ios"),
    
    WEB("web");

    private final String value;

    PlatformEnum(String value) {
      this.value = value;
    }

    @JsonValue
    public String getValue() {
      return value;
    }

    @Override
    public String toString() {
      return String.valueOf(value);
    }

    @JsonCreator
    public static PlatformEnum fromValue(String value) {
      for (PlatformEnum b : PlatformEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull PlatformEnum> platform;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<String> pushToken;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private @Nullable Optional<OffsetDateTime> lastSeenAt;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull Boolean> isActive;

  public UserDeviceUpdate deviceIdentifier(@Nullable Optional<String> deviceIdentifier) {
    this.deviceIdentifier = deviceIdentifier;
    return this;
  }

  /**
   * Device / installation identifier.
   * @return deviceIdentifier
   */
  
  @JsonProperty("deviceIdentifier")
  public @Nullable Optional<String> getDeviceIdentifier() {
    return deviceIdentifier;
  }

  @JsonProperty("deviceIdentifier")
  public void setDeviceIdentifier(@Nullable Optional<String> deviceIdentifier) {
    this.deviceIdentifier = deviceIdentifier;
  }

  public UserDeviceUpdate platform(@Nullable Optional<PlatformEnum> platform) {
    this.platform = platform;
    return this;
  }

  /**
   * Client platform.
   * @return platform
   */
  
  @JsonProperty("platform")
  public @Nullable Optional<PlatformEnum> getPlatform() {
    return platform;
  }

  @JsonProperty("platform")
  public void setPlatform(@Nullable Optional<PlatformEnum> platform) {
    this.platform = platform;
  }

  public UserDeviceUpdate pushToken(@Nullable Optional<String> pushToken) {
    this.pushToken = pushToken;
    return this;
  }

  /**
   * Push-notification token (FCM/APNs/Web Push). Input only; never returned.
   * @return pushToken
   */
  
  @JsonProperty("pushToken")
  public @Nullable Optional<String> getPushToken() {
    return pushToken;
  }

  @JsonProperty("pushToken")
  public void setPushToken(@Nullable Optional<String> pushToken) {
    this.pushToken = pushToken;
  }

  public UserDeviceUpdate lastSeenAt(@Nullable Optional<OffsetDateTime> lastSeenAt) {
    this.lastSeenAt = lastSeenAt;
    return this;
  }

  /**
   * Last activity from this device.
   * @return lastSeenAt
   */
  
  @JsonProperty("lastSeenAt")
  public @Nullable Optional<OffsetDateTime> getLastSeenAt() {
    return lastSeenAt;
  }

  @JsonProperty("lastSeenAt")
  public void setLastSeenAt(@Nullable Optional<OffsetDateTime> lastSeenAt) {
    this.lastSeenAt = lastSeenAt;
  }

  public UserDeviceUpdate isActive(@Nullable Optional<Boolean> isActive) {
    this.isActive = isActive;
    return this;
  }

  /**
   * Whether pushes are sent to this device.
   * @return isActive
   */
  
  @JsonProperty("isActive")
  public @Nullable Optional<Boolean> getIsActive() {
    return isActive;
  }

  @JsonProperty("isActive")
  public void setIsActive(@Nullable Optional<Boolean> isActive) {
    this.isActive = isActive;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    UserDeviceUpdate userDeviceUpdate = (UserDeviceUpdate) o;
    return Objects.equals(this.deviceIdentifier, userDeviceUpdate.deviceIdentifier) &&
        Objects.equals(this.platform, userDeviceUpdate.platform) &&
        Objects.equals(this.pushToken, userDeviceUpdate.pushToken) &&
        Objects.equals(this.lastSeenAt, userDeviceUpdate.lastSeenAt) &&
        Objects.equals(this.isActive, userDeviceUpdate.isActive);
  }

  @Override
  public int hashCode() {
    return Objects.hash(deviceIdentifier, platform, pushToken, lastSeenAt, isActive);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class UserDeviceUpdate {\n");
    sb.append("    deviceIdentifier: ").append(toIndentedString(deviceIdentifier)).append("\n");
    sb.append("    platform: ").append(toIndentedString(platform)).append("\n");
    sb.append("    pushToken: ").append(toIndentedString(pushToken)).append("\n");
    sb.append("    lastSeenAt: ").append(toIndentedString(lastSeenAt)).append("\n");
    sb.append("    isActive: ").append(toIndentedString(isActive)).append("\n");
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

