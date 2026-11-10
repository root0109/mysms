// Owned API contract of facility (JAVA-CODING-RULES §7); first materialized from api/openapi/facility.yaml on 2026-11-10.
package com.mysms.facility.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.UUID;
import org.springframework.lang.Nullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;

/**
 * Fields of Room that can be updated (partial update; see updateMask).
 */

public class RoomUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID institutionId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String code;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String name;

  /**
   * Room type.
   */
  public enum RoomTypeEnum {
    CLASSROOM("classroom"),
    
    LABORATORY("laboratory"),
    
    HALL("hall"),
    
    EXAM_HALL("exam_hall"),
    
    LIBRARY("library"),
    
    OTHER("other");

    private final String value;

    RoomTypeEnum(String value) {
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
    public static RoomTypeEnum fromValue(String value) {
      for (RoomTypeEnum b : RoomTypeEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable RoomTypeEnum roomType;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Integer capacity;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID defaultCourseId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID defaultBatchId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID defaultClassSectionId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Boolean isActive;

  public RoomUpdate institutionId(@Nullable UUID institutionId) {
    this.institutionId = institutionId;
    return this;
  }

  /**
   * Institution.
   * @return institutionId
   */
  @Valid 
  @JsonProperty("institutionId")
  public @Nullable UUID getInstitutionId() {
    return institutionId;
  }

  @JsonProperty("institutionId")
  public void setInstitutionId(@Nullable UUID institutionId) {
    this.institutionId = institutionId;
  }

  public RoomUpdate code(@Nullable String code) {
    this.code = code;
    return this;
  }

  /**
   * Room code.
   * @return code
   */
  @Size(max = 30) 
  @JsonProperty("code")
  public @Nullable String getCode() {
    return code;
  }

  @JsonProperty("code")
  public void setCode(@Nullable String code) {
    this.code = code;
  }

  public RoomUpdate name(@Nullable String name) {
    this.name = name;
    return this;
  }

  /**
   * Room name.
   * @return name
   */
  @Size(max = 120) 
  @JsonProperty("name")
  public @Nullable String getName() {
    return name;
  }

  @JsonProperty("name")
  public void setName(@Nullable String name) {
    this.name = name;
  }

  public RoomUpdate roomType(@Nullable RoomTypeEnum roomType) {
    this.roomType = roomType;
    return this;
  }

  /**
   * Room type.
   * @return roomType
   */
  
  @JsonProperty("roomType")
  public @Nullable RoomTypeEnum getRoomType() {
    return roomType;
  }

  @JsonProperty("roomType")
  public void setRoomType(@Nullable RoomTypeEnum roomType) {
    this.roomType = roomType;
  }

  public RoomUpdate capacity(@Nullable Integer capacity) {
    this.capacity = capacity;
    return this;
  }

  /**
   * Seating capacity.
   * @return capacity
   */
  
  @JsonProperty("capacity")
  public @Nullable Integer getCapacity() {
    return capacity;
  }

  @JsonProperty("capacity")
  public void setCapacity(@Nullable Integer capacity) {
    this.capacity = capacity;
  }

  public RoomUpdate defaultCourseId(@Nullable UUID defaultCourseId) {
    this.defaultCourseId = defaultCourseId;
    return this;
  }

  /**
   * Course the room is normally allotted to.
   * @return defaultCourseId
   */
  @Valid 
  @JsonProperty("defaultCourseId")
  public @Nullable UUID getDefaultCourseId() {
    return defaultCourseId;
  }

  @JsonProperty("defaultCourseId")
  public void setDefaultCourseId(@Nullable UUID defaultCourseId) {
    this.defaultCourseId = defaultCourseId;
  }

  public RoomUpdate defaultBatchId(@Nullable UUID defaultBatchId) {
    this.defaultBatchId = defaultBatchId;
    return this;
  }

  /**
   * Batch the room is normally allotted to.
   * @return defaultBatchId
   */
  @Valid 
  @JsonProperty("defaultBatchId")
  public @Nullable UUID getDefaultBatchId() {
    return defaultBatchId;
  }

  @JsonProperty("defaultBatchId")
  public void setDefaultBatchId(@Nullable UUID defaultBatchId) {
    this.defaultBatchId = defaultBatchId;
  }

  public RoomUpdate defaultClassSectionId(@Nullable UUID defaultClassSectionId) {
    this.defaultClassSectionId = defaultClassSectionId;
    return this;
  }

  /**
   * Class section the room is normally allotted to.
   * @return defaultClassSectionId
   */
  @Valid 
  @JsonProperty("defaultClassSectionId")
  public @Nullable UUID getDefaultClassSectionId() {
    return defaultClassSectionId;
  }

  @JsonProperty("defaultClassSectionId")
  public void setDefaultClassSectionId(@Nullable UUID defaultClassSectionId) {
    this.defaultClassSectionId = defaultClassSectionId;
  }

  public RoomUpdate isActive(@Nullable Boolean isActive) {
    this.isActive = isActive;
    return this;
  }

  /**
   * Whether usable.
   * @return isActive
   */
  
  @JsonProperty("isActive")
  public @Nullable Boolean getIsActive() {
    return isActive;
  }

  @JsonProperty("isActive")
  public void setIsActive(@Nullable Boolean isActive) {
    this.isActive = isActive;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    RoomUpdate roomUpdate = (RoomUpdate) o;
    return Objects.equals(this.institutionId, roomUpdate.institutionId) &&
        Objects.equals(this.code, roomUpdate.code) &&
        Objects.equals(this.name, roomUpdate.name) &&
        Objects.equals(this.roomType, roomUpdate.roomType) &&
        Objects.equals(this.capacity, roomUpdate.capacity) &&
        Objects.equals(this.defaultCourseId, roomUpdate.defaultCourseId) &&
        Objects.equals(this.defaultBatchId, roomUpdate.defaultBatchId) &&
        Objects.equals(this.defaultClassSectionId, roomUpdate.defaultClassSectionId) &&
        Objects.equals(this.isActive, roomUpdate.isActive);
  }

  @Override
  public int hashCode() {
    return Objects.hash(institutionId, code, name, roomType, capacity, defaultCourseId, defaultBatchId, defaultClassSectionId, isActive);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class RoomUpdate {\n");
    sb.append("    institutionId: ").append(toIndentedString(institutionId)).append("\n");
    sb.append("    code: ").append(toIndentedString(code)).append("\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    roomType: ").append(toIndentedString(roomType)).append("\n");
    sb.append("    capacity: ").append(toIndentedString(capacity)).append("\n");
    sb.append("    defaultCourseId: ").append(toIndentedString(defaultCourseId)).append("\n");
    sb.append("    defaultBatchId: ").append(toIndentedString(defaultBatchId)).append("\n");
    sb.append("    defaultClassSectionId: ").append(toIndentedString(defaultClassSectionId)).append("\n");
    sb.append("    isActive: ").append(toIndentedString(isActive)).append("\n");
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

