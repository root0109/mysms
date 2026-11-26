// Owned API contract of iam.
package com.mysms.identity.iam.api.model;

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
 * Fields of SettingValue that can be updated (PATCH: a property left out keeps its value, null clears it, a value sets it).
 */

public class SettingValueUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull @Size(max = 500) String> value;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull Boolean> isLocked;

  public SettingValueUpdate value(@Nullable Optional<String> value) {
    this.value = value;
    return this;
  }

  /**
   * The value, as text in the setting's type.
   * @return value
   */
  
  @JsonProperty("value")
  public @Nullable Optional<String> getValue() {
    return value;
  }

  @JsonProperty("value")
  public void setValue(@Nullable Optional<String> value) {
    this.value = value;
  }

  public SettingValueUpdate isLocked(@Nullable Optional<Boolean> isLocked) {
    this.isLocked = isLocked;
    return this;
  }

  /**
   * Narrower scopes (and users) cannot override it.
   * @return isLocked
   */
  
  @JsonProperty("isLocked")
  public @Nullable Optional<Boolean> getIsLocked() {
    return isLocked;
  }

  @JsonProperty("isLocked")
  public void setIsLocked(@Nullable Optional<Boolean> isLocked) {
    this.isLocked = isLocked;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    SettingValueUpdate settingValueUpdate = (SettingValueUpdate) o;
    return Objects.equals(this.value, settingValueUpdate.value) &&
        Objects.equals(this.isLocked, settingValueUpdate.isLocked);
  }

  @Override
  public int hashCode() {
    return Objects.hash(value, isLocked);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class SettingValueUpdate {\n");
    sb.append("    value: ").append(toIndentedString(value)).append("\n");
    sb.append("    isLocked: ").append(toIndentedString(isLocked)).append("\n");
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

