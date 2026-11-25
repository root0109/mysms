// Owned API contract of privacy.
package com.mysms.privacy.api.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.lang.Nullable;

/**
 * One row of the consent centre: one subject (the caller or a child) and one purpose the school has switched on, with what the caller may decide, the caller's own decision and the effective result; never another giver's decision.
 */

public class MyConsent {

  /**
   * The width the caller decided at (tenant for a tenant-scoped purpose); absent while undecided.
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
   * The caller's own decision: granted, refused, not_answered, reconsent_required (the notice asks again) or expired; absent on a minor_self row (the caller does not decide).
   */
  public enum MyStatusEnum {
    GRANTED("granted"),
    
    REFUSED("refused"),
    
    NOT_ANSWERED("not_answered"),
    
    RECONSENT_REQUIRED("reconsent_required"),
    
    EXPIRED("expired");

    private final String value;

    MyStatusEnum(String value) {
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
    public static MyStatusEnum fromValue(String value) {
      for (MyStatusEnum b : MyStatusEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }

  /**
   * The widest width processing is currently allowed at, every legal guardian's decision taken together; absent when it is allowed at none.
   */
  public enum EffectiveWidestScopeEnum {
    CLASS("class"),
    
    INSTITUTION("institution"),
    
    INTER_SCHOOL("inter_school"),
    
    PUBLIC("public"),
    
    TENANT("tenant");

    private final String value;

    EffectiveWidestScopeEnum(String value) {
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
    public static EffectiveWidestScopeEnum fromValue(String value) {
      for (EffectiveWidestScopeEnum b : EffectiveWidestScopeEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }

  /**
   * How the caller stands to the subject: self (an adult deciding for themself), guardian (deciding for a minor they are the active legal guardian of) or minor_self (a minor looking at their own row: read only, their legal guardians decide until the minor comes of age).
   */
  public enum CapacityEnum {
    SELF("self"),
    
    GUARDIAN("guardian"),
    
    MINOR_SELF("minor_self");

    private final String value;

    CapacityEnum(String value) {
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
    public static CapacityEnum fromValue(String value) {
      for (CapacityEnum b : CapacityEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }

  /**
   * The widths the purpose can be decided at within the school's ceiling, narrowest first (tenant alone for a tenant-scoped purpose).
   */
  public enum AllowedScopesEnum {
    CLASS("class"),
    
    INSTITUTION("institution"),
    
    INTER_SCHOOL("inter_school"),
    
    PUBLIC("public"),
    
    TENANT("tenant");

    private final String value;

    AllowedScopesEnum(String value) {
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
    public static AllowedScopesEnum fromValue(String value) {
      for (AllowedScopesEnum b : AllowedScopesEnum.values()) {
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
  private UUID subjectPersonId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private String purposeCode;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable ScopeEnum scope;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable MyStatusEnum myStatus;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable EffectiveWidestScopeEnum effectiveWidestScope;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private @Nullable OffsetDateTime decidedAt;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Integer noticeVersion;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String channel;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String etag;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private CapacityEnum capacity;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String noticeCode;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID noticeVersionId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private List<AllowedScopesEnum> allowedScopes = new ArrayList<>();

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  private @Nullable LocalDate decidesFrom;

  public MyConsent() {
    super();
  }

  public MyConsent id(@Nullable UUID id) {
    this.id = id;
    return this;
  }

  /**
   * The caller's live consent record; absent while the caller has not decided. Output only.
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

  public MyConsent subjectPersonId(UUID subjectPersonId) {
    this.subjectPersonId = subjectPersonId;
    return this;
  }

  /**
   * The person the decision is about: the caller, or a child the caller is the legal guardian of.
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

  public MyConsent purposeCode(String purposeCode) {
    this.purposeCode = purposeCode;
    return this;
  }

  /**
   * The catalogue purpose (privacy.purpose.code).
   * @return purposeCode
   */
  @NotNull 
  @JsonProperty("purposeCode")
  public String getPurposeCode() {
    return purposeCode;
  }

  @JsonProperty("purposeCode")
  public void setPurposeCode(String purposeCode) {
    this.purposeCode = purposeCode;
  }

  public MyConsent scope(@Nullable ScopeEnum scope) {
    this.scope = scope;
    return this;
  }

  /**
   * The width the caller decided at (tenant for a tenant-scoped purpose); absent while undecided.
   * @return scope
   */
   
  @JsonProperty("scope")
  public @Nullable ScopeEnum getScope() {
    return scope;
  }

  @JsonProperty("scope")
  public void setScope(@Nullable ScopeEnum scope) {
    this.scope = scope;
  }

  public MyConsent myStatus(@Nullable MyStatusEnum myStatus) {
    this.myStatus = myStatus;
    return this;
  }

  /**
   * The caller's own decision: granted, refused, not_answered, reconsent_required (the notice asks again) or expired; absent on a minor_self row (the caller does not decide).
   * @return myStatus
   */
   
  @JsonProperty("myStatus")
  public @Nullable MyStatusEnum getMyStatus() {
    return myStatus;
  }

  @JsonProperty("myStatus")
  public void setMyStatus(@Nullable MyStatusEnum myStatus) {
    this.myStatus = myStatus;
  }

  public MyConsent effectiveWidestScope(@Nullable EffectiveWidestScopeEnum effectiveWidestScope) {
    this.effectiveWidestScope = effectiveWidestScope;
    return this;
  }

  /**
   * The widest width processing is currently allowed at, every legal guardian's decision taken together; absent when it is allowed at none.
   * @return effectiveWidestScope
   */
   
  @JsonProperty("effectiveWidestScope")
  public @Nullable EffectiveWidestScopeEnum getEffectiveWidestScope() {
    return effectiveWidestScope;
  }

  @JsonProperty("effectiveWidestScope")
  public void setEffectiveWidestScope(@Nullable EffectiveWidestScopeEnum effectiveWidestScope) {
    this.effectiveWidestScope = effectiveWidestScope;
  }

  public MyConsent decidedAt(@Nullable OffsetDateTime decidedAt) {
    this.decidedAt = decidedAt;
    return this;
  }

  /**
   * When the caller decided; absent while undecided.
   * @return decidedAt
   */
  @Valid 
  @JsonProperty("decidedAt")
  public @Nullable OffsetDateTime getDecidedAt() {
    return decidedAt;
  }

  @JsonProperty("decidedAt")
  public void setDecidedAt(@Nullable OffsetDateTime decidedAt) {
    this.decidedAt = decidedAt;
  }

  public MyConsent noticeVersion(@Nullable Integer noticeVersion) {
    this.noticeVersion = noticeVersion;
    return this;
  }

  /**
   * Number of the notice version the caller decided under; absent while undecided.
   * @return noticeVersion
   */
   
  @JsonProperty("noticeVersion")
  public @Nullable Integer getNoticeVersion() {
    return noticeVersion;
  }

  @JsonProperty("noticeVersion")
  public void setNoticeVersion(@Nullable Integer noticeVersion) {
    this.noticeVersion = noticeVersion;
  }

  public MyConsent channel(@Nullable String channel) {
    this.channel = channel;
    return this;
  }

  /**
   * How the caller's decision was given (web, paper_form ...); absent while undecided.
   * @return channel
   */
   
  @JsonProperty("channel")
  public @Nullable String getChannel() {
    return channel;
  }

  @JsonProperty("channel")
  public void setChannel(@Nullable String channel) {
    this.channel = channel;
  }

  public MyConsent etag(@Nullable String etag) {
    this.etag = etag;
    return this;
  }

  /**
   * Entity tag of the caller's consent record; send back on withdraw.
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

  public MyConsent capacity(CapacityEnum capacity) {
    this.capacity = capacity;
    return this;
  }

  /**
   * How the caller stands to the subject: self (an adult deciding for themself), guardian (deciding for a minor they are the active legal guardian of) or minor_self (a minor looking at their own row: read only, their legal guardians decide until the minor comes of age).
   * @return capacity
   */
  @NotNull 
  @JsonProperty("capacity")
  public CapacityEnum getCapacity() {
    return capacity;
  }

  @JsonProperty("capacity")
  public void setCapacity(CapacityEnum capacity) {
    this.capacity = capacity;
  }

  public MyConsent noticeCode(@Nullable String noticeCode) {
    this.noticeCode = noticeCode;
    return this;
  }

  /**
   * Code of the school's notice for the purpose, to read with GetMyNotice; absent while the notice has no published version (nothing can be decided then).
   * @return noticeCode
   */
   
  @JsonProperty("noticeCode")
  public @Nullable String getNoticeCode() {
    return noticeCode;
  }

  @JsonProperty("noticeCode")
  public void setNoticeCode(@Nullable String noticeCode) {
    this.noticeCode = noticeCode;
  }

  public MyConsent noticeVersionId(@Nullable UUID noticeVersionId) {
    this.noticeVersionId = noticeVersionId;
    return this;
  }

  /**
   * The notice's active version, the one a decision is recorded under; absent with noticeCode.
   * @return noticeVersionId
   */
  @Valid 
  @JsonProperty("noticeVersionId")
  public @Nullable UUID getNoticeVersionId() {
    return noticeVersionId;
  }

  @JsonProperty("noticeVersionId")
  public void setNoticeVersionId(@Nullable UUID noticeVersionId) {
    this.noticeVersionId = noticeVersionId;
  }

  public MyConsent allowedScopes(List<AllowedScopesEnum> allowedScopes) {
    this.allowedScopes = allowedScopes;
    return this;
  }

  public MyConsent addAllowedScopesItem(AllowedScopesEnum allowedScopesItem) {
    if (this.allowedScopes == null) {
      this.allowedScopes = new ArrayList<>();
    }
    this.allowedScopes.add(allowedScopesItem);
    return this;
  }

  /**
   * The widths the purpose can be decided at within the school's ceiling, narrowest first (tenant alone for a tenant-scoped purpose).
   * @return allowedScopes
   */
  @NotNull 
  @JsonProperty("allowedScopes")
  public List<AllowedScopesEnum> getAllowedScopes() {
    return allowedScopes;
  }

  @JsonProperty("allowedScopes")
  public void setAllowedScopes(List<AllowedScopesEnum> allowedScopes) {
    this.allowedScopes = allowedScopes;
  }

  public MyConsent decidesFrom(@Nullable LocalDate decidesFrom) {
    this.decidesFrom = decidesFrom;
    return this;
  }

  /**
   * On a minor_self row only: the day the caller comes of age and decides for themself.
   * @return decidesFrom
   */
  @Valid 
  @JsonProperty("decidesFrom")
  public @Nullable LocalDate getDecidesFrom() {
    return decidesFrom;
  }

  @JsonProperty("decidesFrom")
  public void setDecidesFrom(@Nullable LocalDate decidesFrom) {
    this.decidesFrom = decidesFrom;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    MyConsent other = (MyConsent) o;
    return Objects.equals(this.id, other.id) &&
        Objects.equals(this.subjectPersonId, other.subjectPersonId) &&
        Objects.equals(this.purposeCode, other.purposeCode) &&
        Objects.equals(this.scope, other.scope) &&
        Objects.equals(this.myStatus, other.myStatus) &&
        Objects.equals(this.effectiveWidestScope, other.effectiveWidestScope) &&
        Objects.equals(this.decidedAt, other.decidedAt) &&
        Objects.equals(this.noticeVersion, other.noticeVersion) &&
        Objects.equals(this.channel, other.channel) &&
        Objects.equals(this.etag, other.etag) &&
        Objects.equals(this.capacity, other.capacity) &&
        Objects.equals(this.noticeCode, other.noticeCode) &&
        Objects.equals(this.noticeVersionId, other.noticeVersionId) &&
        Objects.equals(this.allowedScopes, other.allowedScopes) &&
        Objects.equals(this.decidesFrom, other.decidesFrom);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, subjectPersonId, purposeCode, scope, myStatus, effectiveWidestScope, decidedAt, noticeVersion, channel, etag, capacity, noticeCode, noticeVersionId, allowedScopes, decidesFrom);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class MyConsent {\n");
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    subjectPersonId: ").append(toIndentedString(subjectPersonId)).append("\n");
    sb.append("    purposeCode: ").append(toIndentedString(purposeCode)).append("\n");
    sb.append("    scope: ").append(toIndentedString(scope)).append("\n");
    sb.append("    myStatus: ").append(toIndentedString(myStatus)).append("\n");
    sb.append("    effectiveWidestScope: ").append(toIndentedString(effectiveWidestScope)).append("\n");
    sb.append("    decidedAt: ").append(toIndentedString(decidedAt)).append("\n");
    sb.append("    noticeVersion: ").append(toIndentedString(noticeVersion)).append("\n");
    sb.append("    channel: ").append(toIndentedString(channel)).append("\n");
    sb.append("    etag: ").append(toIndentedString(etag)).append("\n");
    sb.append("    capacity: ").append(toIndentedString(capacity)).append("\n");
    sb.append("    noticeCode: ").append(toIndentedString(noticeCode)).append("\n");
    sb.append("    noticeVersionId: ").append(toIndentedString(noticeVersionId)).append("\n");
    sb.append("    allowedScopes: ").append(toIndentedString(allowedScopes)).append("\n");
    sb.append("    decidesFrom: ").append(toIndentedString(decidesFrom)).append("\n");
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
