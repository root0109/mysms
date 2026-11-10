// Owned API contract of platform (JAVA-CODING-RULES §7); first materialized from api/openapi/platform.yaml on 2026-11-10.
package com.mysms.platform.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import org.springframework.lang.Nullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;

/**
 * RunReportRequest
 */

public class RunReportRequest {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private String reportCode;

  /**
   * Output format.
   */
  public enum FormatEnum {
    CSV("csv"),
    
    XLSX("xlsx"),
    
    PDF("pdf");

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

  private @Nullable Object parameters = null;

  public RunReportRequest() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public RunReportRequest(String reportCode, FormatEnum format) {
    this.reportCode = reportCode;
    this.format = format;
  }

  public RunReportRequest reportCode(String reportCode) {
    this.reportCode = reportCode;
    return this;
  }

  /**
   * Report code (see ListReportDefinitions).
   * @return reportCode
   */
  @NotNull 
  @JsonProperty("reportCode")
  public String getReportCode() {
    return reportCode;
  }

  @JsonProperty("reportCode")
  public void setReportCode(String reportCode) {
    this.reportCode = reportCode;
  }

  public RunReportRequest format(FormatEnum format) {
    this.format = format;
    return this;
  }

  /**
   * Output format.
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

  public RunReportRequest parameters(@Nullable Object parameters) {
    this.parameters = parameters;
    return this;
  }

  /**
   * Report parameters.
   * @return parameters
   */
  
  @JsonProperty("parameters")
  public @Nullable Object getParameters() {
    return parameters;
  }

  @JsonProperty("parameters")
  public void setParameters(@Nullable Object parameters) {
    this.parameters = parameters;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    RunReportRequest runReportRequest = (RunReportRequest) o;
    return Objects.equals(this.reportCode, runReportRequest.reportCode) &&
        Objects.equals(this.format, runReportRequest.format) &&
        Objects.equals(this.parameters, runReportRequest.parameters);
  }

  @Override
  public int hashCode() {
    return Objects.hash(reportCode, format, parameters);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class RunReportRequest {\n");
    sb.append("    reportCode: ").append(toIndentedString(reportCode)).append("\n");
    sb.append("    format: ").append(toIndentedString(format)).append("\n");
    sb.append("    parameters: ").append(toIndentedString(parameters)).append("\n");
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

