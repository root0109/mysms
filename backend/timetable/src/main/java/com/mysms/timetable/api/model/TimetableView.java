// Owned API contract of timetable.
package com.mysms.timetable.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.mysms.timetable.api.model.Session;
import org.springframework.lang.Nullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;

/**
 * TimetableView
 */

public class TimetableView {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private Session session;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String subjectName;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String staffName;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String roomName;

  public TimetableView() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public TimetableView(Session session) {
    this.session = session;
  }

  public TimetableView session(Session session) {
    this.session = session;
    return this;
  }

  /**
   * Get session
   * @return session
   */
  @NotNull @Valid 
  @JsonProperty("session")
  public Session getSession() {
    return session;
  }

  @JsonProperty("session")
  public void setSession(Session session) {
    this.session = session;
  }

  public TimetableView subjectName(@Nullable String subjectName) {
    this.subjectName = subjectName;
    return this;
  }

  /**
   * Subject name.
   * @return subjectName
   */
  
  @JsonProperty("subjectName")
  public @Nullable String getSubjectName() {
    return subjectName;
  }

  @JsonProperty("subjectName")
  public void setSubjectName(@Nullable String subjectName) {
    this.subjectName = subjectName;
  }

  public TimetableView staffName(@Nullable String staffName) {
    this.staffName = staffName;
    return this;
  }

  /**
   * Teacher name.
   * @return staffName
   */
  
  @JsonProperty("staffName")
  public @Nullable String getStaffName() {
    return staffName;
  }

  @JsonProperty("staffName")
  public void setStaffName(@Nullable String staffName) {
    this.staffName = staffName;
  }

  public TimetableView roomName(@Nullable String roomName) {
    this.roomName = roomName;
    return this;
  }

  /**
   * Room name.
   * @return roomName
   */
  
  @JsonProperty("roomName")
  public @Nullable String getRoomName() {
    return roomName;
  }

  @JsonProperty("roomName")
  public void setRoomName(@Nullable String roomName) {
    this.roomName = roomName;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    TimetableView timetableView = (TimetableView) o;
    return Objects.equals(this.session, timetableView.session) &&
        Objects.equals(this.subjectName, timetableView.subjectName) &&
        Objects.equals(this.staffName, timetableView.staffName) &&
        Objects.equals(this.roomName, timetableView.roomName);
  }

  @Override
  public int hashCode() {
    return Objects.hash(session, subjectName, staffName, roomName);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class TimetableView {\n");
    sb.append("    session: ").append(toIndentedString(session)).append("\n");
    sb.append("    subjectName: ").append(toIndentedString(subjectName)).append("\n");
    sb.append("    staffName: ").append(toIndentedString(staffName)).append("\n");
    sb.append("    roomName: ").append(toIndentedString(roomName)).append("\n");
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

