// Owned API contract of lms.
package com.mysms.lms.api.model;

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
 * ScormLaunch
 */

public class ScormLaunch {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private String launchUrl;

  private @Nullable Object runtimeState = null;

  public ScormLaunch() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public ScormLaunch(String launchUrl) {
    this.launchUrl = launchUrl;
  }

  public ScormLaunch launchUrl(String launchUrl) {
    this.launchUrl = launchUrl;
    return this;
  }

  /**
   * URL of the SCORM launch file.
   * @return launchUrl
   */
  @NotNull 
  @JsonProperty("launchUrl")
  public String getLaunchUrl() {
    return launchUrl;
  }

  @JsonProperty("launchUrl")
  public void setLaunchUrl(String launchUrl) {
    this.launchUrl = launchUrl;
  }

  public ScormLaunch runtimeState(@Nullable Object runtimeState) {
    this.runtimeState = runtimeState;
    return this;
  }

  /**
   * Saved CMI data.
   * @return runtimeState
   */
  
  @JsonProperty("runtimeState")
  public @Nullable Object getRuntimeState() {
    return runtimeState;
  }

  @JsonProperty("runtimeState")
  public void setRuntimeState(@Nullable Object runtimeState) {
    this.runtimeState = runtimeState;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ScormLaunch scormLaunch = (ScormLaunch) o;
    return Objects.equals(this.launchUrl, scormLaunch.launchUrl) &&
        Objects.equals(this.runtimeState, scormLaunch.runtimeState);
  }

  @Override
  public int hashCode() {
    return Objects.hash(launchUrl, runtimeState);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ScormLaunch {\n");
    sb.append("    launchUrl: ").append(toIndentedString(launchUrl)).append("\n");
    sb.append("    runtimeState: ").append(toIndentedString(runtimeState)).append("\n");
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

