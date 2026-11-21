// Owned API contract of privacy.
package com.mysms.privacy.api.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import jakarta.validation.Valid;
import java.util.Objects;
import java.util.UUID;
import org.springframework.lang.Nullable;

/**
 * The tenant's setting of a purpose that can be updated (partial update; see updateMask). Tenant administrators only.
 */

public class PurposeUpdate {

  /**
   * The widest width the tenant allows for a width purpose; absent = no ceiling.
   */
  public enum MaxScopeEnum {
    CLASS("class"),
    
    INSTITUTION("institution"),
    
    INTER_SCHOOL("inter_school"),
    
    PUBLIC("public");

    private final String value;

    MaxScopeEnum(String value) {
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
    public static MaxScopeEnum fromValue(String value) {
      for (MaxScopeEnum b : MaxScopeEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Boolean enabled;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable MaxScopeEnum maxScope;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID noticeId;

  public PurposeUpdate enabled(@Nullable Boolean enabled) {
    this.enabled = enabled;
    return this;
  }

  /**
   * Whether the tenant processes data for the purpose; switching it on needs a notice whose active version lists it.
   * @return enabled
   */
   
  @JsonProperty("enabled")
  public @Nullable Boolean getEnabled() {
    return enabled;
  }

  @JsonProperty("enabled")
  public void setEnabled(@Nullable Boolean enabled) {
    this.enabled = enabled;
  }

  public PurposeUpdate maxScope(@Nullable MaxScopeEnum maxScope) {
    this.maxScope = maxScope;
    return this;
  }

  /**
   * The widest width the tenant allows for a width purpose; absent = no ceiling.
   * @return maxScope
   */
   
  @JsonProperty("maxScope")
  public @Nullable MaxScopeEnum getMaxScope() {
    return maxScope;
  }

  @JsonProperty("maxScope")
  public void setMaxScope(@Nullable MaxScopeEnum maxScope) {
    this.maxScope = maxScope;
  }

  public PurposeUpdate noticeId(@Nullable UUID noticeId) {
    this.noticeId = noticeId;
    return this;
  }

  /**
   * The notice shown when asking for consent to the purpose.
   * @return noticeId
   */
  @Valid 
  @JsonProperty("noticeId")
  public @Nullable UUID getNoticeId() {
    return noticeId;
  }

  @JsonProperty("noticeId")
  public void setNoticeId(@Nullable UUID noticeId) {
    this.noticeId = noticeId;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    PurposeUpdate other = (PurposeUpdate) o;
    return Objects.equals(this.enabled, other.enabled) &&
        Objects.equals(this.maxScope, other.maxScope) &&
        Objects.equals(this.noticeId, other.noticeId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(enabled, maxScope, noticeId);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class PurposeUpdate {\n");
    sb.append("    enabled: ").append(toIndentedString(enabled)).append("\n");
    sb.append("    maxScope: ").append(toIndentedString(maxScope)).append("\n");
    sb.append("    noticeId: ").append(toIndentedString(noticeId)).append("\n");
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
