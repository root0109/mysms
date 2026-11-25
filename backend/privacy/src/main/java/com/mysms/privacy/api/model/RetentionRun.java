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
 * One daily run of a retention policy: the cutoff privacy sent to the module that holds the data and, once the module reports, what it removed and kept. Counts only.
 */

public class RetentionRun {

  /**
   * What the module does with data older than the cutoff. Output only.
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

  /**
   * dispatched (waiting for the module), done, partial (rows kept under holds) or failed. Output only.
   */
  public enum StatusEnum {
    DISPATCHED("dispatched"),
    
    DONE("done"),
    
    FAILED("failed"),
    
    PARTIAL("partial");

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
  private @Nullable UUID id;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String policyCode;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String categoryCode;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String module;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable ActionEnum action;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private @Nullable OffsetDateTime cutoff;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable StatusEnum status;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Integer affected;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Integer held;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private @Nullable OffsetDateTime startedAt;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private @Nullable OffsetDateTime completedAt;

  public RetentionRun() {
    super();
  }

  public RetentionRun id(@Nullable UUID id) {
    this.id = id;
    return this;
  }

  /**
   * Surrogate primary key. Output only.
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

  public RetentionRun policyCode(@Nullable String policyCode) {
    this.policyCode = policyCode;
    return this;
  }

  /**
   * The policy run. Output only.
   * @return policyCode
   */
   
  @JsonProperty("policyCode")
  public @Nullable String getPolicyCode() {
    return policyCode;
  }

  @JsonProperty("policyCode")
  public void setPolicyCode(@Nullable String policyCode) {
    this.policyCode = policyCode;
  }

  public RetentionRun categoryCode(@Nullable String categoryCode) {
    this.categoryCode = categoryCode;
    return this;
  }

  /**
   * The data category swept. Output only.
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

  public RetentionRun module(@Nullable String module) {
    this.module = module;
    return this;
  }

  /**
   * The module asked to sweep. Output only.
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

  public RetentionRun action(@Nullable ActionEnum action) {
    this.action = action;
    return this;
  }

  /**
   * What the module does with data older than the cutoff. Output only.
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

  public RetentionRun cutoff(@Nullable OffsetDateTime cutoff) {
    this.cutoff = cutoff;
    return this;
  }

  /**
   * Data whose trigger lies before this instant is past its period. Output only.
   * @return cutoff
   */
  @Valid 
  @JsonProperty("cutoff")
  public @Nullable OffsetDateTime getCutoff() {
    return cutoff;
  }

  @JsonProperty("cutoff")
  public void setCutoff(@Nullable OffsetDateTime cutoff) {
    this.cutoff = cutoff;
  }

  public RetentionRun status(@Nullable StatusEnum status) {
    this.status = status;
    return this;
  }

  /**
   * dispatched (waiting for the module), done, partial (rows kept under holds) or failed. Output only.
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

  public RetentionRun affected(@Nullable Integer affected) {
    this.affected = affected;
    return this;
  }

  /**
   * How many rows the module removed; absent until it reports. Output only.
   * @return affected
   */
   
  @JsonProperty("affected")
  public @Nullable Integer getAffected() {
    return affected;
  }

  @JsonProperty("affected")
  public void setAffected(@Nullable Integer affected) {
    this.affected = affected;
  }

  public RetentionRun held(@Nullable Integer held) {
    this.held = held;
    return this;
  }

  /**
   * How many rows the module kept under a legal hold; absent until it reports. Output only.
   * @return held
   */
   
  @JsonProperty("held")
  public @Nullable Integer getHeld() {
    return held;
  }

  @JsonProperty("held")
  public void setHeld(@Nullable Integer held) {
    this.held = held;
  }

  public RetentionRun startedAt(@Nullable OffsetDateTime startedAt) {
    this.startedAt = startedAt;
    return this;
  }

  /**
   * When the run was dispatched. Output only.
   * @return startedAt
   */
  @Valid 
  @JsonProperty("startedAt")
  public @Nullable OffsetDateTime getStartedAt() {
    return startedAt;
  }

  @JsonProperty("startedAt")
  public void setStartedAt(@Nullable OffsetDateTime startedAt) {
    this.startedAt = startedAt;
  }

  public RetentionRun completedAt(@Nullable OffsetDateTime completedAt) {
    this.completedAt = completedAt;
    return this;
  }

  /**
   * When the module reported; absent until then. Output only.
   * @return completedAt
   */
  @Valid 
  @JsonProperty("completedAt")
  public @Nullable OffsetDateTime getCompletedAt() {
    return completedAt;
  }

  @JsonProperty("completedAt")
  public void setCompletedAt(@Nullable OffsetDateTime completedAt) {
    this.completedAt = completedAt;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    RetentionRun other = (RetentionRun) o;
    return Objects.equals(this.id, other.id) &&
        Objects.equals(this.policyCode, other.policyCode) &&
        Objects.equals(this.categoryCode, other.categoryCode) &&
        Objects.equals(this.module, other.module) &&
        Objects.equals(this.action, other.action) &&
        Objects.equals(this.cutoff, other.cutoff) &&
        Objects.equals(this.status, other.status) &&
        Objects.equals(this.affected, other.affected) &&
        Objects.equals(this.held, other.held) &&
        Objects.equals(this.startedAt, other.startedAt) &&
        Objects.equals(this.completedAt, other.completedAt);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, policyCode, categoryCode, module, action, cutoff, status, affected, held, startedAt, completedAt);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class RetentionRun {\n");
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    policyCode: ").append(toIndentedString(policyCode)).append("\n");
    sb.append("    categoryCode: ").append(toIndentedString(categoryCode)).append("\n");
    sb.append("    module: ").append(toIndentedString(module)).append("\n");
    sb.append("    action: ").append(toIndentedString(action)).append("\n");
    sb.append("    cutoff: ").append(toIndentedString(cutoff)).append("\n");
    sb.append("    status: ").append(toIndentedString(status)).append("\n");
    sb.append("    affected: ").append(toIndentedString(affected)).append("\n");
    sb.append("    held: ").append(toIndentedString(held)).append("\n");
    sb.append("    startedAt: ").append(toIndentedString(startedAt)).append("\n");
    sb.append("    completedAt: ").append(toIndentedString(completedAt)).append("\n");
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
