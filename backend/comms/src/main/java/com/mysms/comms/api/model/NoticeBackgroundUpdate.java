// Owned API contract of comms.
package com.mysms.comms.api.model;

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
 * Fields of NoticeBackground that can be updated (PATCH: a property left out keeps its value, null clears it, a value sets it).
 */

public class NoticeBackgroundUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull UUID> institutionId;

  /**
   * Where it is used.
   */
  public enum UsageEnum {
    EVENT("event"),
    
    NOTICE("notice");

    private final String value;

    UsageEnum(String value) {
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
    public static UsageEnum fromValue(String value) {
      for (UsageEnum b : UsageEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull UsageEnum> usage;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<UUID> imageFileId;

  public NoticeBackgroundUpdate institutionId(@Nullable Optional<UUID> institutionId) {
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

  public NoticeBackgroundUpdate usage(@Nullable Optional<UsageEnum> usage) {
    this.usage = usage;
    return this;
  }

  /**
   * Where it is used.
   * @return usage
   */
  
  @JsonProperty("usage")
  public @Nullable Optional<UsageEnum> getUsage() {
    return usage;
  }

  @JsonProperty("usage")
  public void setUsage(@Nullable Optional<UsageEnum> usage) {
    this.usage = usage;
  }

  public NoticeBackgroundUpdate imageFileId(@Nullable Optional<UUID> imageFileId) {
    this.imageFileId = imageFileId;
    return this;
  }

  /**
   * Image.
   * @return imageFileId
   */
  
  @JsonProperty("imageFileId")
  public @Nullable Optional<UUID> getImageFileId() {
    return imageFileId;
  }

  @JsonProperty("imageFileId")
  public void setImageFileId(@Nullable Optional<UUID> imageFileId) {
    this.imageFileId = imageFileId;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    NoticeBackgroundUpdate noticeBackgroundUpdate = (NoticeBackgroundUpdate) o;
    return Objects.equals(this.institutionId, noticeBackgroundUpdate.institutionId) &&
        Objects.equals(this.usage, noticeBackgroundUpdate.usage) &&
        Objects.equals(this.imageFileId, noticeBackgroundUpdate.imageFileId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(institutionId, usage, imageFileId);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class NoticeBackgroundUpdate {\n");
    sb.append("    institutionId: ").append(toIndentedString(institutionId)).append("\n");
    sb.append("    usage: ").append(toIndentedString(usage)).append("\n");
    sb.append("    imageFileId: ").append(toIndentedString(imageFileId)).append("\n");
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

