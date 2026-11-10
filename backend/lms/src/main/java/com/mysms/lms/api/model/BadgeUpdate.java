// Owned API contract of lms (JAVA-CODING-RULES §7); first materialized from api/openapi/lms.yaml on 2026-11-10.
package com.mysms.lms.api.model;

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
 * Fields of Badge that can be updated (partial update; see updateMask).
 */

public class BadgeUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String title;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String description;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID imageFileId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String targetEntity;

  /**
   * When the rule is evaluated.
   */
  public enum TriggerEventEnum {
    NEW("new"),
    
    VALUE_CHANGE("value_change"),
    
    MANUAL("manual");

    private final String value;

    TriggerEventEnum(String value) {
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
    public static TriggerEventEnum fromValue(String value) {
      for (TriggerEventEnum b : TriggerEventEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable TriggerEventEnum triggerEvent;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String conditionExpr;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String fieldToCheck;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String userField;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Boolean grantOnlyOnce;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Boolean isEnabled;

  public BadgeUpdate title(@Nullable String title) {
    this.title = title;
    return this;
  }

  /**
   * Badge name.
   * @return title
   */
  @Size(max = 120) 
  @JsonProperty("title")
  public @Nullable String getTitle() {
    return title;
  }

  @JsonProperty("title")
  public void setTitle(@Nullable String title) {
    this.title = title;
  }

  public BadgeUpdate description(@Nullable String description) {
    this.description = description;
    return this;
  }

  /**
   * Description.
   * @return description
   */
  
  @JsonProperty("description")
  public @Nullable String getDescription() {
    return description;
  }

  @JsonProperty("description")
  public void setDescription(@Nullable String description) {
    this.description = description;
  }

  public BadgeUpdate imageFileId(@Nullable UUID imageFileId) {
    this.imageFileId = imageFileId;
    return this;
  }

  /**
   * Badge image.
   * @return imageFileId
   */
  @Valid 
  @JsonProperty("imageFileId")
  public @Nullable UUID getImageFileId() {
    return imageFileId;
  }

  @JsonProperty("imageFileId")
  public void setImageFileId(@Nullable UUID imageFileId) {
    this.imageFileId = imageFileId;
  }

  public BadgeUpdate targetEntity(@Nullable String targetEntity) {
    this.targetEntity = targetEntity;
    return this;
  }

  /**
   * Table whose changes are evaluated (e.g. lms.enrollment).
   * @return targetEntity
   */
  @Size(max = 80) 
  @JsonProperty("targetEntity")
  public @Nullable String getTargetEntity() {
    return targetEntity;
  }

  @JsonProperty("targetEntity")
  public void setTargetEntity(@Nullable String targetEntity) {
    this.targetEntity = targetEntity;
  }

  public BadgeUpdate triggerEvent(@Nullable TriggerEventEnum triggerEvent) {
    this.triggerEvent = triggerEvent;
    return this;
  }

  /**
   * When the rule is evaluated.
   * @return triggerEvent
   */
  
  @JsonProperty("triggerEvent")
  public @Nullable TriggerEventEnum getTriggerEvent() {
    return triggerEvent;
  }

  @JsonProperty("triggerEvent")
  public void setTriggerEvent(@Nullable TriggerEventEnum triggerEvent) {
    this.triggerEvent = triggerEvent;
  }

  public BadgeUpdate conditionExpr(@Nullable String conditionExpr) {
    this.conditionExpr = conditionExpr;
    return this;
  }

  /**
   * Rule expression that must be true.
   * @return conditionExpr
   */
  
  @JsonProperty("conditionExpr")
  public @Nullable String getConditionExpr() {
    return conditionExpr;
  }

  @JsonProperty("conditionExpr")
  public void setConditionExpr(@Nullable String conditionExpr) {
    this.conditionExpr = conditionExpr;
  }

  public BadgeUpdate fieldToCheck(@Nullable String fieldToCheck) {
    this.fieldToCheck = fieldToCheck;
    return this;
  }

  /**
   * Field watched for value_change.
   * @return fieldToCheck
   */
  @Size(max = 80) 
  @JsonProperty("fieldToCheck")
  public @Nullable String getFieldToCheck() {
    return fieldToCheck;
  }

  @JsonProperty("fieldToCheck")
  public void setFieldToCheck(@Nullable String fieldToCheck) {
    this.fieldToCheck = fieldToCheck;
  }

  public BadgeUpdate userField(@Nullable String userField) {
    this.userField = userField;
    return this;
  }

  /**
   * Field of the target that holds the user to award.
   * @return userField
   */
  @Size(max = 80) 
  @JsonProperty("userField")
  public @Nullable String getUserField() {
    return userField;
  }

  @JsonProperty("userField")
  public void setUserField(@Nullable String userField) {
    this.userField = userField;
  }

  public BadgeUpdate grantOnlyOnce(@Nullable Boolean grantOnlyOnce) {
    this.grantOnlyOnce = grantOnlyOnce;
    return this;
  }

  /**
   * Award at most once per user.
   * @return grantOnlyOnce
   */
  
  @JsonProperty("grantOnlyOnce")
  public @Nullable Boolean getGrantOnlyOnce() {
    return grantOnlyOnce;
  }

  @JsonProperty("grantOnlyOnce")
  public void setGrantOnlyOnce(@Nullable Boolean grantOnlyOnce) {
    this.grantOnlyOnce = grantOnlyOnce;
  }

  public BadgeUpdate isEnabled(@Nullable Boolean isEnabled) {
    this.isEnabled = isEnabled;
    return this;
  }

  /**
   * Rule active.
   * @return isEnabled
   */
  
  @JsonProperty("isEnabled")
  public @Nullable Boolean getIsEnabled() {
    return isEnabled;
  }

  @JsonProperty("isEnabled")
  public void setIsEnabled(@Nullable Boolean isEnabled) {
    this.isEnabled = isEnabled;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    BadgeUpdate badgeUpdate = (BadgeUpdate) o;
    return Objects.equals(this.title, badgeUpdate.title) &&
        Objects.equals(this.description, badgeUpdate.description) &&
        Objects.equals(this.imageFileId, badgeUpdate.imageFileId) &&
        Objects.equals(this.targetEntity, badgeUpdate.targetEntity) &&
        Objects.equals(this.triggerEvent, badgeUpdate.triggerEvent) &&
        Objects.equals(this.conditionExpr, badgeUpdate.conditionExpr) &&
        Objects.equals(this.fieldToCheck, badgeUpdate.fieldToCheck) &&
        Objects.equals(this.userField, badgeUpdate.userField) &&
        Objects.equals(this.grantOnlyOnce, badgeUpdate.grantOnlyOnce) &&
        Objects.equals(this.isEnabled, badgeUpdate.isEnabled);
  }

  @Override
  public int hashCode() {
    return Objects.hash(title, description, imageFileId, targetEntity, triggerEvent, conditionExpr, fieldToCheck, userField, grantOnlyOnce, isEnabled);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class BadgeUpdate {\n");
    sb.append("    title: ").append(toIndentedString(title)).append("\n");
    sb.append("    description: ").append(toIndentedString(description)).append("\n");
    sb.append("    imageFileId: ").append(toIndentedString(imageFileId)).append("\n");
    sb.append("    targetEntity: ").append(toIndentedString(targetEntity)).append("\n");
    sb.append("    triggerEvent: ").append(toIndentedString(triggerEvent)).append("\n");
    sb.append("    conditionExpr: ").append(toIndentedString(conditionExpr)).append("\n");
    sb.append("    fieldToCheck: ").append(toIndentedString(fieldToCheck)).append("\n");
    sb.append("    userField: ").append(toIndentedString(userField)).append("\n");
    sb.append("    grantOnlyOnce: ").append(toIndentedString(grantOnlyOnce)).append("\n");
    sb.append("    isEnabled: ").append(toIndentedString(isEnabled)).append("\n");
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

