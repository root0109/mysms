// Owned API contract of finance.
package com.mysms.finance.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.lang.Nullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;

/**
 * Operation
 */

public class Operation {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private UUID operationId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private String module;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private String kind;

  /**
   * State.
   */
  public enum StatusEnum {
    QUEUED("queued"),
    
    RUNNING("running"),
    
    SUCCEEDED("succeeded"),
    
    FAILED("failed"),
    
    CANCELLED("cancelled");

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
  private StatusEnum status;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Integer progressPercent;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID resultFileId;

  private @Nullable Object result = null;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String errorMessage;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private OffsetDateTime createTime;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private @Nullable OffsetDateTime doneTime;

  public Operation() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public Operation(UUID operationId, String module, String kind, StatusEnum status, OffsetDateTime createTime) {
    this.operationId = operationId;
    this.module = module;
    this.kind = kind;
    this.status = status;
    this.createTime = createTime;
  }

  public Operation operationId(UUID operationId) {
    this.operationId = operationId;
    return this;
  }

  /**
   * Operation id.
   * @return operationId
   */
  @NotNull @Valid 
  @JsonProperty("operationId")
  public UUID getOperationId() {
    return operationId;
  }

  @JsonProperty("operationId")
  public void setOperationId(UUID operationId) {
    this.operationId = operationId;
  }

  public Operation module(String module) {
    this.module = module;
    return this;
  }

  /**
   * Module that owns the operation; poll and cancel it at /v1/{module}/operations/{operationId}.
   * @return module
   */
  
  @JsonProperty("module")
  public String getModule() {
    return module;
  }

  @JsonProperty("module")
  public void setModule(String module) {
    this.module = module;
  }

  public Operation kind(String kind) {
    this.kind = kind;
    return this;
  }

  /**
   * Operation kind, e.g. report.run, import.students.
   * @return kind
   */
  @NotNull 
  @JsonProperty("kind")
  public String getKind() {
    return kind;
  }

  @JsonProperty("kind")
  public void setKind(String kind) {
    this.kind = kind;
  }

  public Operation status(StatusEnum status) {
    this.status = status;
    return this;
  }

  /**
   * State.
   * @return status
   */
  @NotNull 
  @JsonProperty("status")
  public StatusEnum getStatus() {
    return status;
  }

  @JsonProperty("status")
  public void setStatus(StatusEnum status) {
    this.status = status;
  }

  public Operation progressPercent(@Nullable Integer progressPercent) {
    this.progressPercent = progressPercent;
    return this;
  }

  /**
   * Progress 0-100.
   * @return progressPercent
   */
  
  @JsonProperty("progressPercent")
  public @Nullable Integer getProgressPercent() {
    return progressPercent;
  }

  @JsonProperty("progressPercent")
  public void setProgressPercent(@Nullable Integer progressPercent) {
    this.progressPercent = progressPercent;
  }

  public Operation resultFileId(@Nullable UUID resultFileId) {
    this.resultFileId = resultFileId;
    return this;
  }

  /**
   * Output file (reports / exports).
   * @return resultFileId
   */
  @Valid 
  @JsonProperty("resultFileId")
  public @Nullable UUID getResultFileId() {
    return resultFileId;
  }

  @JsonProperty("resultFileId")
  public void setResultFileId(@Nullable UUID resultFileId) {
    this.resultFileId = resultFileId;
  }

  public Operation result(@Nullable Object result) {
    this.result = result;
    return this;
  }

  /**
   * Structured result summary.
   * @return result
   */
  
  @JsonProperty("result")
  public @Nullable Object getResult() {
    return result;
  }

  @JsonProperty("result")
  public void setResult(@Nullable Object result) {
    this.result = result;
  }

  public Operation errorMessage(@Nullable String errorMessage) {
    this.errorMessage = errorMessage;
    return this;
  }

  /**
   * Failure reason.
   * @return errorMessage
   */
  
  @JsonProperty("errorMessage")
  public @Nullable String getErrorMessage() {
    return errorMessage;
  }

  @JsonProperty("errorMessage")
  public void setErrorMessage(@Nullable String errorMessage) {
    this.errorMessage = errorMessage;
  }

  public Operation createTime(OffsetDateTime createTime) {
    this.createTime = createTime;
    return this;
  }

  /**
   * Submitted at.
   * @return createTime
   */
  @NotNull @Valid 
  @JsonProperty("createTime")
  public OffsetDateTime getCreateTime() {
    return createTime;
  }

  @JsonProperty("createTime")
  public void setCreateTime(OffsetDateTime createTime) {
    this.createTime = createTime;
  }

  public Operation doneTime(@Nullable OffsetDateTime doneTime) {
    this.doneTime = doneTime;
    return this;
  }

  /**
   * Finished at.
   * @return doneTime
   */
  @Valid 
  @JsonProperty("doneTime")
  public @Nullable OffsetDateTime getDoneTime() {
    return doneTime;
  }

  @JsonProperty("doneTime")
  public void setDoneTime(@Nullable OffsetDateTime doneTime) {
    this.doneTime = doneTime;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Operation operation = (Operation) o;
    return Objects.equals(this.operationId, operation.operationId) &&
        Objects.equals(this.module, operation.module) &&
        Objects.equals(this.kind, operation.kind) &&
        Objects.equals(this.status, operation.status) &&
        Objects.equals(this.progressPercent, operation.progressPercent) &&
        Objects.equals(this.resultFileId, operation.resultFileId) &&
        Objects.equals(this.result, operation.result) &&
        Objects.equals(this.errorMessage, operation.errorMessage) &&
        Objects.equals(this.createTime, operation.createTime) &&
        Objects.equals(this.doneTime, operation.doneTime);
  }

  @Override
  public int hashCode() {
    return Objects.hash(operationId, module, kind, status, progressPercent, resultFileId, result, errorMessage, createTime, doneTime);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class Operation {\n");
    sb.append("    operationId: ").append(toIndentedString(operationId)).append("\n");
    sb.append("    module: ").append(toIndentedString(module)).append("\n");
    sb.append("    kind: ").append(toIndentedString(kind)).append("\n");
    sb.append("    status: ").append(toIndentedString(status)).append("\n");
    sb.append("    progressPercent: ").append(toIndentedString(progressPercent)).append("\n");
    sb.append("    resultFileId: ").append(toIndentedString(resultFileId)).append("\n");
    sb.append("    result: ").append(toIndentedString(result)).append("\n");
    sb.append("    errorMessage: ").append(toIndentedString(errorMessage)).append("\n");
    sb.append("    createTime: ").append(toIndentedString(createTime)).append("\n");
    sb.append("    doneTime: ").append(toIndentedString(doneTime)).append("\n");
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

