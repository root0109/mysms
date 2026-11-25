// Owned API contract of privacy.
package com.mysms.privacy.api.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Objects;
import java.util.UUID;
import org.springframework.lang.Nullable;

/**
 * How the calling tenant stands on an activity's purpose: its legal basis, whether the tenant switched the purpose
 * on, the width ceiling and the notice version consents are asked under. Internal: for service callers.
 */

public class PurposeStatus {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String activityCode;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String purposeCode;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String legalBasis;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Boolean enabled;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String maxScope;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID activeNoticeVersionId;

  public PurposeStatus() {
    super();
  }

  public PurposeStatus activityCode(@Nullable String activityCode) {
    this.activityCode = activityCode;
    return this;
  }

  /**
   * The processing activity.
   * @return activityCode
   */
  
  @JsonProperty("activityCode")
  public @Nullable String getActivityCode() {
    return activityCode;
  }

  @JsonProperty("activityCode")
  public void setActivityCode(@Nullable String activityCode) {
    this.activityCode = activityCode;
  }

  public PurposeStatus purposeCode(@Nullable String purposeCode) {
    this.purposeCode = purposeCode;
    return this;
  }

  /**
   * The purpose the activity serves.
   * @return purposeCode
   */
  
  @JsonProperty("purposeCode")
  public @Nullable String getPurposeCode() {
    return purposeCode;
  }

  @JsonProperty("purposeCode")
  public void setPurposeCode(@Nullable String purposeCode) {
    this.purposeCode = purposeCode;
  }

  public PurposeStatus legalBasis(@Nullable String legalBasis) {
    this.legalBasis = legalBasis;
    return this;
  }

  /**
   * The activity's legal basis.
   * @return legalBasis
   */
  
  @JsonProperty("legalBasis")
  public @Nullable String getLegalBasis() {
    return legalBasis;
  }

  @JsonProperty("legalBasis")
  public void setLegalBasis(@Nullable String legalBasis) {
    this.legalBasis = legalBasis;
  }

  public PurposeStatus enabled(@Nullable Boolean enabled) {
    this.enabled = enabled;
    return this;
  }

  /**
   * Whether the tenant has switched the purpose on.
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

  public PurposeStatus maxScope(@Nullable String maxScope) {
    this.maxScope = maxScope;
    return this;
  }

  /**
   * The widest width the tenant allows for a width purpose; absent = no ceiling.
   * @return maxScope
   */
  
  @JsonProperty("maxScope")
  public @Nullable String getMaxScope() {
    return maxScope;
  }

  @JsonProperty("maxScope")
  public void setMaxScope(@Nullable String maxScope) {
    this.maxScope = maxScope;
  }

  public PurposeStatus activeNoticeVersionId(@Nullable UUID activeNoticeVersionId) {
    this.activeNoticeVersionId = activeNoticeVersionId;
    return this;
  }

  /**
   * The active version of the notice the tenant asks consent under; absent while none is active.
   * @return activeNoticeVersionId
   */
  
  @JsonProperty("activeNoticeVersionId")
  public @Nullable UUID getActiveNoticeVersionId() {
    return activeNoticeVersionId;
  }

  @JsonProperty("activeNoticeVersionId")
  public void setActiveNoticeVersionId(@Nullable UUID activeNoticeVersionId) {
    this.activeNoticeVersionId = activeNoticeVersionId;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    PurposeStatus other = (PurposeStatus) o;
    return Objects.equals(this.activityCode, other.activityCode) &&
        Objects.equals(this.purposeCode, other.purposeCode) &&
        Objects.equals(this.legalBasis, other.legalBasis) &&
        Objects.equals(this.enabled, other.enabled) &&
        Objects.equals(this.maxScope, other.maxScope) &&
        Objects.equals(this.activeNoticeVersionId, other.activeNoticeVersionId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(activityCode, purposeCode, legalBasis, enabled, maxScope, activeNoticeVersionId);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class PurposeStatus {\n");
    sb.append("    activityCode: ").append(toIndentedString(activityCode)).append("\n");
    sb.append("    purposeCode: ").append(toIndentedString(purposeCode)).append("\n");
    sb.append("    legalBasis: ").append(toIndentedString(legalBasis)).append("\n");
    sb.append("    enabled: ").append(toIndentedString(enabled)).append("\n");
    sb.append("    maxScope: ").append(toIndentedString(maxScope)).append("\n");
    sb.append("    activeNoticeVersionId: ").append(toIndentedString(activeNoticeVersionId)).append("\n");
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
