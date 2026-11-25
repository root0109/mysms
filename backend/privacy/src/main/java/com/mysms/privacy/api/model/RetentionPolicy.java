// Owned API contract of privacy.
package com.mysms.privacy.api.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import java.util.Objects;
import java.util.UUID;
import org.springframework.lang.Nullable;

/**
 * How long the tenant keeps one kind of personal data and what happens after: the platform catalogue's policy with the tenant's own period and action, never shorter than the legal floor. Tenant administrators only.
 */

public class RetentionPolicy {

  /**
   * When the period starts: after the person leaves, after the record's own date, after inactivity, or after the purpose ends. Output only.
   */
  public enum TriggerEnum {
    AFTER_INACTIVITY("after_inactivity"),
    
    AFTER_LEAVING("after_leaving"),
    
    AFTER_PURPOSE_END("after_purpose_end"),
    
    AFTER_RECORD_DATE("after_record_date");

    private final String value;

    TriggerEnum(String value) {
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
    public static TriggerEnum fromValue(String value) {
      for (TriggerEnum b : TriggerEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }

  /**
   * What the module does with data past the period: delete, anonymise or archive.
   */
  public enum ActionEnum {
    ANONYMISE("anonymise"),
    
    ARCHIVE("archive"),
    
    DELETE("delete");

    private final String value;

    ActionEnum(String value) {
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
    public static ActionEnum fromValue(String value) {
      for (ActionEnum b : ActionEnum.values()) {
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
  private @Nullable String categoryCode;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String module;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable TriggerEnum trigger;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Integer periodDays;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Integer minimumPeriodDays;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable ActionEnum action;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Integer notifyBeforeDays;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String legalReference;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Boolean overridden;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String etag;

  public RetentionPolicy() {
    super();
  }

  public RetentionPolicy id(@Nullable UUID id) {
    this.id = id;
    return this;
  }

  /**
   * The catalogue policy (stable in every environment). Output only.
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

  public RetentionPolicy code(@Nullable String code) {
    this.code = code;
    return this;
  }

  /**
   * Policy code, e.g. attendance.after_leaving. Output only.
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

  public RetentionPolicy categoryCode(@Nullable String categoryCode) {
    this.categoryCode = categoryCode;
    return this;
  }

  /**
   * The data category kept. Output only.
   * @return categoryCode
   */
   
  @JsonProperty("categoryCode")
  public @Nullable String getCategoryCode() {
    return categoryCode;
  }

  @JsonProperty("categoryCode")
  public void setCategoryCode(@Nullable String categoryCode) {
    this.categoryCode = categoryCode;
  }

  public RetentionPolicy module(@Nullable String module) {
    this.module = module;
    return this;
  }

  /**
   * The module holding the data and sweeping it. Output only.
   * @return module
   */
   
  @JsonProperty("module")
  public @Nullable String getModule() {
    return module;
  }

  @JsonProperty("module")
  public void setModule(@Nullable String module) {
    this.module = module;
  }

  public RetentionPolicy trigger(@Nullable TriggerEnum trigger) {
    this.trigger = trigger;
    return this;
  }

  /**
   * When the period starts: after the person leaves, after the record's own date, after inactivity, or after the purpose ends. Output only.
   * @return trigger
   */
   
  @JsonProperty("trigger")
  public @Nullable TriggerEnum getTrigger() {
    return trigger;
  }

  @JsonProperty("trigger")
  public void setTrigger(@Nullable TriggerEnum trigger) {
    this.trigger = trigger;
  }

  public RetentionPolicy periodDays(@Nullable Integer periodDays) {
    this.periodDays = periodDays;
    return this;
  }

  /**
   * How many days the data is kept after the trigger: the tenant's own when set, the catalogue's otherwise.
   * @return periodDays
   */
  @Min(value = 1) 
  @JsonProperty("periodDays")
  public @Nullable Integer getPeriodDays() {
    return periodDays;
  }

  @JsonProperty("periodDays")
  public void setPeriodDays(@Nullable Integer periodDays) {
    this.periodDays = periodDays;
  }

  public RetentionPolicy minimumPeriodDays(@Nullable Integer minimumPeriodDays) {
    this.minimumPeriodDays = minimumPeriodDays;
    return this;
  }

  /**
   * The legal floor: no period may be shorter. Output only.
   * @return minimumPeriodDays
   */
   
  @JsonProperty("minimumPeriodDays")
  public @Nullable Integer getMinimumPeriodDays() {
    return minimumPeriodDays;
  }

  @JsonProperty("minimumPeriodDays")
  public void setMinimumPeriodDays(@Nullable Integer minimumPeriodDays) {
    this.minimumPeriodDays = minimumPeriodDays;
  }

  public RetentionPolicy action(@Nullable ActionEnum action) {
    this.action = action;
    return this;
  }

  /**
   * What the module does with data past the period: delete, anonymise or archive.
   * @return action
   */
   
  @JsonProperty("action")
  public @Nullable ActionEnum getAction() {
    return action;
  }

  @JsonProperty("action")
  public void setAction(@Nullable ActionEnum action) {
    this.action = action;
  }

  public RetentionPolicy notifyBeforeDays(@Nullable Integer notifyBeforeDays) {
    this.notifyBeforeDays = notifyBeforeDays;
    return this;
  }

  /**
   * How many days before the sweep the person is told; absent = not told. Output only.
   * @return notifyBeforeDays
   */
   
  @JsonProperty("notifyBeforeDays")
  public @Nullable Integer getNotifyBeforeDays() {
    return notifyBeforeDays;
  }

  @JsonProperty("notifyBeforeDays")
  public void setNotifyBeforeDays(@Nullable Integer notifyBeforeDays) {
    this.notifyBeforeDays = notifyBeforeDays;
  }

  public RetentionPolicy legalReference(@Nullable String legalReference) {
    this.legalReference = legalReference;
    return this;
  }

  /**
   * The law or rule the floor comes from (to verify with counsel). Output only.
   * @return legalReference
   */
   
  @JsonProperty("legalReference")
  public @Nullable String getLegalReference() {
    return legalReference;
  }

  @JsonProperty("legalReference")
  public void setLegalReference(@Nullable String legalReference) {
    this.legalReference = legalReference;
  }

  public RetentionPolicy overridden(@Nullable Boolean overridden) {
    this.overridden = overridden;
    return this;
  }

  /**
   * The tenant set its own period or action. Output only.
   * @return overridden
   */
   
  @JsonProperty("overridden")
  public @Nullable Boolean getOverridden() {
    return overridden;
  }

  @JsonProperty("overridden")
  public void setOverridden(@Nullable Boolean overridden) {
    this.overridden = overridden;
  }

  public RetentionPolicy etag(@Nullable String etag) {
    this.etag = etag;
    return this;
  }

  /**
   * Entity tag (the tenant setting's row version, 0 while never set). Output only; send back as If-Match on update.
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
    RetentionPolicy other = (RetentionPolicy) o;
    return Objects.equals(this.id, other.id) &&
        Objects.equals(this.code, other.code) &&
        Objects.equals(this.categoryCode, other.categoryCode) &&
        Objects.equals(this.module, other.module) &&
        Objects.equals(this.trigger, other.trigger) &&
        Objects.equals(this.periodDays, other.periodDays) &&
        Objects.equals(this.minimumPeriodDays, other.minimumPeriodDays) &&
        Objects.equals(this.action, other.action) &&
        Objects.equals(this.notifyBeforeDays, other.notifyBeforeDays) &&
        Objects.equals(this.legalReference, other.legalReference) &&
        Objects.equals(this.overridden, other.overridden) &&
        Objects.equals(this.etag, other.etag);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, code, categoryCode, module, trigger, periodDays, minimumPeriodDays, action, notifyBeforeDays, legalReference, overridden, etag);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class RetentionPolicy {\n");
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    code: ").append(toIndentedString(code)).append("\n");
    sb.append("    categoryCode: ").append(toIndentedString(categoryCode)).append("\n");
    sb.append("    module: ").append(toIndentedString(module)).append("\n");
    sb.append("    trigger: ").append(toIndentedString(trigger)).append("\n");
    sb.append("    periodDays: ").append(toIndentedString(periodDays)).append("\n");
    sb.append("    minimumPeriodDays: ").append(toIndentedString(minimumPeriodDays)).append("\n");
    sb.append("    action: ").append(toIndentedString(action)).append("\n");
    sb.append("    notifyBeforeDays: ").append(toIndentedString(notifyBeforeDays)).append("\n");
    sb.append("    legalReference: ").append(toIndentedString(legalReference)).append("\n");
    sb.append("    overridden: ").append(toIndentedString(overridden)).append("\n");
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
