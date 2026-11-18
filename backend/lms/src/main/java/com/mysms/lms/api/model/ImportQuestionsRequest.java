// Owned API contract of lms.
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
 * ImportQuestionsRequest
 */

public class ImportQuestionsRequest {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private UUID fileId;

  /**
   * File format.
   */
  public enum FormatEnum {
    GIFT("gift"),
    
    QTI("qti"),
    
    EXAMVIEW("examview"),
    
    WEBCT("webct"),
    
    CSV("csv");

    private final String value;

    FormatEnum(String value) {
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
    public static FormatEnum fromValue(String value) {
      for (FormatEnum b : FormatEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private FormatEnum format;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID questionPoolId;

  public ImportQuestionsRequest() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public ImportQuestionsRequest(UUID fileId, FormatEnum format) {
    this.fileId = fileId;
    this.format = format;
  }

  public ImportQuestionsRequest fileId(UUID fileId) {
    this.fileId = fileId;
    return this;
  }

  /**
   * Uploaded file.
   * @return fileId
   */
  @NotNull @Valid 
  @JsonProperty("fileId")
  public UUID getFileId() {
    return fileId;
  }

  @JsonProperty("fileId")
  public void setFileId(UUID fileId) {
    this.fileId = fileId;
  }

  public ImportQuestionsRequest format(FormatEnum format) {
    this.format = format;
    return this;
  }

  /**
   * File format.
   * @return format
   */
  @NotNull 
  @JsonProperty("format")
  public FormatEnum getFormat() {
    return format;
  }

  @JsonProperty("format")
  public void setFormat(FormatEnum format) {
    this.format = format;
  }

  public ImportQuestionsRequest questionPoolId(@Nullable UUID questionPoolId) {
    this.questionPoolId = questionPoolId;
    return this;
  }

  /**
   * Also add to this pool.
   * @return questionPoolId
   */
  @Valid 
  @JsonProperty("questionPoolId")
  public @Nullable UUID getQuestionPoolId() {
    return questionPoolId;
  }

  @JsonProperty("questionPoolId")
  public void setQuestionPoolId(@Nullable UUID questionPoolId) {
    this.questionPoolId = questionPoolId;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ImportQuestionsRequest importQuestionsRequest = (ImportQuestionsRequest) o;
    return Objects.equals(this.fileId, importQuestionsRequest.fileId) &&
        Objects.equals(this.format, importQuestionsRequest.format) &&
        Objects.equals(this.questionPoolId, importQuestionsRequest.questionPoolId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(fileId, format, questionPoolId);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ImportQuestionsRequest {\n");
    sb.append("    fileId: ").append(toIndentedString(fileId)).append("\n");
    sb.append("    format: ").append(toIndentedString(format)).append("\n");
    sb.append("    questionPoolId: ").append(toIndentedString(questionPoolId)).append("\n");
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

