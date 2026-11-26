// Owned API contract of lms.
package com.mysms.learning.lms.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import java.time.OffsetDateTime;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.lang.Nullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;

/**
 * LtiLaunch
 */

public class LtiLaunch {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private String launchUrl;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private String method;

  @JsonInclude(JsonInclude.Include.ALWAYS)
  private Object formFields = null;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private @Nullable OffsetDateTime expiresAt;

  public LtiLaunch() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public LtiLaunch(String launchUrl, String method, Object formFields) {
    this.launchUrl = launchUrl;
    this.method = method;
    this.formFields = formFields;
  }

  public LtiLaunch launchUrl(String launchUrl) {
    this.launchUrl = launchUrl;
    return this;
  }

  /**
   * Tool launch URL.
   * @return launchUrl
   */
  @NotNull 
  @JsonProperty("launchUrl")
  public String getLaunchUrl() {
    return launchUrl;
  }

  @JsonProperty("launchUrl")
  public void setLaunchUrl(String launchUrl) {
    this.launchUrl = launchUrl;
  }

  public LtiLaunch method(String method) {
    this.method = method;
    return this;
  }

  /**
   * HTTP method (POST for LTI 1.1 / 1.3 form launch).
   * @return method
   */
  @NotNull 
  @JsonProperty("method")
  public String getMethod() {
    return method;
  }

  @JsonProperty("method")
  public void setMethod(String method) {
    this.method = method;
  }

  public LtiLaunch formFields(Object formFields) {
    this.formFields = formFields;
    return this;
  }

  /**
   * Signed launch parameters / id_token to auto-submit.
   * @return formFields
   */
  
  @JsonProperty("formFields")
  public Object getFormFields() {
    return formFields;
  }

  @JsonProperty("formFields")
  public void setFormFields(Object formFields) {
    this.formFields = formFields;
  }

  public LtiLaunch expiresAt(@Nullable OffsetDateTime expiresAt) {
    this.expiresAt = expiresAt;
    return this;
  }

  /**
   * Launch expiry.
   * @return expiresAt
   */
  @Valid 
  @JsonProperty("expiresAt")
  public @Nullable OffsetDateTime getExpiresAt() {
    return expiresAt;
  }

  @JsonProperty("expiresAt")
  public void setExpiresAt(@Nullable OffsetDateTime expiresAt) {
    this.expiresAt = expiresAt;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    LtiLaunch ltiLaunch = (LtiLaunch) o;
    return Objects.equals(this.launchUrl, ltiLaunch.launchUrl) &&
        Objects.equals(this.method, ltiLaunch.method) &&
        Objects.equals(this.formFields, ltiLaunch.formFields) &&
        Objects.equals(this.expiresAt, ltiLaunch.expiresAt);
  }

  @Override
  public int hashCode() {
    return Objects.hash(launchUrl, method, formFields, expiresAt);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class LtiLaunch {\n");
    sb.append("    launchUrl: ").append(toIndentedString(launchUrl)).append("\n");
    sb.append("    method: ").append(toIndentedString(method)).append("\n");
    sb.append("    formFields: ").append(toIndentedString(formFields)).append("\n");
    sb.append("    expiresAt: ").append(toIndentedString(expiresAt)).append("\n");
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

