// Owned API contract of lms.
package com.mysms.learning.lms.api.model;

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
 * Fields of ExternalToolSetting that can be updated (PATCH: a property left out keeps its value, null clears it, a value sets it).
 */

public class ExternalToolSettingUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull @Size(max = 100) String> settingKey;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<String> settingValue;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull String> secretValue;

  public ExternalToolSettingUpdate settingKey(@Nullable Optional<String> settingKey) {
    this.settingKey = settingKey;
    return this;
  }

  /**
   * Setting name.
   * @return settingKey
   */
  
  @JsonProperty("settingKey")
  public @Nullable Optional<String> getSettingKey() {
    return settingKey;
  }

  @JsonProperty("settingKey")
  public void setSettingKey(@Nullable Optional<String> settingKey) {
    this.settingKey = settingKey;
  }

  public ExternalToolSettingUpdate settingValue(@Nullable Optional<String> settingValue) {
    this.settingValue = settingValue;
    return this;
  }

  /**
   * Non-secret value.
   * @return settingValue
   */
  
  @JsonProperty("settingValue")
  public @Nullable Optional<String> getSettingValue() {
    return settingValue;
  }

  @JsonProperty("settingValue")
  public void setSettingValue(@Nullable Optional<String> settingValue) {
    this.settingValue = settingValue;
  }

  public ExternalToolSettingUpdate secretValue(@Nullable Optional<String> secretValue) {
    this.secretValue = secretValue;
    return this;
  }

  /**
   * Plain value, input only: stored encrypted and never returned. Secret value (shared secret, password), encrypted by the application; set instead of setting_value.
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

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ExternalToolSettingUpdate externalToolSettingUpdate = (ExternalToolSettingUpdate) o;
    return Objects.equals(this.settingKey, externalToolSettingUpdate.settingKey) &&
        Objects.equals(this.settingValue, externalToolSettingUpdate.settingValue) &&
        Objects.equals(this.secretValue, externalToolSettingUpdate.secretValue);
  }

  @Override
  public int hashCode() {
    return Objects.hash(settingKey, settingValue, secretValue);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ExternalToolSettingUpdate {\n");
    sb.append("    settingKey: ").append(toIndentedString(settingKey)).append("\n");
    sb.append("    settingValue: ").append(toIndentedString(settingValue)).append("\n");
    sb.append("    secretValue: ").append(toIndentedString(secretValue)).append("\n");
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

