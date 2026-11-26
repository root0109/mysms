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
 * Fields of UserPreference that can be updated (PATCH: a property left out keeps its value, null clears it, a value sets it).
 */

public class UserPreferenceUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull @Size(max = 100) String> prefKey;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@Size(max = 500) String> prefValue;

  public UserPreferenceUpdate prefKey(@Nullable Optional<String> prefKey) {
    this.prefKey = prefKey;
    return this;
  }

  /**
   * Preference key.
   * @return prefKey
   */
  
  @JsonProperty("prefKey")
  public @Nullable Optional<String> getPrefKey() {
    return prefKey;
  }

  @JsonProperty("prefKey")
  public void setPrefKey(@Nullable Optional<String> prefKey) {
    this.prefKey = prefKey;
  }

  public UserPreferenceUpdate prefValue(@Nullable Optional<String> prefValue) {
    this.prefValue = prefValue;
    return this;
  }

  /**
   * Preference value.
   * @return prefValue
   */
  
  @JsonProperty("prefValue")
  public @Nullable Optional<String> getPrefValue() {
    return prefValue;
  }

  @JsonProperty("prefValue")
  public void setPrefValue(@Nullable Optional<String> prefValue) {
    this.prefValue = prefValue;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    UserPreferenceUpdate userPreferenceUpdate = (UserPreferenceUpdate) o;
    return Objects.equals(this.prefKey, userPreferenceUpdate.prefKey) &&
        Objects.equals(this.prefValue, userPreferenceUpdate.prefValue);
  }

  @Override
  public int hashCode() {
    return Objects.hash(prefKey, prefValue);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class UserPreferenceUpdate {\n");
    sb.append("    prefKey: ").append(toIndentedString(prefKey)).append("\n");
    sb.append("    prefValue: ").append(toIndentedString(prefValue)).append("\n");
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

