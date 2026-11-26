// Owned API contract of payroll.
package com.mysms.hrms.payroll.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import org.springframework.lang.Nullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;

/**
 * ProblemErrorsInner
 */

@JsonTypeName("Problem_errors_inner")
public class ProblemErrorsInner {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private String field;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private String message;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String code;

  public ProblemErrorsInner() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public ProblemErrorsInner(String field, String message) {
    this.field = field;
    this.message = message;
  }

  public ProblemErrorsInner field(String field) {
    this.field = field;
    return this;
  }

  /**
   * Get field
   * @return field
   */
  @NotNull 
  @JsonProperty("field")
  public String getField() {
    return field;
  }

  @JsonProperty("field")
  public void setField(String field) {
    this.field = field;
  }

  public ProblemErrorsInner message(String message) {
    this.message = message;
    return this;
  }

  /**
   * Get message
   * @return message
   */
  @NotNull 
  @JsonProperty("message")
  public String getMessage() {
    return message;
  }

  @JsonProperty("message")
  public void setMessage(String message) {
    this.message = message;
  }

  public ProblemErrorsInner code(@Nullable String code) {
    this.code = code;
    return this;
  }

  /**
   * Get code
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

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ProblemErrorsInner problemErrorsInner = (ProblemErrorsInner) o;
    return Objects.equals(this.field, problemErrorsInner.field) &&
        Objects.equals(this.message, problemErrorsInner.message) &&
        Objects.equals(this.code, problemErrorsInner.code);
  }

  @Override
  public int hashCode() {
    return Objects.hash(field, message, code);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ProblemErrorsInner {\n");
    sb.append("    field: ").append(toIndentedString(field)).append("\n");
    sb.append("    message: ").append(toIndentedString(message)).append("\n");
    sb.append("    code: ").append(toIndentedString(code)).append("\n");
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

