// Owned API contract of library (JAVA-CODING-RULES §7); first materialized from api/openapi/library.yaml on 2026-11-10.
package com.mysms.library.api.model;

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
 * Fields of MediaPublisher that can be updated (partial update; see updateMask).
 */

public class MediaPublisherUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID publisherId;

  public MediaPublisherUpdate publisherId(@Nullable UUID publisherId) {
    this.publisherId = publisherId;
    return this;
  }

  /**
   * Publisher.
   * @return publisherId
   */
  @Valid 
  @JsonProperty("publisherId")
  public @Nullable UUID getPublisherId() {
    return publisherId;
  }

  @JsonProperty("publisherId")
  public void setPublisherId(@Nullable UUID publisherId) {
    this.publisherId = publisherId;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    MediaPublisherUpdate mediaPublisherUpdate = (MediaPublisherUpdate) o;
    return Objects.equals(this.publisherId, mediaPublisherUpdate.publisherId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(publisherId);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class MediaPublisherUpdate {\n");
    sb.append("    publisherId: ").append(toIndentedString(publisherId)).append("\n");
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

