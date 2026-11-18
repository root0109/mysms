// Owned API contract of facility.
package com.mysms.facility.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.mysms.facility.api.model.ProblemErrorsInner;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.springframework.lang.Nullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;

/**
 * RFC 9457 problem details.
 */

public class Problem {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private URI type;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private String title;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private Integer status;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String detail;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String instance;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String code;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String traceId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private List<@Valid ProblemErrorsInner> errors = new ArrayList<>();

  public Problem() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public Problem(URI type, String title, Integer status) {
    this.type = type;
    this.title = title;
    this.status = status;
  }

  public Problem type(URI type) {
    this.type = type;
    return this;
  }

  /**
   * Get type
   * @return type
   */
  @NotNull @Valid 
  @JsonProperty("type")
  public URI getType() {
    return type;
  }

  @JsonProperty("type")
  public void setType(URI type) {
    this.type = type;
  }

  public Problem title(String title) {
    this.title = title;
    return this;
  }

  /**
   * Get title
   * @return title
   */
  @NotNull 
  @JsonProperty("title")
  public String getTitle() {
    return title;
  }

  @JsonProperty("title")
  public void setTitle(String title) {
    this.title = title;
  }

  public Problem status(Integer status) {
    this.status = status;
    return this;
  }

  /**
   * Get status
   * @return status
   */
  @NotNull 
  @JsonProperty("status")
  public Integer getStatus() {
    return status;
  }

  @JsonProperty("status")
  public void setStatus(Integer status) {
    this.status = status;
  }

  public Problem detail(@Nullable String detail) {
    this.detail = detail;
    return this;
  }

  /**
   * Get detail
   * @return detail
   */
  
  @JsonProperty("detail")
  public @Nullable String getDetail() {
    return detail;
  }

  @JsonProperty("detail")
  public void setDetail(@Nullable String detail) {
    this.detail = detail;
  }

  public Problem instance(@Nullable String instance) {
    this.instance = instance;
    return this;
  }

  /**
   * Get instance
   * @return instance
   */
  
  @JsonProperty("instance")
  public @Nullable String getInstance() {
    return instance;
  }

  @JsonProperty("instance")
  public void setInstance(@Nullable String instance) {
    this.instance = instance;
  }

  public Problem code(@Nullable String code) {
    this.code = code;
    return this;
  }

  /**
   * Stable machine-readable error code, e.g. INVALID_STATE_TRANSITION.
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

  public Problem traceId(@Nullable String traceId) {
    this.traceId = traceId;
    return this;
  }

  /**
   * Get traceId
   * @return traceId
   */
  
  @JsonProperty("traceId")
  public @Nullable String getTraceId() {
    return traceId;
  }

  @JsonProperty("traceId")
  public void setTraceId(@Nullable String traceId) {
    this.traceId = traceId;
  }

  public Problem errors(List<@Valid ProblemErrorsInner> errors) {
    this.errors = errors;
    return this;
  }

  public Problem addErrorsItem(ProblemErrorsInner errorsItem) {
    if (this.errors == null) {
      this.errors = new ArrayList<>();
    }
    this.errors.add(errorsItem);
    return this;
  }

  /**
   * Get errors
   * @return errors
   */
  
  @JsonProperty("errors")
  public List<@Valid ProblemErrorsInner> getErrors() {
    return errors;
  }

  @JsonProperty("errors")
  public void setErrors(List<@Valid ProblemErrorsInner> errors) {
    this.errors = errors;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Problem problem = (Problem) o;
    return Objects.equals(this.type, problem.type) &&
        Objects.equals(this.title, problem.title) &&
        Objects.equals(this.status, problem.status) &&
        Objects.equals(this.detail, problem.detail) &&
        Objects.equals(this.instance, problem.instance) &&
        Objects.equals(this.code, problem.code) &&
        Objects.equals(this.traceId, problem.traceId) &&
        Objects.equals(this.errors, problem.errors);
  }

  @Override
  public int hashCode() {
    return Objects.hash(type, title, status, detail, instance, code, traceId, errors);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class Problem {\n");
    sb.append("    type: ").append(toIndentedString(type)).append("\n");
    sb.append("    title: ").append(toIndentedString(title)).append("\n");
    sb.append("    status: ").append(toIndentedString(status)).append("\n");
    sb.append("    detail: ").append(toIndentedString(detail)).append("\n");
    sb.append("    instance: ").append(toIndentedString(instance)).append("\n");
    sb.append("    code: ").append(toIndentedString(code)).append("\n");
    sb.append("    traceId: ").append(toIndentedString(traceId)).append("\n");
    sb.append("    errors: ").append(toIndentedString(errors)).append("\n");
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

