// Owned API contract of privacy.
package com.mysms.privacy.api.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import jakarta.validation.Valid;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.lang.Nullable;

/**
 * Why personal data is processed: a purpose of the platform catalogue and the calling tenant's setting of it
 * (enabled, width ceiling, notice). Tenant administrators only.
 */

public class Purpose {

  /**
   * Whether a consent names a width (class to public) or covers the whole tenant.
   */
  public enum ScopeKindEnum {
    TENANT("tenant"),
    
    WIDTH("width");

    private final String value;

    ScopeKindEnum(String value) {
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
    public static ScopeKindEnum fromValue(String value) {
      for (ScopeKindEnum b : ScopeKindEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }

  /**
   * Who decides the processing: the school (tenant) or MySMS on its own account (platform).
   */
  public enum FiduciaryEnum {
    PLATFORM("platform"),
    
    TENANT("tenant");

    private final String value;

    FiduciaryEnum(String value) {
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
    public static FiduciaryEnum fromValue(String value) {
      for (FiduciaryEnum b : FiduciaryEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }

  /**
   * What a guardian's consent still covers once the student turns 18.
   */
  public enum MajorityGraceEnum {
    KEEP_NARROW_WIDTHS("keep_narrow_widths"),
    
    KEEP_PUBLISHED("keep_published"),
    
    NONE("none");

    private final String value;

    MajorityGraceEnum(String value) {
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
    public static MajorityGraceEnum fromValue(String value) {
      for (MajorityGraceEnum b : MajorityGraceEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }

  /**
   * Lifecycle of the catalogue purpose: deprecated takes no new consents, existing ones still count.
   */
  public enum StatusEnum {
    ACTIVE("active"),
    
    DEPRECATED("deprecated"),
    
    RETIRED("retired");

    private final String value;

    StatusEnum(String value) {
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
    public static StatusEnum fromValue(String value) {
      for (StatusEnum b : StatusEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }

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
  private @Nullable UUID id;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String code;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable ScopeKindEnum scopeKind;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable FiduciaryEnum fiduciary;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Boolean forbiddenForMinors;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable MajorityGraceEnum majorityGrace;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Integer defaultValidMonths;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable StatusEnum status;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Integer version;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Boolean enabled;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable MaxScopeEnum maxScope;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID noticeId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private @Nullable OffsetDateTime updateTime;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String etag;

  public Purpose id(@Nullable UUID id) {
    this.id = id;
    return this;
  }

  /**
   * Stable id of the catalogue purpose, the same in every environment.
   * @return id
   */
  @Valid
  @JsonProperty("id")
  public @Nullable UUID getId() {
    return id;
  }

  @JsonProperty("id")
  public void setId(@Nullable UUID id) {
    this.id = id;
  }

  public Purpose code(@Nullable String code) {
    this.code = code;
    return this;
  }

  /**
   * Purpose code, e.g. photo_video_publication.
   * @return code
   */
  
  @JsonProperty("code")
  public @Nullable String getCode() {
    return code;
  }

  @JsonProperty("code")
  public void setCode(@Nullable String code) {
    this.code = code;
  }

  public Purpose scopeKind(@Nullable ScopeKindEnum scopeKind) {
    this.scopeKind = scopeKind;
    return this;
  }

  /**
   * Whether a consent names a width (class to public) or covers the whole tenant.
   * @return scopeKind
   */
  
  @JsonProperty("scopeKind")
  public @Nullable ScopeKindEnum getScopeKind() {
    return scopeKind;
  }

  @JsonProperty("scopeKind")
  public void setScopeKind(@Nullable ScopeKindEnum scopeKind) {
    this.scopeKind = scopeKind;
  }

  public Purpose fiduciary(@Nullable FiduciaryEnum fiduciary) {
    this.fiduciary = fiduciary;
    return this;
  }

  /**
   * Who decides the processing: the school (tenant) or MySMS on its own account (platform).
   * @return fiduciary
   */
  
  @JsonProperty("fiduciary")
  public @Nullable FiduciaryEnum getFiduciary() {
    return fiduciary;
  }

  @JsonProperty("fiduciary")
  public void setFiduciary(@Nullable FiduciaryEnum fiduciary) {
    this.fiduciary = fiduciary;
  }

  public Purpose forbiddenForMinors(@Nullable Boolean forbiddenForMinors) {
    this.forbiddenForMinors = forbiddenForMinors;
    return this;
  }

  /**
   * Never allowed for a minor, not even with a guardian's consent.
   * @return forbiddenForMinors
   */
  
  @JsonProperty("forbiddenForMinors")
  public @Nullable Boolean getForbiddenForMinors() {
    return forbiddenForMinors;
  }

  @JsonProperty("forbiddenForMinors")
  public void setForbiddenForMinors(@Nullable Boolean forbiddenForMinors) {
    this.forbiddenForMinors = forbiddenForMinors;
  }

  public Purpose majorityGrace(@Nullable MajorityGraceEnum majorityGrace) {
    this.majorityGrace = majorityGrace;
    return this;
  }

  /**
   * What a guardian's consent still covers once the student turns 18.
   * @return majorityGrace
   */
  
  @JsonProperty("majorityGrace")
  public @Nullable MajorityGraceEnum getMajorityGrace() {
    return majorityGrace;
  }

  @JsonProperty("majorityGrace")
  public void setMajorityGrace(@Nullable MajorityGraceEnum majorityGrace) {
    this.majorityGrace = majorityGrace;
  }

  public Purpose defaultValidMonths(@Nullable Integer defaultValidMonths) {
    this.defaultValidMonths = defaultValidMonths;
    return this;
  }

  /**
   * How long a consent lasts unless the notice says otherwise; absent = until withdrawn.
   * @return defaultValidMonths
   */
  
  @JsonProperty("defaultValidMonths")
  public @Nullable Integer getDefaultValidMonths() {
    return defaultValidMonths;
  }

  @JsonProperty("defaultValidMonths")
  public void setDefaultValidMonths(@Nullable Integer defaultValidMonths) {
    this.defaultValidMonths = defaultValidMonths;
  }

  public Purpose status(@Nullable StatusEnum status) {
    this.status = status;
    return this;
  }

  /**
   * Lifecycle of the catalogue purpose: deprecated takes no new consents, existing ones still count.
   * @return status
   */
  
  @JsonProperty("status")
  public @Nullable StatusEnum getStatus() {
    return status;
  }

  @JsonProperty("status")
  public void setStatus(@Nullable StatusEnum status) {
    this.status = status;
  }

  public Purpose version(@Nullable Integer version) {
    this.version = version;
    return this;
  }

  /**
   * Version of the catalogue purpose; incremented on a material change.
   * @return version
   */
  
  @JsonProperty("version")
  public @Nullable Integer getVersion() {
    return version;
  }

  @JsonProperty("version")
  public void setVersion(@Nullable Integer version) {
    this.version = version;
  }

  public Purpose enabled(@Nullable Boolean enabled) {
    this.enabled = enabled;
    return this;
  }

  /**
   * Whether the tenant processes data for the purpose; off until a tenant administrator switches it on.
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

  public Purpose maxScope(@Nullable MaxScopeEnum maxScope) {
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

  public Purpose noticeId(@Nullable UUID noticeId) {
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

  public Purpose updateTime(@Nullable OffsetDateTime updateTime) {
    this.updateTime = updateTime;
    return this;
  }

  /**
   * When the tenant last changed its setting; absent while it never has.
   * @return updateTime
   */
  @Valid
  @JsonProperty("updateTime")
  public @Nullable OffsetDateTime getUpdateTime() {
    return updateTime;
  }

  @JsonProperty("updateTime")
  public void setUpdateTime(@Nullable OffsetDateTime updateTime) {
    this.updateTime = updateTime;
  }

  public Purpose etag(@Nullable String etag) {
    this.etag = etag;
    return this;
  }

  /**
   * Entity tag (version of the tenant's setting); send back in If-Match.
   * @return etag
   */
  
  @JsonProperty("etag")
  public @Nullable String getEtag() {
    return etag;
  }

  @JsonProperty("etag")
  public void setEtag(@Nullable String etag) {
    this.etag = etag;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Purpose other = (Purpose) o;
    return Objects.equals(this.id, other.id) &&
        Objects.equals(this.code, other.code) &&
        Objects.equals(this.scopeKind, other.scopeKind) &&
        Objects.equals(this.fiduciary, other.fiduciary) &&
        Objects.equals(this.forbiddenForMinors, other.forbiddenForMinors) &&
        Objects.equals(this.majorityGrace, other.majorityGrace) &&
        Objects.equals(this.defaultValidMonths, other.defaultValidMonths) &&
        Objects.equals(this.status, other.status) &&
        Objects.equals(this.version, other.version) &&
        Objects.equals(this.enabled, other.enabled) &&
        Objects.equals(this.maxScope, other.maxScope) &&
        Objects.equals(this.noticeId, other.noticeId) &&
        Objects.equals(this.updateTime, other.updateTime) &&
        Objects.equals(this.etag, other.etag);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, code, scopeKind, fiduciary, forbiddenForMinors, majorityGrace, defaultValidMonths, status, version, enabled, maxScope, noticeId, updateTime, etag);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class Purpose {\n");
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    code: ").append(toIndentedString(code)).append("\n");
    sb.append("    scopeKind: ").append(toIndentedString(scopeKind)).append("\n");
    sb.append("    fiduciary: ").append(toIndentedString(fiduciary)).append("\n");
    sb.append("    forbiddenForMinors: ").append(toIndentedString(forbiddenForMinors)).append("\n");
    sb.append("    majorityGrace: ").append(toIndentedString(majorityGrace)).append("\n");
    sb.append("    defaultValidMonths: ").append(toIndentedString(defaultValidMonths)).append("\n");
    sb.append("    status: ").append(toIndentedString(status)).append("\n");
    sb.append("    version: ").append(toIndentedString(version)).append("\n");
    sb.append("    enabled: ").append(toIndentedString(enabled)).append("\n");
    sb.append("    maxScope: ").append(toIndentedString(maxScope)).append("\n");
    sb.append("    noticeId: ").append(toIndentedString(noticeId)).append("\n");
    sb.append("    updateTime: ").append(toIndentedString(updateTime)).append("\n");
    sb.append("    etag: ").append(toIndentedString(etag)).append("\n");
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
