// Owned API contract of comms (JAVA-CODING-RULES §7); first materialized from api/openapi/comms.yaml on 2026-11-10.
package com.mysms.comms.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import java.util.UUID;
import org.springframework.lang.Nullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;

/**
 * Fields of NotificationRule that can be updated (partial update; see updateMask).
 */

public class NotificationRuleUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID roleId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID permissionId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Boolean isEnabled;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String emailSubjectTemplate;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String emailBodyTemplate;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String smsTemplate;

  public NotificationRuleUpdate roleId(@Nullable UUID roleId) {
    this.roleId = roleId;
    return this;
  }

  /**
   * Recipient role.
   * @return roleId
   */
  @Valid 
  @JsonProperty("roleId")
  public @Nullable UUID getRoleId() {
    return roleId;
  }

  @JsonProperty("roleId")
  public void setRoleId(@Nullable UUID roleId) {
    this.roleId = roleId;
  }

  public NotificationRuleUpdate permissionId(@Nullable UUID permissionId) {
    this.permissionId = permissionId;
    return this;
  }

  /**
   * Operation (permission) that triggers it.
   * @return permissionId
   */
  @Valid 
  @JsonProperty("permissionId")
  public @Nullable UUID getPermissionId() {
    return permissionId;
  }

  @JsonProperty("permissionId")
  public void setPermissionId(@Nullable UUID permissionId) {
    this.permissionId = permissionId;
  }

  public NotificationRuleUpdate isEnabled(@Nullable Boolean isEnabled) {
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

  public NotificationRuleUpdate emailSubjectTemplate(@Nullable String emailSubjectTemplate) {
    this.emailSubjectTemplate = emailSubjectTemplate;
    return this;
  }

  /**
   * E-mail subject template.
   * @return emailSubjectTemplate
   */
  
  @JsonProperty("emailSubjectTemplate")
  public @Nullable String getEmailSubjectTemplate() {
    return emailSubjectTemplate;
  }

  @JsonProperty("emailSubjectTemplate")
  public void setEmailSubjectTemplate(@Nullable String emailSubjectTemplate) {
    this.emailSubjectTemplate = emailSubjectTemplate;
  }

  public NotificationRuleUpdate emailBodyTemplate(@Nullable String emailBodyTemplate) {
    this.emailBodyTemplate = emailBodyTemplate;
    return this;
  }

  /**
   * E-mail body template.
   * @return emailBodyTemplate
   */
  
  @JsonProperty("emailBodyTemplate")
  public @Nullable String getEmailBodyTemplate() {
    return emailBodyTemplate;
  }

  @JsonProperty("emailBodyTemplate")
  public void setEmailBodyTemplate(@Nullable String emailBodyTemplate) {
    this.emailBodyTemplate = emailBodyTemplate;
  }

  public NotificationRuleUpdate smsTemplate(@Nullable String smsTemplate) {
    this.smsTemplate = smsTemplate;
    return this;
  }

  /**
   * SMS template.
   * @return smsTemplate
   */
  
  @JsonProperty("smsTemplate")
  public @Nullable String getSmsTemplate() {
    return smsTemplate;
  }

  @JsonProperty("smsTemplate")
  public void setSmsTemplate(@Nullable String smsTemplate) {
    this.smsTemplate = smsTemplate;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    NotificationRuleUpdate notificationRuleUpdate = (NotificationRuleUpdate) o;
    return Objects.equals(this.roleId, notificationRuleUpdate.roleId) &&
        Objects.equals(this.permissionId, notificationRuleUpdate.permissionId) &&
        Objects.equals(this.isEnabled, notificationRuleUpdate.isEnabled) &&
        Objects.equals(this.emailSubjectTemplate, notificationRuleUpdate.emailSubjectTemplate) &&
        Objects.equals(this.emailBodyTemplate, notificationRuleUpdate.emailBodyTemplate) &&
        Objects.equals(this.smsTemplate, notificationRuleUpdate.smsTemplate);
  }

  @Override
  public int hashCode() {
    return Objects.hash(roleId, permissionId, isEnabled, emailSubjectTemplate, emailBodyTemplate, smsTemplate);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class NotificationRuleUpdate {\n");
    sb.append("    roleId: ").append(toIndentedString(roleId)).append("\n");
    sb.append("    permissionId: ").append(toIndentedString(permissionId)).append("\n");
    sb.append("    isEnabled: ").append(toIndentedString(isEnabled)).append("\n");
    sb.append("    emailSubjectTemplate: ").append(toIndentedString(emailSubjectTemplate)).append("\n");
    sb.append("    emailBodyTemplate: ").append(toIndentedString(emailBodyTemplate)).append("\n");
    sb.append("    smsTemplate: ").append(toIndentedString(smsTemplate)).append("\n");
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

