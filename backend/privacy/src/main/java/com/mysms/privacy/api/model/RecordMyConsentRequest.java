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
 * Body of RecordMyConsent: a decision about oneself (an adult) or a child one is the active legal guardian of. The giver is always the caller, never a field.
 */

public class RecordMyConsentRequest {

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
  private String purposeCode;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private ScopeEnum scope;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private DecisionEnum decision;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private UUID noticeVersionId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private String noticeLanguage;

  public RecordMyConsentRequest() {
    super();
  }

  public RecordMyConsentRequest subjectPersonId(UUID subjectPersonId) {
    this.subjectPersonId = subjectPersonId;
    return this;
  }

  /**
   * The caller, or a minor the caller is the active legal guardian of.
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

  public RecordMyConsentRequest purposeCode(String purposeCode) {
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

  public RecordMyConsentRequest scope(ScopeEnum scope) {
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

  public RecordMyConsentRequest decision(DecisionEnum decision) {
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

  public RecordMyConsentRequest noticeVersionId(UUID noticeVersionId) {
    this.noticeVersionId = noticeVersionId;
    return this;
  }

  /**
   * The active notice version shown.
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

  public RecordMyConsentRequest noticeLanguage(String noticeLanguage) {
    this.noticeLanguage = noticeLanguage;
    return this;
  }

  /**
   * The language of the translation shown (BCP 47).
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

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    RecordMyConsentRequest other = (RecordMyConsentRequest) o;
    return Objects.equals(this.subjectPersonId, other.subjectPersonId) &&
        Objects.equals(this.purposeCode, other.purposeCode) &&
        Objects.equals(this.scope, other.scope) &&
        Objects.equals(this.decision, other.decision) &&
        Objects.equals(this.noticeVersionId, other.noticeVersionId) &&
        Objects.equals(this.noticeLanguage, other.noticeLanguage);
  }

  @Override
  public int hashCode() {
    return Objects.hash(subjectPersonId, purposeCode, scope, decision, noticeVersionId, noticeLanguage);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class RecordMyConsentRequest {\n");
    sb.append("    subjectPersonId: ").append(toIndentedString(subjectPersonId)).append("\n");
    sb.append("    purposeCode: ").append(toIndentedString(purposeCode)).append("\n");
    sb.append("    scope: ").append(toIndentedString(scope)).append("\n");
    sb.append("    decision: ").append(toIndentedString(decision)).append("\n");
    sb.append("    noticeVersionId: ").append(toIndentedString(noticeVersionId)).append("\n");
    sb.append("    noticeLanguage: ").append(toIndentedString(noticeLanguage)).append("\n");
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
