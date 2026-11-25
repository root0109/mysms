// Owned API contract of privacy.
package com.mysms.privacy.api.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Objects;
import java.util.UUID;
import org.springframework.lang.Nullable;

/**
 * Whether one subject's data may be processed for an activity at a width: the effective consent (or the activity's
 * non-consent legal basis), with the subject's state version for cache coherence. Internal: for service callers.
 */

public class ConsentDecision {

  /**
   * Why the decision allows or denies.
   */
  public enum ReasonEnum {
    CONSENT("consent"),
    
    EXPIRED("expired"),
    
    FORBIDDEN_FOR_MINOR("forbidden_for_minor"),
    
    NARROWER_GRANT("narrower_grant"),
    
    NON_CONSENT_BASIS("non_consent_basis"),
    
    NOT_ANSWERED("not_answered"),
    
    PURPOSE_DISABLED("purpose_disabled"),
    
    RECONSENT_REQUIRED("reconsent_required"),
    
    REFUSED("refused"),
    
    UNKNOWN_ACTIVITY("unknown_activity");

    private final String value;

    ReasonEnum(String value) {
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
    public static ReasonEnum fromValue(String value) {
      for (ReasonEnum b : ReasonEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID subjectPersonId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Boolean allowed;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable ReasonEnum reason;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String legalBasis;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String stateVersion;

  public ConsentDecision() {
    super();
  }

  public ConsentDecision subjectPersonId(@Nullable UUID subjectPersonId) {
    this.subjectPersonId = subjectPersonId;
    return this;
  }

  /**
   * The person the decision is about.
   * @return subjectPersonId
   */
  
  @JsonProperty("subjectPersonId")
  public @Nullable UUID getSubjectPersonId() {
    return subjectPersonId;
  }

  @JsonProperty("subjectPersonId")
  public void setSubjectPersonId(@Nullable UUID subjectPersonId) {
    this.subjectPersonId = subjectPersonId;
  }

  public ConsentDecision allowed(@Nullable Boolean allowed) {
    this.allowed = allowed;
    return this;
  }

  /**
   * Whether the processing may go ahead.
   * @return allowed
   */
  
  @JsonProperty("allowed")
  public @Nullable Boolean getAllowed() {
    return allowed;
  }

  @JsonProperty("allowed")
  public void setAllowed(@Nullable Boolean allowed) {
    this.allowed = allowed;
  }

  public ConsentDecision reason(@Nullable ReasonEnum reason) {
    this.reason = reason;
    return this;
  }

  /**
   * Why the decision allows or denies.
   * @return reason
   */
  
  @JsonProperty("reason")
  public @Nullable ReasonEnum getReason() {
    return reason;
  }

  @JsonProperty("reason")
  public void setReason(@Nullable ReasonEnum reason) {
    this.reason = reason;
  }

  public ConsentDecision legalBasis(@Nullable String legalBasis) {
    this.legalBasis = legalBasis;
    return this;
  }

  /**
   * The activity's legal basis (consent, legal_obligation, ...); absent for an unknown activity.
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

  public ConsentDecision stateVersion(@Nullable String stateVersion) {
    this.stateVersion = stateVersion;
    return this;
  }

  /**
   * The subject's consent state version (a string-encoded 64-bit integer); a cached decision older than an
   * event's state version is stale.
   * @return stateVersion
   */
  
  @JsonProperty("stateVersion")
  public @Nullable String getStateVersion() {
    return stateVersion;
  }

  @JsonProperty("stateVersion")
  public void setStateVersion(@Nullable String stateVersion) {
    this.stateVersion = stateVersion;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ConsentDecision other = (ConsentDecision) o;
    return Objects.equals(this.subjectPersonId, other.subjectPersonId) &&
        Objects.equals(this.allowed, other.allowed) &&
        Objects.equals(this.reason, other.reason) &&
        Objects.equals(this.legalBasis, other.legalBasis) &&
        Objects.equals(this.stateVersion, other.stateVersion);
  }

  @Override
  public int hashCode() {
    return Objects.hash(subjectPersonId, allowed, reason, legalBasis, stateVersion);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ConsentDecision {\n");
    sb.append("    subjectPersonId: ").append(toIndentedString(subjectPersonId)).append("\n");
    sb.append("    allowed: ").append(toIndentedString(allowed)).append("\n");
    sb.append("    reason: ").append(toIndentedString(reason)).append("\n");
    sb.append("    legalBasis: ").append(toIndentedString(legalBasis)).append("\n");
    sb.append("    stateVersion: ").append(toIndentedString(stateVersion)).append("\n");
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
