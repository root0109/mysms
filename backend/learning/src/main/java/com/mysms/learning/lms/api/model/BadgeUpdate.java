// Owned API contract of lms.
package com.mysms.learning.lms.api.model;

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
 * Fields of Badge that can be updated (PATCH: a property left out keeps its value, null clears it, a value sets it).
 */

public class BadgeUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull @Size(max = 120) String> title;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull String> description;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull UUID> imageFileId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull @Size(max = 80) String> targetEntity;

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
  private @Nullable Optional<@NotNull TriggerEventEnum> triggerEvent;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull String> conditionExpr;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@Size(max = 80) String> fieldToCheck;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull @Size(max = 80) String> userField;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull Boolean> grantOnlyOnce;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull Boolean> isEnabled;

  public BadgeUpdate title(@Nullable Optional<String> title) {
    this.title = title;
    return this;
  }

  /**
   * Badge name.
   * @return title
   */
  
  @JsonProperty("title")
  public @Nullable Optional<String> getTitle() {
    return title;
  }

  @JsonProperty("title")
  public void setTitle(@Nullable Optional<String> title) {
    this.title = title;
  }

  public BadgeUpdate description(@Nullable Optional<String> description) {
    this.description = description;
    return this;
  }

  /**
   * Description.
   * @return description
   */
  
  @JsonProperty("description")
  public @Nullable Optional<String> getDescription() {
    return description;
  }

  @JsonProperty("description")
  public void setDescription(@Nullable Optional<String> description) {
    this.description = description;
  }

  public BadgeUpdate imageFileId(@Nullable Optional<UUID> imageFileId) {
    this.imageFileId = imageFileId;
    return this;
  }

  /**
   * Badge image.
   * @return imageFileId
   */
  
  @JsonProperty("imageFileId")
  public @Nullable Optional<UUID> getImageFileId() {
    return imageFileId;
  }

  @JsonProperty("imageFileId")
  public void setImageFileId(@Nullable Optional<UUID> imageFileId) {
    this.imageFileId = imageFileId;
  }

  public BadgeUpdate targetEntity(@Nullable Optional<String> targetEntity) {
    this.targetEntity = targetEntity;
    return this;
  }

  /**
   * Table whose changes are evaluated (e.g. lms.enrollment).
   * @return targetEntity
   */
  
  @JsonProperty("targetEntity")
  public @Nullable Optional<String> getTargetEntity() {
    return targetEntity;
  }

  @JsonProperty("targetEntity")
  public void setTargetEntity(@Nullable Optional<String> targetEntity) {
    this.targetEntity = targetEntity;
  }

  public BadgeUpdate triggerEvent(@Nullable Optional<TriggerEventEnum> triggerEvent) {
    this.triggerEvent = triggerEvent;
    return this;
  }

  /**
   * When the rule is evaluated.
   * @return triggerEvent
   */
  
  @JsonProperty("triggerEvent")
  public @Nullable Optional<TriggerEventEnum> getTriggerEvent() {
    return triggerEvent;
  }

  @JsonProperty("triggerEvent")
  public void setTriggerEvent(@Nullable Optional<TriggerEventEnum> triggerEvent) {
    this.triggerEvent = triggerEvent;
  }

  public BadgeUpdate conditionExpr(@Nullable Optional<String> conditionExpr) {
    this.conditionExpr = conditionExpr;
    return this;
  }

  /**
   * Rule expression that must be true.
   * @return conditionExpr
   */
  
  @JsonProperty("conditionExpr")
  public @Nullable Optional<String> getConditionExpr() {
    return conditionExpr;
  }

  @JsonProperty("conditionExpr")
  public void setConditionExpr(@Nullable Optional<String> conditionExpr) {
    this.conditionExpr = conditionExpr;
  }

  public BadgeUpdate fieldToCheck(@Nullable Optional<String> fieldToCheck) {
    this.fieldToCheck = fieldToCheck;
    return this;
  }

  /**
   * Field watched for value_change.
   * @return fieldToCheck
   */
  
  @JsonProperty("fieldToCheck")
  public @Nullable Optional<String> getFieldToCheck() {
    return fieldToCheck;
  }

  @JsonProperty("fieldToCheck")
  public void setFieldToCheck(@Nullable Optional<String> fieldToCheck) {
    this.fieldToCheck = fieldToCheck;
  }

  public BadgeUpdate userField(@Nullable Optional<String> userField) {
    this.userField = userField;
    return this;
  }

  /**
   * Field of the target that holds the user to award.
   * @return userField
   */
  
  @JsonProperty("userField")
  public @Nullable Optional<String> getUserField() {
    return userField;
  }

  @JsonProperty("userField")
  public void setUserField(@Nullable Optional<String> userField) {
    this.userField = userField;
  }

  public BadgeUpdate grantOnlyOnce(@Nullable Optional<Boolean> grantOnlyOnce) {
    this.grantOnlyOnce = grantOnlyOnce;
    return this;
  }

  /**
   * Award at most once per user.
   * @return grantOnlyOnce
   */
  
  @JsonProperty("grantOnlyOnce")
  public @Nullable Optional<Boolean> getGrantOnlyOnce() {
    return grantOnlyOnce;
  }

  @JsonProperty("grantOnlyOnce")
  public void setGrantOnlyOnce(@Nullable Optional<Boolean> grantOnlyOnce) {
    this.grantOnlyOnce = grantOnlyOnce;
  }

  public BadgeUpdate isEnabled(@Nullable Optional<Boolean> isEnabled) {
    this.isEnabled = isEnabled;
    return this;
  }

  /**
   * Rule active.
   * @return isEnabled
   */
  
  @JsonProperty("isEnabled")
  public @Nullable Optional<Boolean> getIsEnabled() {
    return isEnabled;
  }

  @JsonProperty("isEnabled")
  public void setIsEnabled(@Nullable Optional<Boolean> isEnabled) {
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

