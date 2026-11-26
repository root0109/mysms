// Owned API contract of comms.
package com.mysms.comms.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import org.springframework.lang.Nullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;

/**
 * Fields of NotificationType that can be updated (PATCH: a property left out keeps its value, null clears it, a value sets it).
 */

public class NotificationTypeUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull @Size(max = 80) String> code;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull String> description;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull @Size(max = 60) String> moduleCode;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@Size(max = 80) String> roleCode;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull Boolean> isEnabled;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull Boolean> popupEnabled;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull Boolean> teacherPopupEnabled;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@Size(max = 500) String> validParameters;

  public NotificationTypeUpdate code(@Nullable Optional<String> code) {
    this.code = code;
    return this;
  }

  /**
   * Notification code.
   * @return code
   */
  
  @JsonProperty("code")
  public @Nullable Optional<String> getCode() {
    return code;
  }

  @JsonProperty("code")
  public void setCode(@Nullable Optional<String> code) {
    this.code = code;
  }

  public NotificationTypeUpdate description(@Nullable Optional<String> description) {
    this.description = description;
    return this;
  }

  /**
   * Default message text with placeholders.
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

  public NotificationTypeUpdate moduleCode(@Nullable Optional<String> moduleCode) {
    this.moduleCode = moduleCode;
    return this;
  }

  /**
   * Module raising it (see iam.app_module).
   * @return moduleCode
   */
  
  @JsonProperty("moduleCode")
  public @Nullable Optional<String> getModuleCode() {
    return moduleCode;
  }

  @JsonProperty("moduleCode")
  public void setModuleCode(@Nullable Optional<String> moduleCode) {
    this.moduleCode = moduleCode;
  }

  public NotificationTypeUpdate roleCode(@Nullable Optional<String> roleCode) {
    this.roleCode = roleCode;
    return this;
  }

  /**
   * Platform role code it is addressed to by default (resolved to each tenant's role with that code).
   * @return roleCode
   */
  
  @JsonProperty("roleCode")
  public @Nullable Optional<String> getRoleCode() {
    return roleCode;
  }

  @JsonProperty("roleCode")
  public void setRoleCode(@Nullable Optional<String> roleCode) {
    this.roleCode = roleCode;
  }

  public NotificationTypeUpdate isEnabled(@Nullable Optional<Boolean> isEnabled) {
    this.isEnabled = isEnabled;
    return this;
  }

  /**
   * Enabled by default.
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

  public NotificationTypeUpdate popupEnabled(@Nullable Optional<Boolean> popupEnabled) {
    this.popupEnabled = popupEnabled;
    return this;
  }

  /**
   * Shown as an in-app popup to students.
   * @return popupEnabled
   */
  
  @JsonProperty("popupEnabled")
  public @Nullable Optional<Boolean> getPopupEnabled() {
    return popupEnabled;
  }

  @JsonProperty("popupEnabled")
  public void setPopupEnabled(@Nullable Optional<Boolean> popupEnabled) {
    this.popupEnabled = popupEnabled;
  }

  public NotificationTypeUpdate teacherPopupEnabled(@Nullable Optional<Boolean> teacherPopupEnabled) {
    this.teacherPopupEnabled = teacherPopupEnabled;
    return this;
  }

  /**
   * Shown as an in-app popup to teachers.
   * @return teacherPopupEnabled
   */
  
  @JsonProperty("teacherPopupEnabled")
  public @Nullable Optional<Boolean> getTeacherPopupEnabled() {
    return teacherPopupEnabled;
  }

  @JsonProperty("teacherPopupEnabled")
  public void setTeacherPopupEnabled(@Nullable Optional<Boolean> teacherPopupEnabled) {
    this.teacherPopupEnabled = teacherPopupEnabled;
  }

  public NotificationTypeUpdate validParameters(@Nullable Optional<String> validParameters) {
    this.validParameters = validParameters;
    return this;
  }

  /**
   * Placeholders the message may use.
   * @return validParameters
   */
  
  @JsonProperty("validParameters")
  public @Nullable Optional<String> getValidParameters() {
    return validParameters;
  }

  @JsonProperty("validParameters")
  public void setValidParameters(@Nullable Optional<String> validParameters) {
    this.validParameters = validParameters;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    NotificationTypeUpdate notificationTypeUpdate = (NotificationTypeUpdate) o;
    return Objects.equals(this.code, notificationTypeUpdate.code) &&
        Objects.equals(this.description, notificationTypeUpdate.description) &&
        Objects.equals(this.moduleCode, notificationTypeUpdate.moduleCode) &&
        Objects.equals(this.roleCode, notificationTypeUpdate.roleCode) &&
        Objects.equals(this.isEnabled, notificationTypeUpdate.isEnabled) &&
        Objects.equals(this.popupEnabled, notificationTypeUpdate.popupEnabled) &&
        Objects.equals(this.teacherPopupEnabled, notificationTypeUpdate.teacherPopupEnabled) &&
        Objects.equals(this.validParameters, notificationTypeUpdate.validParameters);
  }

  @Override
  public int hashCode() {
    return Objects.hash(code, description, moduleCode, roleCode, isEnabled, popupEnabled, teacherPopupEnabled, validParameters);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class NotificationTypeUpdate {\n");
    sb.append("    code: ").append(toIndentedString(code)).append("\n");
    sb.append("    description: ").append(toIndentedString(description)).append("\n");
    sb.append("    moduleCode: ").append(toIndentedString(moduleCode)).append("\n");
    sb.append("    roleCode: ").append(toIndentedString(roleCode)).append("\n");
    sb.append("    isEnabled: ").append(toIndentedString(isEnabled)).append("\n");
    sb.append("    popupEnabled: ").append(toIndentedString(popupEnabled)).append("\n");
    sb.append("    teacherPopupEnabled: ").append(toIndentedString(teacherPopupEnabled)).append("\n");
    sb.append("    validParameters: ").append(toIndentedString(validParameters)).append("\n");
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

