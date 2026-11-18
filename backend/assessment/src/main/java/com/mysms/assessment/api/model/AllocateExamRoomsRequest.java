// Owned API contract of assessment.
package com.mysms.assessment.api.model;

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
 * AllocateExamRoomsRequest
 */

public class AllocateExamRoomsRequest {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private List<UUID> roomIds = new ArrayList<>();

  public AllocateExamRoomsRequest() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public AllocateExamRoomsRequest(List<UUID> roomIds) {
    this.roomIds = roomIds;
  }

  public AllocateExamRoomsRequest roomIds(List<UUID> roomIds) {
    this.roomIds = roomIds;
    return this;
  }

  public AllocateExamRoomsRequest addRoomIdsItem(UUID roomIdsItem) {
    if (this.roomIds == null) {
      this.roomIds = new ArrayList<>();
    }
    this.roomIds.add(roomIdsItem);
    return this;
  }

  /**
   * Rooms in fill order.
   * @return roomIds
   */
  @NotNull 
  @JsonProperty("roomIds")
  public List<UUID> getRoomIds() {
    return roomIds;
  }

  @JsonProperty("roomIds")
  public void setRoomIds(List<UUID> roomIds) {
    this.roomIds = roomIds;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    AllocateExamRoomsRequest allocateExamRoomsRequest = (AllocateExamRoomsRequest) o;
    return Objects.equals(this.roomIds, allocateExamRoomsRequest.roomIds);
  }

  @Override
  public int hashCode() {
    return Objects.hash(roomIds);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class AllocateExamRoomsRequest {\n");
    sb.append("    roomIds: ").append(toIndentedString(roomIds)).append("\n");
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

