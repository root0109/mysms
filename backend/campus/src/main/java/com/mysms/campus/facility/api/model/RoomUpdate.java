// Owned API contract of facility.
package com.mysms.campus.facility.api.model;

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
 * Fields of Room that can be updated (PATCH: a property left out keeps its value, null clears it, a value sets it).
 */

public class RoomUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull UUID> institutionId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull @Size(max = 30) String> code;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull @Size(max = 120) String> name;

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
  private @Nullable Optional<@NotNull RoomTypeEnum> roomType;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull Integer> capacity;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<UUID> defaultCourseId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<UUID> defaultBatchId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<UUID> defaultClassSectionId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull Boolean> isActive;

  public RoomUpdate institutionId(@Nullable Optional<UUID> institutionId) {
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

  public RoomUpdate code(@Nullable Optional<String> code) {
    this.code = code;
    return this;
  }

  /**
   * Room code.
   * @return code
   */
  
  @JsonProperty("code")
  public @Nullable Optional<String> getCode() {
    return code;
  }

  @JsonProperty("code")
  public void setCode(@Nullable Optional<String> code) {
    this.code = code;
  }

  public RoomUpdate name(@Nullable Optional<String> name) {
    this.name = name;
    return this;
  }

  /**
   * Room name.
   * @return name
   */
  
  @JsonProperty("name")
  public @Nullable Optional<String> getName() {
    return name;
  }

  @JsonProperty("name")
  public void setName(@Nullable Optional<String> name) {
    this.name = name;
  }

  public RoomUpdate roomType(@Nullable Optional<RoomTypeEnum> roomType) {
    this.roomType = roomType;
    return this;
  }

  /**
   * Room type.
   * @return roomType
   */
  
  @JsonProperty("roomType")
  public @Nullable Optional<RoomTypeEnum> getRoomType() {
    return roomType;
  }

  @JsonProperty("roomType")
  public void setRoomType(@Nullable Optional<RoomTypeEnum> roomType) {
    this.roomType = roomType;
  }

  public RoomUpdate capacity(@Nullable Optional<Integer> capacity) {
    this.capacity = capacity;
    return this;
  }

  /**
   * Seating capacity.
   * @return capacity
   */
  
  @JsonProperty("capacity")
  public @Nullable Optional<Integer> getCapacity() {
    return capacity;
  }

  @JsonProperty("capacity")
  public void setCapacity(@Nullable Optional<Integer> capacity) {
    this.capacity = capacity;
  }

  public RoomUpdate defaultCourseId(@Nullable Optional<UUID> defaultCourseId) {
    this.defaultCourseId = defaultCourseId;
    return this;
  }

  /**
   * Course the room is normally allotted to.
   * @return defaultCourseId
   */
  
  @JsonProperty("defaultCourseId")
  public @Nullable Optional<UUID> getDefaultCourseId() {
    return defaultCourseId;
  }

  @JsonProperty("defaultCourseId")
  public void setDefaultCourseId(@Nullable Optional<UUID> defaultCourseId) {
    this.defaultCourseId = defaultCourseId;
  }

  public RoomUpdate defaultBatchId(@Nullable Optional<UUID> defaultBatchId) {
    this.defaultBatchId = defaultBatchId;
    return this;
  }

  /**
   * Batch the room is normally allotted to.
   * @return defaultBatchId
   */
  
  @JsonProperty("defaultBatchId")
  public @Nullable Optional<UUID> getDefaultBatchId() {
    return defaultBatchId;
  }

  @JsonProperty("defaultBatchId")
  public void setDefaultBatchId(@Nullable Optional<UUID> defaultBatchId) {
    this.defaultBatchId = defaultBatchId;
  }

  public RoomUpdate defaultClassSectionId(@Nullable Optional<UUID> defaultClassSectionId) {
    this.defaultClassSectionId = defaultClassSectionId;
    return this;
  }

  /**
   * Class section the room is normally allotted to.
   * @return defaultClassSectionId
   */
  
  @JsonProperty("defaultClassSectionId")
  public @Nullable Optional<UUID> getDefaultClassSectionId() {
    return defaultClassSectionId;
  }

  @JsonProperty("defaultClassSectionId")
  public void setDefaultClassSectionId(@Nullable Optional<UUID> defaultClassSectionId) {
    this.defaultClassSectionId = defaultClassSectionId;
  }

  public RoomUpdate isActive(@Nullable Optional<Boolean> isActive) {
    this.isActive = isActive;
    return this;
  }

  /**
   * Whether usable.
   * @return isActive
   */
  
  @JsonProperty("isActive")
  public @Nullable Optional<Boolean> getIsActive() {
    return isActive;
  }

  @JsonProperty("isActive")
  public void setIsActive(@Nullable Optional<Boolean> isActive) {
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

