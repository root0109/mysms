// Owned API contract of finance (JAVA-CODING-RULES §7); first materialized from api/openapi/finance.yaml on 2026-11-10.
package com.mysms.finance.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.lang.Nullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;

/**
 * Fields of FeeConcession that can be updated (partial update; see updateMask).
 */

public class FeeConcessionUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID institutionId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID feeHeadId;

  /**
   * percent or amount.
   */
  public enum ConcessionTypeEnum {
    PERCENT("percent"),
    
    AMOUNT("amount");

    private final String value;

    ConcessionTypeEnum(String value) {
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
    public static ConcessionTypeEnum fromValue(String value) {
      for (ConcessionTypeEnum b : ConcessionTypeEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable ConcessionTypeEnum concessionType;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String concessionValue;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String reason;

  /**
   * active / revoked.
   */
  public enum StatusEnum {
    ACTIVE("active"),
    
    REVOKED("revoked");

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

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable StatusEnum status;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  private @Nullable LocalDate validFrom;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  private @Nullable LocalDate validUntil;

  public FeeConcessionUpdate institutionId(@Nullable UUID institutionId) {
    this.institutionId = institutionId;
    return this;
  }

  /**
   * Institution (NULL = tenant-wide).
   * @return institutionId
   */
  @Valid 
  @JsonProperty("institutionId")
  public @Nullable UUID getInstitutionId() {
    return institutionId;
  }

  @JsonProperty("institutionId")
  public void setInstitutionId(@Nullable UUID institutionId) {
    this.institutionId = institutionId;
  }

  public FeeConcessionUpdate feeHeadId(@Nullable UUID feeHeadId) {
    this.feeHeadId = feeHeadId;
    return this;
  }

  /**
   * Fee head it applies to (NULL = all heads).
   * @return feeHeadId
   */
  @Valid 
  @JsonProperty("feeHeadId")
  public @Nullable UUID getFeeHeadId() {
    return feeHeadId;
  }

  @JsonProperty("feeHeadId")
  public void setFeeHeadId(@Nullable UUID feeHeadId) {
    this.feeHeadId = feeHeadId;
  }

  public FeeConcessionUpdate concessionType(@Nullable ConcessionTypeEnum concessionType) {
    this.concessionType = concessionType;
    return this;
  }

  /**
   * percent or amount.
   * @return concessionType
   */
  
  @JsonProperty("concessionType")
  public @Nullable ConcessionTypeEnum getConcessionType() {
    return concessionType;
  }

  @JsonProperty("concessionType")
  public void setConcessionType(@Nullable ConcessionTypeEnum concessionType) {
    this.concessionType = concessionType;
  }

  public FeeConcessionUpdate concessionValue(@Nullable String concessionValue) {
    this.concessionValue = concessionValue;
    return this;
  }

  /**
   * Percentage (0-100) or absolute amount per the type.
   * @return concessionValue
   */
  @Pattern(regexp = "^-?\\d+(\\.\\d+)?$") 
  @JsonProperty("concessionValue")
  public @Nullable String getConcessionValue() {
    return concessionValue;
  }

  @JsonProperty("concessionValue")
  public void setConcessionValue(@Nullable String concessionValue) {
    this.concessionValue = concessionValue;
  }

  public FeeConcessionUpdate reason(@Nullable String reason) {
    this.reason = reason;
    return this;
  }

  /**
   * Why it was granted.
   * @return reason
   */
  @Size(max = 300) 
  @JsonProperty("reason")
  public @Nullable String getReason() {
    return reason;
  }

  @JsonProperty("reason")
  public void setReason(@Nullable String reason) {
    this.reason = reason;
  }

  public FeeConcessionUpdate status(@Nullable StatusEnum status) {
    this.status = status;
    return this;
  }

  /**
   * active / revoked.
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

  public FeeConcessionUpdate validFrom(@Nullable LocalDate validFrom) {
    this.validFrom = validFrom;
    return this;
  }

  /**
   * Start of validity (NULL = whole year).
   * @return validFrom
   */
  @Valid 
  @JsonProperty("validFrom")
  public @Nullable LocalDate getValidFrom() {
    return validFrom;
  }

  @JsonProperty("validFrom")
  public void setValidFrom(@Nullable LocalDate validFrom) {
    this.validFrom = validFrom;
  }

  public FeeConcessionUpdate validUntil(@Nullable LocalDate validUntil) {
    this.validUntil = validUntil;
    return this;
  }

  /**
   * End of validity (NULL = whole year).
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

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    FeeConcessionUpdate feeConcessionUpdate = (FeeConcessionUpdate) o;
    return Objects.equals(this.institutionId, feeConcessionUpdate.institutionId) &&
        Objects.equals(this.feeHeadId, feeConcessionUpdate.feeHeadId) &&
        Objects.equals(this.concessionType, feeConcessionUpdate.concessionType) &&
        Objects.equals(this.concessionValue, feeConcessionUpdate.concessionValue) &&
        Objects.equals(this.reason, feeConcessionUpdate.reason) &&
        Objects.equals(this.status, feeConcessionUpdate.status) &&
        Objects.equals(this.validFrom, feeConcessionUpdate.validFrom) &&
        Objects.equals(this.validUntil, feeConcessionUpdate.validUntil);
  }

  @Override
  public int hashCode() {
    return Objects.hash(institutionId, feeHeadId, concessionType, concessionValue, reason, status, validFrom, validUntil);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class FeeConcessionUpdate {\n");
    sb.append("    institutionId: ").append(toIndentedString(institutionId)).append("\n");
    sb.append("    feeHeadId: ").append(toIndentedString(feeHeadId)).append("\n");
    sb.append("    concessionType: ").append(toIndentedString(concessionType)).append("\n");
    sb.append("    concessionValue: ").append(toIndentedString(concessionValue)).append("\n");
    sb.append("    reason: ").append(toIndentedString(reason)).append("\n");
    sb.append("    status: ").append(toIndentedString(status)).append("\n");
    sb.append("    validFrom: ").append(toIndentedString(validFrom)).append("\n");
    sb.append("    validUntil: ").append(toIndentedString(validUntil)).append("\n");
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

