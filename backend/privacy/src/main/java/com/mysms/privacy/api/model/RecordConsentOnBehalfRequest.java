// Owned API contract of privacy.
package com.mysms.privacy.api.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.Objects;
import java.util.UUID;
import org.springframework.lang.Nullable;

/**
 * Body of RecordConsentOnBehalf: a signed paper form recorded by office staff for its giver. The record stays unverified, and does not count, until a second staff member verifies it.
 */

public class RecordConsentOnBehalfRequest {

  /**
   * self (an adult subject) or guardian (a legal guardian of a minor).
   */
  public enum GiverCapacityEnum {
    SELF("self"),
    
    GUARDIAN("guardian");

    private final String value;

    GiverCapacityEnum(String value) {
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
    public static GiverCapacityEnum fromValue(String value) {
      for (GiverCapacityEnum b : GiverCapacityEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }

  /**
   * The width decided at; tenant for a tenant-scoped purpose.
   */
  public enum ScopeEnum {
    CLASS("class"),
    
    INSTITUTION("institution"),
    
    INTER_SCHOOL("inter_school"),
    
    PUBLIC("public"),
    
    TENANT("tenant");

    private final String value;

    ScopeEnum(String value) {
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
    public static ScopeEnum fromValue(String value) {
      for (ScopeEnum b : ScopeEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }

  /**
   * granted or refused.
   */
  public enum DecisionEnum {
    GRANTED("granted"),
    
    REFUSED("refused");

    private final String value;

    DecisionEnum(String value) {
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
    public static DecisionEnum fromValue(String value) {
      for (DecisionEnum b : DecisionEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private UUID subjectPersonId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private UUID giverPersonId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private GiverCapacityEnum giverCapacity;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private String purposeCode;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private ScopeEnum scope;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private DecisionEnum decision;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private UUID noticeVersionId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private String noticeLanguage;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private UUID evidenceFileId;

  public RecordConsentOnBehalfRequest() {
    super();
  }

  public RecordConsentOnBehalfRequest subjectPersonId(UUID subjectPersonId) {
    this.subjectPersonId = subjectPersonId;
    return this;
  }

  /**
   * The person the decision is about.
   * @return subjectPersonId
   */
  @NotNull @Valid 
  @JsonProperty("subjectPersonId")
  public UUID getSubjectPersonId() {
    return subjectPersonId;
  }

  @JsonProperty("subjectPersonId")
  public void setSubjectPersonId(UUID subjectPersonId) {
    this.subjectPersonId = subjectPersonId;
  }

  public RecordConsentOnBehalfRequest giverPersonId(UUID giverPersonId) {
    this.giverPersonId = giverPersonId;
    return this;
  }

  /**
   * The person who signed the form: the subject (an adult) or an active legal guardian of the minor.
   * @return giverPersonId
   */
  @NotNull @Valid 
  @JsonProperty("giverPersonId")
  public UUID getGiverPersonId() {
    return giverPersonId;
  }

  @JsonProperty("giverPersonId")
  public void setGiverPersonId(UUID giverPersonId) {
    this.giverPersonId = giverPersonId;
  }

  public RecordConsentOnBehalfRequest giverCapacity(GiverCapacityEnum giverCapacity) {
    this.giverCapacity = giverCapacity;
    return this;
  }

  /**
   * self (an adult subject) or guardian (a legal guardian of a minor).
   * @return giverCapacity
   */
  @NotNull 
  @JsonProperty("giverCapacity")
  public GiverCapacityEnum getGiverCapacity() {
    return giverCapacity;
  }

  @JsonProperty("giverCapacity")
  public void setGiverCapacity(GiverCapacityEnum giverCapacity) {
    this.giverCapacity = giverCapacity;
  }

  public RecordConsentOnBehalfRequest purposeCode(String purposeCode) {
    this.purposeCode = purposeCode;
    return this;
  }

  /**
   * The catalogue purpose decided on.
   * @return purposeCode
   */
  @NotNull @Size(max = 40) 
  @JsonProperty("purposeCode")
  public String getPurposeCode() {
    return purposeCode;
  }

  @JsonProperty("purposeCode")
  public void setPurposeCode(String purposeCode) {
    this.purposeCode = purposeCode;
  }

  public RecordConsentOnBehalfRequest scope(ScopeEnum scope) {
    this.scope = scope;
    return this;
  }

  /**
   * The width decided at; tenant for a tenant-scoped purpose.
   * @return scope
   */
  @NotNull 
  @JsonProperty("scope")
  public ScopeEnum getScope() {
    return scope;
  }

  @JsonProperty("scope")
  public void setScope(ScopeEnum scope) {
    this.scope = scope;
  }

  public RecordConsentOnBehalfRequest decision(DecisionEnum decision) {
    this.decision = decision;
    return this;
  }

  /**
   * granted or refused.
   * @return decision
   */
  @NotNull 
  @JsonProperty("decision")
  public DecisionEnum getDecision() {
    return decision;
  }

  @JsonProperty("decision")
  public void setDecision(DecisionEnum decision) {
    this.decision = decision;
  }

  public RecordConsentOnBehalfRequest noticeVersionId(UUID noticeVersionId) {
    this.noticeVersionId = noticeVersionId;
    return this;
  }

  /**
   * The active notice version printed on the form.
   * @return noticeVersionId
   */
  @NotNull @Valid 
  @JsonProperty("noticeVersionId")
  public UUID getNoticeVersionId() {
    return noticeVersionId;
  }

  @JsonProperty("noticeVersionId")
  public void setNoticeVersionId(UUID noticeVersionId) {
    this.noticeVersionId = noticeVersionId;
  }

  public RecordConsentOnBehalfRequest noticeLanguage(String noticeLanguage) {
    this.noticeLanguage = noticeLanguage;
    return this;
  }

  /**
   * The language of the translation printed on the form.
   * @return noticeLanguage
   */
  @NotNull @Size(max = 10) @Pattern(regexp = "^[a-z]{2,3}(-[A-Z]{2})?$") 
  @JsonProperty("noticeLanguage")
  public String getNoticeLanguage() {
    return noticeLanguage;
  }

  @JsonProperty("noticeLanguage")
  public void setNoticeLanguage(String noticeLanguage) {
    this.noticeLanguage = noticeLanguage;
  }

  public RecordConsentOnBehalfRequest evidenceFileId(UUID evidenceFileId) {
    this.evidenceFileId = evidenceFileId;
    return this;
  }

  /**
   * The scanned signed form (a platform file).
   * @return evidenceFileId
   */
  @NotNull @Valid 
  @JsonProperty("evidenceFileId")
  public UUID getEvidenceFileId() {
    return evidenceFileId;
  }

  @JsonProperty("evidenceFileId")
  public void setEvidenceFileId(UUID evidenceFileId) {
    this.evidenceFileId = evidenceFileId;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    RecordConsentOnBehalfRequest other = (RecordConsentOnBehalfRequest) o;
    return Objects.equals(this.subjectPersonId, other.subjectPersonId) &&
        Objects.equals(this.giverPersonId, other.giverPersonId) &&
        Objects.equals(this.giverCapacity, other.giverCapacity) &&
        Objects.equals(this.purposeCode, other.purposeCode) &&
        Objects.equals(this.scope, other.scope) &&
        Objects.equals(this.decision, other.decision) &&
        Objects.equals(this.noticeVersionId, other.noticeVersionId) &&
        Objects.equals(this.noticeLanguage, other.noticeLanguage) &&
        Objects.equals(this.evidenceFileId, other.evidenceFileId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(subjectPersonId, giverPersonId, giverCapacity, purposeCode, scope, decision, noticeVersionId, noticeLanguage, evidenceFileId);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class RecordConsentOnBehalfRequest {\n");
    sb.append("    subjectPersonId: ").append(toIndentedString(subjectPersonId)).append("\n");
    sb.append("    giverPersonId: ").append(toIndentedString(giverPersonId)).append("\n");
    sb.append("    giverCapacity: ").append(toIndentedString(giverCapacity)).append("\n");
    sb.append("    purposeCode: ").append(toIndentedString(purposeCode)).append("\n");
    sb.append("    scope: ").append(toIndentedString(scope)).append("\n");
    sb.append("    decision: ").append(toIndentedString(decision)).append("\n");
    sb.append("    noticeVersionId: ").append(toIndentedString(noticeVersionId)).append("\n");
    sb.append("    noticeLanguage: ").append(toIndentedString(noticeLanguage)).append("\n");
    sb.append("    evidenceFileId: ").append(toIndentedString(evidenceFileId)).append("\n");
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
