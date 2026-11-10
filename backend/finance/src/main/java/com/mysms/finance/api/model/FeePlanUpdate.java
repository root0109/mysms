// Owned API contract of finance (JAVA-CODING-RULES §7); first materialized from api/openapi/finance.yaml on 2026-11-10.
package com.mysms.finance.api.model;

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
 * Fields of FeePlan that can be updated (partial update; see updateMask).
 */

public class FeePlanUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID institutionId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String code;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String name;

  /**
   * Installments due after N days or on fixed dates.
   */
  public enum ScheduleBasisEnum {
    FIXED_DAYS("fixed_days"),
    
    FIXED_DATE("fixed_date");

    private final String value;

    ScheduleBasisEnum(String value) {
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
    public static ScheduleBasisEnum fromValue(String value) {
      for (ScheduleBasisEnum b : ScheduleBasisEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable ScheduleBasisEnum scheduleBasis;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Integer offsetDays;

  /**
   * Before or after the reference date.
   */
  public enum OffsetDirectionEnum {
    BEFORE("before"),
    
    AFTER("after");

    private final String value;

    OffsetDirectionEnum(String value) {
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
    public static OffsetDirectionEnum fromValue(String value) {
      for (OffsetDirectionEnum b : OffsetDirectionEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable OffsetDirectionEnum offsetDirection;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String discountPercent;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String notes;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Boolean isActive;

  public FeePlanUpdate institutionId(@Nullable UUID institutionId) {
    this.institutionId = institutionId;
    return this;
  }

  /**
   * Institution.
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

  public FeePlanUpdate code(@Nullable String code) {
    this.code = code;
    return this;
  }

  /**
   * Plan code.
   * @return code
   */
  @Size(max = 40) 
  @JsonProperty("code")
  public @Nullable String getCode() {
    return code;
  }

  @JsonProperty("code")
  public void setCode(@Nullable String code) {
    this.code = code;
  }

  public FeePlanUpdate name(@Nullable String name) {
    this.name = name;
    return this;
  }

  /**
   * Plan name.
   * @return name
   */
  @Size(max = 150) 
  @JsonProperty("name")
  public @Nullable String getName() {
    return name;
  }

  @JsonProperty("name")
  public void setName(@Nullable String name) {
    this.name = name;
  }

  public FeePlanUpdate scheduleBasis(@Nullable ScheduleBasisEnum scheduleBasis) {
    this.scheduleBasis = scheduleBasis;
    return this;
  }

  /**
   * Installments due after N days or on fixed dates.
   * @return scheduleBasis
   */
  
  @JsonProperty("scheduleBasis")
  public @Nullable ScheduleBasisEnum getScheduleBasis() {
    return scheduleBasis;
  }

  @JsonProperty("scheduleBasis")
  public void setScheduleBasis(@Nullable ScheduleBasisEnum scheduleBasis) {
    this.scheduleBasis = scheduleBasis;
  }

  public FeePlanUpdate offsetDays(@Nullable Integer offsetDays) {
    this.offsetDays = offsetDays;
    return this;
  }

  /**
   * Days used with offset_direction for the first installment.
   * @return offsetDays
   */
  
  @JsonProperty("offsetDays")
  public @Nullable Integer getOffsetDays() {
    return offsetDays;
  }

  @JsonProperty("offsetDays")
  public void setOffsetDays(@Nullable Integer offsetDays) {
    this.offsetDays = offsetDays;
  }

  public FeePlanUpdate offsetDirection(@Nullable OffsetDirectionEnum offsetDirection) {
    this.offsetDirection = offsetDirection;
    return this;
  }

  /**
   * Before or after the reference date.
   * @return offsetDirection
   */
  
  @JsonProperty("offsetDirection")
  public @Nullable OffsetDirectionEnum getOffsetDirection() {
    return offsetDirection;
  }

  @JsonProperty("offsetDirection")
  public void setOffsetDirection(@Nullable OffsetDirectionEnum offsetDirection) {
    this.offsetDirection = offsetDirection;
  }

  public FeePlanUpdate discountPercent(@Nullable String discountPercent) {
    this.discountPercent = discountPercent;
    return this;
  }

  /**
   * Plan-level discount.
   * @return discountPercent
   */
  @Pattern(regexp = "^-?\\d+(\\.\\d+)?$") 
  @JsonProperty("discountPercent")
  public @Nullable String getDiscountPercent() {
    return discountPercent;
  }

  @JsonProperty("discountPercent")
  public void setDiscountPercent(@Nullable String discountPercent) {
    this.discountPercent = discountPercent;
  }

  public FeePlanUpdate notes(@Nullable String notes) {
    this.notes = notes;
    return this;
  }

  /**
   * Notes.
   * @return notes
   */
  
  @JsonProperty("notes")
  public @Nullable String getNotes() {
    return notes;
  }

  @JsonProperty("notes")
  public void setNotes(@Nullable String notes) {
    this.notes = notes;
  }

  public FeePlanUpdate isActive(@Nullable Boolean isActive) {
    this.isActive = isActive;
    return this;
  }

  /**
   * Whether usable.
   * @return isActive
   */
  
  @JsonProperty("isActive")
  public @Nullable Boolean getIsActive() {
    return isActive;
  }

  @JsonProperty("isActive")
  public void setIsActive(@Nullable Boolean isActive) {
    this.isActive = isActive;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    FeePlanUpdate feePlanUpdate = (FeePlanUpdate) o;
    return Objects.equals(this.institutionId, feePlanUpdate.institutionId) &&
        Objects.equals(this.code, feePlanUpdate.code) &&
        Objects.equals(this.name, feePlanUpdate.name) &&
        Objects.equals(this.scheduleBasis, feePlanUpdate.scheduleBasis) &&
        Objects.equals(this.offsetDays, feePlanUpdate.offsetDays) &&
        Objects.equals(this.offsetDirection, feePlanUpdate.offsetDirection) &&
        Objects.equals(this.discountPercent, feePlanUpdate.discountPercent) &&
        Objects.equals(this.notes, feePlanUpdate.notes) &&
        Objects.equals(this.isActive, feePlanUpdate.isActive);
  }

  @Override
  public int hashCode() {
    return Objects.hash(institutionId, code, name, scheduleBasis, offsetDays, offsetDirection, discountPercent, notes, isActive);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class FeePlanUpdate {\n");
    sb.append("    institutionId: ").append(toIndentedString(institutionId)).append("\n");
    sb.append("    code: ").append(toIndentedString(code)).append("\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    scheduleBasis: ").append(toIndentedString(scheduleBasis)).append("\n");
    sb.append("    offsetDays: ").append(toIndentedString(offsetDays)).append("\n");
    sb.append("    offsetDirection: ").append(toIndentedString(offsetDirection)).append("\n");
    sb.append("    discountPercent: ").append(toIndentedString(discountPercent)).append("\n");
    sb.append("    notes: ").append(toIndentedString(notes)).append("\n");
    sb.append("    isActive: ").append(toIndentedString(isActive)).append("\n");
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

