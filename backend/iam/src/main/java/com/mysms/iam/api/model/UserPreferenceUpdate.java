// Owned API contract of iam (JAVA-CODING-RULES §7); first materialized from api/openapi/iam.yaml on 2026-11-10.
package com.mysms.iam.api.model;

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
 * Fields of UserPreference that can be updated (partial update; see updateMask).
 */

public class UserPreferenceUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String prefKey;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String prefValue;

  public UserPreferenceUpdate prefKey(@Nullable String prefKey) {
    this.prefKey = prefKey;
    return this;
  }

  /**
   * Preference key.
   * @return prefKey
   */
  @Size(max = 100) 
  @JsonProperty("prefKey")
  public @Nullable String getPrefKey() {
    return prefKey;
  }

  @JsonProperty("prefKey")
  public void setPrefKey(@Nullable String prefKey) {
    this.prefKey = prefKey;
  }

  public UserPreferenceUpdate prefValue(@Nullable String prefValue) {
    this.prefValue = prefValue;
    return this;
  }

  /**
   * Preference value.
   * @return prefValue
   */
  @Size(max = 500) 
  @JsonProperty("prefValue")
  public @Nullable String getPrefValue() {
    return prefValue;
  }

  @JsonProperty("prefValue")
  public void setPrefValue(@Nullable String prefValue) {
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

