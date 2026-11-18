// Owned API contract of comms.
package com.mysms.comms.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.springframework.lang.Nullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;

/**
 * ArchiveNotificationsRequest
 */

public class ArchiveNotificationsRequest {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable List<UUID> notificationIds = new ArrayList<>();

  public ArchiveNotificationsRequest notificationIds(List<UUID> notificationIds) {
    this.notificationIds = notificationIds;
    return this;
  }

  public ArchiveNotificationsRequest addNotificationIdsItem(UUID notificationIdsItem) {
    if (this.notificationIds == null) {
      this.notificationIds = new ArrayList<>();
    }
    this.notificationIds.add(notificationIdsItem);
    return this;
  }

  /**
   * Notifications to hide (default: all read ones).
   * @return notificationIds
   */
  
  @JsonProperty("notificationIds")
  public List<UUID> getNotificationIds() {
    return notificationIds;
  }

  @JsonProperty("notificationIds")
  public void setNotificationIds(List<UUID> notificationIds) {
    this.notificationIds = notificationIds;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ArchiveNotificationsRequest archiveNotificationsRequest = (ArchiveNotificationsRequest) o;
    return Objects.equals(this.notificationIds, archiveNotificationsRequest.notificationIds);
  }

  @Override
  public int hashCode() {
    return Objects.hash(notificationIds);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ArchiveNotificationsRequest {\n");
    sb.append("    notificationIds: ").append(toIndentedString(notificationIds)).append("\n");
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

