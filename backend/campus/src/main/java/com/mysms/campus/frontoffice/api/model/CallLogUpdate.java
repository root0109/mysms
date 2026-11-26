// Owned API contract of frontoffice.
package com.mysms.campus.frontoffice.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.lang.Nullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;

/**
 * Fields of CallLog that can be updated (PATCH: a property left out keeps its value, null clears it, a value sets it).
 */

public class CallLogUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull UUID> institutionId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<UUID> academicYearId;

  /**
   * Direction.
   */
  public enum DirectionEnum {
    INCOMING("incoming"),
    
    OUTGOING("outgoing");

    private final String value;

    DirectionEnum(String value) {
      this.value = value;
    }

    @JsonValue
    public String getValue() {
      return value;
    }

    @Override
    public String toString() {
      return String.valueOf(value);
    }

    @JsonCreator
    public static DirectionEnum fromValue(String value) {
      for (DirectionEnum b : DirectionEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull DirectionEnum> direction;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@Size(max = 255) String> purpose;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull @Size(max = 150) String> callerName;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@Size(max = 20) String> incomingNumber;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@Size(max = 20) String> outgoingNumber;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  private @Nullable Optional<@NotNull LocalDate> callDate;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@Pattern(regexp = "^\\d{2}:\\d{2}(:\\d{2})?$") String> startTime;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@Pattern(regexp = "^\\d{2}:\\d{2}(:\\d{2})?$") String> endTime;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@Pattern(regexp = "^-?\\d+(\\.\\d+)?s$") String> durationSeconds;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<String> description;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<UUID> loggedByUserId;

  public CallLogUpdate institutionId(@Nullable Optional<UUID> institutionId) {
    this.institutionId = institutionId;
    return this;
  }

  /**
   * Institution.
   * @return institutionId
   */
  
  @JsonProperty("institutionId")
  public @Nullable Optional<UUID> getInstitutionId() {
    return institutionId;
  }

  @JsonProperty("institutionId")
  public void setInstitutionId(@Nullable Optional<UUID> institutionId) {
    this.institutionId = institutionId;
  }

  public CallLogUpdate academicYearId(@Nullable Optional<UUID> academicYearId) {
    this.academicYearId = academicYearId;
    return this;
  }

  /**
   * Academic year.
   * @return academicYearId
   */
  
  @JsonProperty("academicYearId")
  public @Nullable Optional<UUID> getAcademicYearId() {
    return academicYearId;
  }

  @JsonProperty("academicYearId")
  public void setAcademicYearId(@Nullable Optional<UUID> academicYearId) {
    this.academicYearId = academicYearId;
  }

  public CallLogUpdate direction(@Nullable Optional<DirectionEnum> direction) {
    this.direction = direction;
    return this;
  }

  /**
   * Direction.
   * @return direction
   */
  
  @JsonProperty("direction")
  public @Nullable Optional<DirectionEnum> getDirection() {
    return direction;
  }

  @JsonProperty("direction")
  public void setDirection(@Nullable Optional<DirectionEnum> direction) {
    this.direction = direction;
  }

  public CallLogUpdate purpose(@Nullable Optional<String> purpose) {
    this.purpose = purpose;
    return this;
  }

  /**
   * Purpose.
   * @return purpose
   */
  
  @JsonProperty("purpose")
  public @Nullable Optional<String> getPurpose() {
    return purpose;
  }

  @JsonProperty("purpose")
  public void setPurpose(@Nullable Optional<String> purpose) {
    this.purpose = purpose;
  }

  public CallLogUpdate callerName(@Nullable Optional<String> callerName) {
    this.callerName = callerName;
    return this;
  }

  /**
   * Caller / callee name.
   * @return callerName
   */
  
  @JsonProperty("callerName")
  public @Nullable Optional<String> getCallerName() {
    return callerName;
  }

  @JsonProperty("callerName")
  public void setCallerName(@Nullable Optional<String> callerName) {
    this.callerName = callerName;
  }

  public CallLogUpdate incomingNumber(@Nullable Optional<String> incomingNumber) {
    this.incomingNumber = incomingNumber;
    return this;
  }

  /**
   * Incoming number.
   * @return incomingNumber
   */
  
  @JsonProperty("incomingNumber")
  public @Nullable Optional<String> getIncomingNumber() {
    return incomingNumber;
  }

  @JsonProperty("incomingNumber")
  public void setIncomingNumber(@Nullable Optional<String> incomingNumber) {
    this.incomingNumber = incomingNumber;
  }

  public CallLogUpdate outgoingNumber(@Nullable Optional<String> outgoingNumber) {
    this.outgoingNumber = outgoingNumber;
    return this;
  }

  /**
   * Outgoing number.
   * @return outgoingNumber
   */
  
  @JsonProperty("outgoingNumber")
  public @Nullable Optional<String> getOutgoingNumber() {
    return outgoingNumber;
  }

  @JsonProperty("outgoingNumber")
  public void setOutgoingNumber(@Nullable Optional<String> outgoingNumber) {
    this.outgoingNumber = outgoingNumber;
  }

  public CallLogUpdate callDate(@Nullable Optional<LocalDate> callDate) {
    this.callDate = callDate;
    return this;
  }

  /**
   * Date.
   * @return callDate
   */
  
  @JsonProperty("callDate")
  public @Nullable Optional<LocalDate> getCallDate() {
    return callDate;
  }

  @JsonProperty("callDate")
  public void setCallDate(@Nullable Optional<LocalDate> callDate) {
    this.callDate = callDate;
  }

  public CallLogUpdate startTime(@Nullable Optional<String> startTime) {
    this.startTime = startTime;
    return this;
  }

  /**
   * Start.
   * @return startTime
   */
  
  @JsonProperty("startTime")
  public @Nullable Optional<String> getStartTime() {
    return startTime;
  }

  @JsonProperty("startTime")
  public void setStartTime(@Nullable Optional<String> startTime) {
    this.startTime = startTime;
  }

  public CallLogUpdate endTime(@Nullable Optional<String> endTime) {
    this.endTime = endTime;
    return this;
  }

  /**
   * End.
   * @return endTime
   */
  
  @JsonProperty("endTime")
  public @Nullable Optional<String> getEndTime() {
    return endTime;
  }

  @JsonProperty("endTime")
  public void setEndTime(@Nullable Optional<String> endTime) {
    this.endTime = endTime;
  }

  public CallLogUpdate durationSeconds(@Nullable Optional<String> durationSeconds) {
    this.durationSeconds = durationSeconds;
    return this;
  }

  /**
   * Duration.
   * @return durationSeconds
   */
  
  @JsonProperty("durationSeconds")
  public @Nullable Optional<String> getDurationSeconds() {
    return durationSeconds;
  }

  @JsonProperty("durationSeconds")
  public void setDurationSeconds(@Nullable Optional<String> durationSeconds) {
    this.durationSeconds = durationSeconds;
  }

  public CallLogUpdate description(@Nullable Optional<String> description) {
    this.description = description;
    return this;
  }

  /**
   * Notes.
   * @return description
   */
  
  @JsonProperty("description")
  public @Nullable Optional<String> getDescription() {
    return description;
  }

  @JsonProperty("description")
  public void setDescription(@Nullable Optional<String> description) {
    this.description = description;
  }

  public CallLogUpdate loggedByUserId(@Nullable Optional<UUID> loggedByUserId) {
    this.loggedByUserId = loggedByUserId;
    return this;
  }

  /**
   * Staff who logged it.
   * @return loggedByUserId
   */
  
  @JsonProperty("loggedByUserId")
  public @Nullable Optional<UUID> getLoggedByUserId() {
    return loggedByUserId;
  }

  @JsonProperty("loggedByUserId")
  public void setLoggedByUserId(@Nullable Optional<UUID> loggedByUserId) {
    this.loggedByUserId = loggedByUserId;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    CallLogUpdate callLogUpdate = (CallLogUpdate) o;
    return Objects.equals(this.institutionId, callLogUpdate.institutionId) &&
        Objects.equals(this.academicYearId, callLogUpdate.academicYearId) &&
        Objects.equals(this.direction, callLogUpdate.direction) &&
        Objects.equals(this.purpose, callLogUpdate.purpose) &&
        Objects.equals(this.callerName, callLogUpdate.callerName) &&
        Objects.equals(this.incomingNumber, callLogUpdate.incomingNumber) &&
        Objects.equals(this.outgoingNumber, callLogUpdate.outgoingNumber) &&
        Objects.equals(this.callDate, callLogUpdate.callDate) &&
        Objects.equals(this.startTime, callLogUpdate.startTime) &&
        Objects.equals(this.endTime, callLogUpdate.endTime) &&
        Objects.equals(this.durationSeconds, callLogUpdate.durationSeconds) &&
        Objects.equals(this.description, callLogUpdate.description) &&
        Objects.equals(this.loggedByUserId, callLogUpdate.loggedByUserId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(institutionId, academicYearId, direction, purpose, callerName, incomingNumber, outgoingNumber, callDate, startTime, endTime, durationSeconds, description, loggedByUserId);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class CallLogUpdate {\n");
    sb.append("    institutionId: ").append(toIndentedString(institutionId)).append("\n");
    sb.append("    academicYearId: ").append(toIndentedString(academicYearId)).append("\n");
    sb.append("    direction: ").append(toIndentedString(direction)).append("\n");
    sb.append("    purpose: ").append(toIndentedString(purpose)).append("\n");
    sb.append("    callerName: ").append(toIndentedString(callerName)).append("\n");
    sb.append("    incomingNumber: ").append(toIndentedString(incomingNumber)).append("\n");
    sb.append("    outgoingNumber: ").append(toIndentedString(outgoingNumber)).append("\n");
    sb.append("    callDate: ").append(toIndentedString(callDate)).append("\n");
    sb.append("    startTime: ").append(toIndentedString(startTime)).append("\n");
    sb.append("    endTime: ").append(toIndentedString(endTime)).append("\n");
    sb.append("    durationSeconds: ").append(toIndentedString(durationSeconds)).append("\n");
    sb.append("    description: ").append(toIndentedString(description)).append("\n");
    sb.append("    loggedByUserId: ").append(toIndentedString(loggedByUserId)).append("\n");
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

