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
 * TrackVideoWatchRequest
 */

public class TrackVideoWatchRequest {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private String videoSource;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private String watchSeconds;

  public TrackVideoWatchRequest() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public TrackVideoWatchRequest(String videoSource, String watchSeconds) {
    this.videoSource = videoSource;
    this.watchSeconds = watchSeconds;
  }

  public TrackVideoWatchRequest videoSource(String videoSource) {
    this.videoSource = videoSource;
    return this;
  }

  /**
   * Video.
   * @return videoSource
   */
  @NotNull 
  @JsonProperty("videoSource")
  public String getVideoSource() {
    return videoSource;
  }

  @JsonProperty("videoSource")
  public void setVideoSource(String videoSource) {
    this.videoSource = videoSource;
  }

  public TrackVideoWatchRequest watchSeconds(String watchSeconds) {
    this.watchSeconds = watchSeconds;
    return this;
  }

  /**
   * Seconds watched (cumulative).
   * @return watchSeconds
   */
  @NotNull @Pattern(regexp = "^-?\\d+(\\.\\d+)?$") 
  @JsonProperty("watchSeconds")
  public String getWatchSeconds() {
    return watchSeconds;
  }

  @JsonProperty("watchSeconds")
  public void setWatchSeconds(String watchSeconds) {
    this.watchSeconds = watchSeconds;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    TrackVideoWatchRequest trackVideoWatchRequest = (TrackVideoWatchRequest) o;
    return Objects.equals(this.videoSource, trackVideoWatchRequest.videoSource) &&
        Objects.equals(this.watchSeconds, trackVideoWatchRequest.watchSeconds);
  }

  @Override
  public int hashCode() {
    return Objects.hash(videoSource, watchSeconds);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class TrackVideoWatchRequest {\n");
    sb.append("    videoSource: ").append(toIndentedString(videoSource)).append("\n");
    sb.append("    watchSeconds: ").append(toIndentedString(watchSeconds)).append("\n");
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

