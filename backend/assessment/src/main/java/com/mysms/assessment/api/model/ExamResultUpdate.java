// Owned API contract of assessment.
package com.mysms.assessment.api.model;

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
 * Fields of ExamResult that can be updated (partial update; see updateMask).
 */

public class ExamResultUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID roomId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String note;

  public ExamResultUpdate roomId(@Nullable UUID roomId) {
    this.roomId = roomId;
    return this;
  }

  /**
   * Exam room.
   * @return roomId
   */
  @Valid 
  @JsonProperty("roomId")
  public @Nullable UUID getRoomId() {
    return roomId;
  }

  @JsonProperty("roomId")
  public void setRoomId(@Nullable UUID roomId) {
    this.roomId = roomId;
  }

  public ExamResultUpdate note(@Nullable String note) {
    this.note = note;
    return this;
  }

  /**
   * Note.
   * @return note
   */
  
  @JsonProperty("note")
  public @Nullable String getNote() {
    return note;
  }

  @JsonProperty("note")
  public void setNote(@Nullable String note) {
    this.note = note;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ExamResultUpdate examResultUpdate = (ExamResultUpdate) o;
    return Objects.equals(this.roomId, examResultUpdate.roomId) &&
        Objects.equals(this.note, examResultUpdate.note);
  }

  @Override
  public int hashCode() {
    return Objects.hash(roomId, note);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ExamResultUpdate {\n");
    sb.append("    roomId: ").append(toIndentedString(roomId)).append("\n");
    sb.append("    note: ").append(toIndentedString(note)).append("\n");
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

