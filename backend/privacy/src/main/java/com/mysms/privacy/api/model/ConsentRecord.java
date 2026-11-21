// Owned API contract of privacy.
package com.mysms.privacy.api.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.lang.Nullable;

/**
 * One consent decision as staff see it: a giver's granted or refused for one subject, purpose and width, how it was given and whether it was verified.
 */

public class ConsentRecord {

  /**
   * self (an adult subject), guardian (a legal guardian of a minor) or nominee.
   */
  public enum GiverCapacityEnum {
    SELF("self"),
    
    GUARDIAN("guardian"),
    
    NOMINEE("nominee");

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

  /**
   * How the decision was given.
   */
  public enum ChannelEnum {
    WEB("web"),
    
    MOBILE_APP("mobile_app"),
    
    PAPER_FORM("paper_form"),
    
    IN_PERSON("in_person"),
    
    EMAIL("email"),
    
    CONSENT_MANAGER("consent_manager"),
    
    IMPORT("import");

    private final String value;

    ChannelEnum(String value) {
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
    public static ChannelEnum fromValue(String value) {
      for (ChannelEnum b : ChannelEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }

  /**
   * verified (counts), unverified (a paper form awaiting a second staff member) or rejected.
   */
  public enum VerificationStatusEnum {
    VERIFIED("verified"),
    
    UNVERIFIED("unverified"),
    
    REJECTED("rejected");

    private final String value;

    VerificationStatusEnum(String value) {
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
    public static VerificationStatusEnum fromValue(String value) {
      for (VerificationStatusEnum b : VerificationStatusEnum.values()) {
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
  private @Nullable UUID subjectPersonId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID giverPersonId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable GiverCapacityEnum giverCapacity;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String purposeCode;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable ScopeEnum scope;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable DecisionEnum decision;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID noticeVersionId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String noticeLanguage;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private @Nullable OffsetDateTime validFrom;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  private @Nullable LocalDate validUntil;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private @Nullable OffsetDateTime withdrawnAt;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID supersededById;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable ChannelEnum channel;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable VerificationStatusEnum verificationStatus;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String verificationMethod;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID verifiedByUserId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private @Nullable OffsetDateTime verifiedAt;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID recordedByUserId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID evidenceFileId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private @Nullable OffsetDateTime createTime;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID createdBy;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private @Nullable OffsetDateTime updateTime;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID updatedBy;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String etag;

  public ConsentRecord() {
    super();
  }

  public ConsentRecord id(@Nullable UUID id) {
    this.id = id;
    return this;
  }

  /**
   * Surrogate primary key (UUIDv7 generated by the application). Output only.
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

  public ConsentRecord subjectPersonId(@Nullable UUID subjectPersonId) {
    this.subjectPersonId = subjectPersonId;
    return this;
  }

  /**
   * The person the decision is about.
   * @return subjectPersonId
   */
  @Valid 
  @JsonProperty("subjectPersonId")
  public @Nullable UUID getSubjectPersonId() {
    return subjectPersonId;
  }

  @JsonProperty("subjectPersonId")
  public void setSubjectPersonId(@Nullable UUID subjectPersonId) {
    this.subjectPersonId = subjectPersonId;
  }

  public ConsentRecord giverPersonId(@Nullable UUID giverPersonId) {
    this.giverPersonId = giverPersonId;
    return this;
  }

  /**
   * The person who decided.
   * @return giverPersonId
   */
  @Valid 
  @JsonProperty("giverPersonId")
  public @Nullable UUID getGiverPersonId() {
    return giverPersonId;
  }

  @JsonProperty("giverPersonId")
  public void setGiverPersonId(@Nullable UUID giverPersonId) {
    this.giverPersonId = giverPersonId;
  }

  public ConsentRecord giverCapacity(@Nullable GiverCapacityEnum giverCapacity) {
    this.giverCapacity = giverCapacity;
    return this;
  }

  /**
   * self (an adult subject), guardian (a legal guardian of a minor) or nominee.
   * @return giverCapacity
   */
   
  @JsonProperty("giverCapacity")
  public @Nullable GiverCapacityEnum getGiverCapacity() {
    return giverCapacity;
  }

  @JsonProperty("giverCapacity")
  public void setGiverCapacity(@Nullable GiverCapacityEnum giverCapacity) {
    this.giverCapacity = giverCapacity;
  }

  public ConsentRecord purposeCode(@Nullable String purposeCode) {
    this.purposeCode = purposeCode;
    return this;
  }

  /**
   * The catalogue purpose.
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

  public ConsentRecord scope(@Nullable ScopeEnum scope) {
    this.scope = scope;
    return this;
  }

  /**
   * The width decided at; tenant for a tenant-scoped purpose.
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

  public ConsentRecord decision(@Nullable DecisionEnum decision) {
    this.decision = decision;
    return this;
  }

  /**
   * granted or refused.
   * @return decision
   */
   
  @JsonProperty("decision")
  public @Nullable DecisionEnum getDecision() {
    return decision;
  }

  @JsonProperty("decision")
  public void setDecision(@Nullable DecisionEnum decision) {
    this.decision = decision;
  }

  public ConsentRecord noticeVersionId(@Nullable UUID noticeVersionId) {
    this.noticeVersionId = noticeVersionId;
    return this;
  }

  /**
   * The notice version shown.
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

  public ConsentRecord noticeLanguage(@Nullable String noticeLanguage) {
    this.noticeLanguage = noticeLanguage;
    return this;
  }

  /**
   * The language of the translation shown.
   * @return noticeLanguage
   */
   
  @JsonProperty("noticeLanguage")
  public @Nullable String getNoticeLanguage() {
    return noticeLanguage;
  }

  @JsonProperty("noticeLanguage")
  public void setNoticeLanguage(@Nullable String noticeLanguage) {
    this.noticeLanguage = noticeLanguage;
  }

  public ConsentRecord validFrom(@Nullable OffsetDateTime validFrom) {
    this.validFrom = validFrom;
    return this;
  }

  /**
   * When the decision was recorded.
   * @return validFrom
   */
  @Valid 
  @JsonProperty("validFrom")
  public @Nullable OffsetDateTime getValidFrom() {
    return validFrom;
  }

  @JsonProperty("validFrom")
  public void setValidFrom(@Nullable OffsetDateTime validFrom) {
    this.validFrom = validFrom;
  }

  public ConsentRecord validUntil(@Nullable LocalDate validUntil) {
    this.validUntil = validUntil;
    return this;
  }

  /**
   * Last day the decision counts; absent = until withdrawn.
   * @return validUntil
   */
  @Valid 
  @JsonProperty("validUntil")
  public @Nullable LocalDate getValidUntil() {
    return validUntil;
  }

  @JsonProperty("validUntil")
  public void setValidUntil(@Nullable LocalDate validUntil) {
    this.validUntil = validUntil;
  }

  public ConsentRecord withdrawnAt(@Nullable OffsetDateTime withdrawnAt) {
    this.withdrawnAt = withdrawnAt;
    return this;
  }

  /**
   * When the giver withdrew it; absent = not withdrawn.
   * @return withdrawnAt
   */
  @Valid 
  @JsonProperty("withdrawnAt")
  public @Nullable OffsetDateTime getWithdrawnAt() {
    return withdrawnAt;
  }

  @JsonProperty("withdrawnAt")
  public void setWithdrawnAt(@Nullable OffsetDateTime withdrawnAt) {
    this.withdrawnAt = withdrawnAt;
  }

  public ConsentRecord supersededById(@Nullable UUID supersededById) {
    this.supersededById = supersededById;
    return this;
  }

  /**
   * The giver's newer decision that replaced it; absent = not superseded.
   * @return supersededById
   */
  @Valid 
  @JsonProperty("supersededById")
  public @Nullable UUID getSupersededById() {
    return supersededById;
  }

  @JsonProperty("supersededById")
  public void setSupersededById(@Nullable UUID supersededById) {
    this.supersededById = supersededById;
  }

  public ConsentRecord channel(@Nullable ChannelEnum channel) {
    this.channel = channel;
    return this;
  }

  /**
   * How the decision was given.
   * @return channel
   */
   
  @JsonProperty("channel")
  public @Nullable ChannelEnum getChannel() {
    return channel;
  }

  @JsonProperty("channel")
  public void setChannel(@Nullable ChannelEnum channel) {
    this.channel = channel;
  }

  public ConsentRecord verificationStatus(@Nullable VerificationStatusEnum verificationStatus) {
    this.verificationStatus = verificationStatus;
    return this;
  }

  /**
   * verified (counts), unverified (a paper form awaiting a second staff member) or rejected.
   * @return verificationStatus
   */
   
  @JsonProperty("verificationStatus")
  public @Nullable VerificationStatusEnum getVerificationStatus() {
    return verificationStatus;
  }

  @JsonProperty("verificationStatus")
  public void setVerificationStatus(@Nullable VerificationStatusEnum verificationStatus) {
    this.verificationStatus = verificationStatus;
  }

  public ConsentRecord verificationMethod(@Nullable String verificationMethod) {
    this.verificationMethod = verificationMethod;
    return this;
  }

  /**
   * How the giver was established (existing_record, paper_form_countersigned ...).
   * @return verificationMethod
   */
   
  @JsonProperty("verificationMethod")
  public @Nullable String getVerificationMethod() {
    return verificationMethod;
  }

  @JsonProperty("verificationMethod")
  public void setVerificationMethod(@Nullable String verificationMethod) {
    this.verificationMethod = verificationMethod;
  }

  public ConsentRecord verifiedByUserId(@Nullable UUID verifiedByUserId) {
    this.verifiedByUserId = verifiedByUserId;
    return this;
  }

  /**
   * The staff member who verified a paper form.
   * @return verifiedByUserId
   */
  @Valid 
  @JsonProperty("verifiedByUserId")
  public @Nullable UUID getVerifiedByUserId() {
    return verifiedByUserId;
  }

  @JsonProperty("verifiedByUserId")
  public void setVerifiedByUserId(@Nullable UUID verifiedByUserId) {
    this.verifiedByUserId = verifiedByUserId;
  }

  public ConsentRecord verifiedAt(@Nullable OffsetDateTime verifiedAt) {
    this.verifiedAt = verifiedAt;
    return this;
  }

  /**
   * When the record became verified.
   * @return verifiedAt
   */
  @Valid 
  @JsonProperty("verifiedAt")
  public @Nullable OffsetDateTime getVerifiedAt() {
    return verifiedAt;
  }

  @JsonProperty("verifiedAt")
  public void setVerifiedAt(@Nullable OffsetDateTime verifiedAt) {
    this.verifiedAt = verifiedAt;
  }

  public ConsentRecord recordedByUserId(@Nullable UUID recordedByUserId) {
    this.recordedByUserId = recordedByUserId;
    return this;
  }

  /**
   * The staff member who recorded a paper form.
   * @return recordedByUserId
   */
  @Valid 
  @JsonProperty("recordedByUserId")
  public @Nullable UUID getRecordedByUserId() {
    return recordedByUserId;
  }

  @JsonProperty("recordedByUserId")
  public void setRecordedByUserId(@Nullable UUID recordedByUserId) {
    this.recordedByUserId = recordedByUserId;
  }

  public ConsentRecord evidenceFileId(@Nullable UUID evidenceFileId) {
    this.evidenceFileId = evidenceFileId;
    return this;
  }

  /**
   * The scanned signed form (a platform file).
   * @return evidenceFileId
   */
  @Valid 
  @JsonProperty("evidenceFileId")
  public @Nullable UUID getEvidenceFileId() {
    return evidenceFileId;
  }

  @JsonProperty("evidenceFileId")
  public void setEvidenceFileId(@Nullable UUID evidenceFileId) {
    this.evidenceFileId = evidenceFileId;
  }

  public ConsentRecord createTime(@Nullable OffsetDateTime createTime) {
    this.createTime = createTime;
    return this;
  }

  /**
   * Row creation time. Output only.
   * @return createTime
   */
  @Valid 
  @JsonProperty("createTime")
  public @Nullable OffsetDateTime getCreateTime() {
    return createTime;
  }

  @JsonProperty("createTime")
  public void setCreateTime(@Nullable OffsetDateTime createTime) {
    this.createTime = createTime;
  }

  public ConsentRecord createdBy(@Nullable UUID createdBy) {
    this.createdBy = createdBy;
    return this;
  }

  /**
   * User account that created the row. Output only.
   * @return createdBy
   */
  @Valid 
  @JsonProperty("createdBy")
  public @Nullable UUID getCreatedBy() {
    return createdBy;
  }

  @JsonProperty("createdBy")
  public void setCreatedBy(@Nullable UUID createdBy) {
    this.createdBy = createdBy;
  }

  public ConsentRecord updateTime(@Nullable OffsetDateTime updateTime) {
    this.updateTime = updateTime;
    return this;
  }

  /**
   * Last update time. Output only.
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

  public ConsentRecord updatedBy(@Nullable UUID updatedBy) {
    this.updatedBy = updatedBy;
    return this;
  }

  /**
   * User account that last updated the row. Output only.
   * @return updatedBy
   */
  @Valid 
  @JsonProperty("updatedBy")
  public @Nullable UUID getUpdatedBy() {
    return updatedBy;
  }

  @JsonProperty("updatedBy")
  public void setUpdatedBy(@Nullable UUID updatedBy) {
    this.updatedBy = updatedBy;
  }

  public ConsentRecord etag(@Nullable String etag) {
    this.etag = etag;
    return this;
  }

  /**
   * Entity tag (row version); send back on verify.
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
    ConsentRecord other = (ConsentRecord) o;
    return Objects.equals(this.id, other.id) &&
        Objects.equals(this.subjectPersonId, other.subjectPersonId) &&
        Objects.equals(this.giverPersonId, other.giverPersonId) &&
        Objects.equals(this.giverCapacity, other.giverCapacity) &&
        Objects.equals(this.purposeCode, other.purposeCode) &&
        Objects.equals(this.scope, other.scope) &&
        Objects.equals(this.decision, other.decision) &&
        Objects.equals(this.noticeVersionId, other.noticeVersionId) &&
        Objects.equals(this.noticeLanguage, other.noticeLanguage) &&
        Objects.equals(this.validFrom, other.validFrom) &&
        Objects.equals(this.validUntil, other.validUntil) &&
        Objects.equals(this.withdrawnAt, other.withdrawnAt) &&
        Objects.equals(this.supersededById, other.supersededById) &&
        Objects.equals(this.channel, other.channel) &&
        Objects.equals(this.verificationStatus, other.verificationStatus) &&
        Objects.equals(this.verificationMethod, other.verificationMethod) &&
        Objects.equals(this.verifiedByUserId, other.verifiedByUserId) &&
        Objects.equals(this.verifiedAt, other.verifiedAt) &&
        Objects.equals(this.recordedByUserId, other.recordedByUserId) &&
        Objects.equals(this.evidenceFileId, other.evidenceFileId) &&
        Objects.equals(this.createTime, other.createTime) &&
        Objects.equals(this.createdBy, other.createdBy) &&
        Objects.equals(this.updateTime, other.updateTime) &&
        Objects.equals(this.updatedBy, other.updatedBy) &&
        Objects.equals(this.etag, other.etag);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, subjectPersonId, giverPersonId, giverCapacity, purposeCode, scope, decision, noticeVersionId, noticeLanguage, validFrom, validUntil, withdrawnAt, supersededById, channel, verificationStatus, verificationMethod, verifiedByUserId, verifiedAt, recordedByUserId, evidenceFileId, createTime, createdBy, updateTime, updatedBy, etag);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ConsentRecord {\n");
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    subjectPersonId: ").append(toIndentedString(subjectPersonId)).append("\n");
    sb.append("    giverPersonId: ").append(toIndentedString(giverPersonId)).append("\n");
    sb.append("    giverCapacity: ").append(toIndentedString(giverCapacity)).append("\n");
    sb.append("    purposeCode: ").append(toIndentedString(purposeCode)).append("\n");
    sb.append("    scope: ").append(toIndentedString(scope)).append("\n");
    sb.append("    decision: ").append(toIndentedString(decision)).append("\n");
    sb.append("    noticeVersionId: ").append(toIndentedString(noticeVersionId)).append("\n");
    sb.append("    noticeLanguage: ").append(toIndentedString(noticeLanguage)).append("\n");
    sb.append("    validFrom: ").append(toIndentedString(validFrom)).append("\n");
    sb.append("    validUntil: ").append(toIndentedString(validUntil)).append("\n");
    sb.append("    withdrawnAt: ").append(toIndentedString(withdrawnAt)).append("\n");
    sb.append("    supersededById: ").append(toIndentedString(supersededById)).append("\n");
    sb.append("    channel: ").append(toIndentedString(channel)).append("\n");
    sb.append("    verificationStatus: ").append(toIndentedString(verificationStatus)).append("\n");
    sb.append("    verificationMethod: ").append(toIndentedString(verificationMethod)).append("\n");
    sb.append("    verifiedByUserId: ").append(toIndentedString(verifiedByUserId)).append("\n");
    sb.append("    verifiedAt: ").append(toIndentedString(verifiedAt)).append("\n");
    sb.append("    recordedByUserId: ").append(toIndentedString(recordedByUserId)).append("\n");
    sb.append("    evidenceFileId: ").append(toIndentedString(evidenceFileId)).append("\n");
    sb.append("    createTime: ").append(toIndentedString(createTime)).append("\n");
    sb.append("    createdBy: ").append(toIndentedString(createdBy)).append("\n");
    sb.append("    updateTime: ").append(toIndentedString(updateTime)).append("\n");
    sb.append("    updatedBy: ").append(toIndentedString(updatedBy)).append("\n");
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
