// Owned API contract of lms.
package com.mysms.learning.lms.api.model;

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
 * Fields of SettingsPrimaryCountry that can be updated (PATCH: a property left out keeps its value, null clears it, a value sets it).
 */

public class SettingsPrimaryCountryUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull UUID> countryId;

  public SettingsPrimaryCountryUpdate countryId(@Nullable Optional<UUID> countryId) {
    this.countryId = countryId;
    return this;
  }

  /**
   * Country.
   * @return countryId
   */
  
  @JsonProperty("countryId")
  public @Nullable Optional<UUID> getCountryId() {
    return countryId;
  }

  @JsonProperty("countryId")
  public void setCountryId(@Nullable Optional<UUID> countryId) {
    this.countryId = countryId;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    SettingsPrimaryCountryUpdate settingsPrimaryCountryUpdate = (SettingsPrimaryCountryUpdate) o;
    return Objects.equals(this.countryId, settingsPrimaryCountryUpdate.countryId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(countryId);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class SettingsPrimaryCountryUpdate {\n");
    sb.append("    countryId: ").append(toIndentedString(countryId)).append("\n");
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

