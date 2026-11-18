// Owned API contract of platform.
package com.mysms.platform.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.springframework.lang.Nullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;

/**
 * ReportDefinition
 */

public class ReportDefinition {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private String reportCode;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private String module;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private String title;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private List<String> formats = new ArrayList<>();

  private @Nullable Object parameters = null;

  public ReportDefinition() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public ReportDefinition(String reportCode, String module, String title, List<String> formats) {
    this.reportCode = reportCode;
    this.module = module;
    this.title = title;
    this.formats = formats;
  }

  public ReportDefinition reportCode(String reportCode) {
    this.reportCode = reportCode;
    return this;
  }

  /**
   * Report code.
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

  public ReportDefinition module(String module) {
    this.module = module;
    return this;
  }

  /**
   * Owning module.
   * @return module
   */
  @NotNull 
  @JsonProperty("module")
  public String getModule() {
    return module;
  }

  @JsonProperty("module")
  public void setModule(String module) {
    this.module = module;
  }

  public ReportDefinition title(String title) {
    this.title = title;
    return this;
  }

  /**
   * Title.
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

  public ReportDefinition formats(List<String> formats) {
    this.formats = formats;
    return this;
  }

  public ReportDefinition addFormatsItem(String formatsItem) {
    if (this.formats == null) {
      this.formats = new ArrayList<>();
    }
    this.formats.add(formatsItem);
    return this;
  }

  /**
   * csv / xlsx / pdf.
   * @return formats
   */
  @NotNull 
  @JsonProperty("formats")
  public List<String> getFormats() {
    return formats;
  }

  @JsonProperty("formats")
  public void setFormats(List<String> formats) {
    this.formats = formats;
  }

  public ReportDefinition parameters(@Nullable Object parameters) {
    this.parameters = parameters;
    return this;
  }

  /**
   * JSON Schema of parameters.
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
    ReportDefinition reportDefinition = (ReportDefinition) o;
    return Objects.equals(this.reportCode, reportDefinition.reportCode) &&
        Objects.equals(this.module, reportDefinition.module) &&
        Objects.equals(this.title, reportDefinition.title) &&
        Objects.equals(this.formats, reportDefinition.formats) &&
        Objects.equals(this.parameters, reportDefinition.parameters);
  }

  @Override
  public int hashCode() {
    return Objects.hash(reportCode, module, title, formats, parameters);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ReportDefinition {\n");
    sb.append("    reportCode: ").append(toIndentedString(reportCode)).append("\n");
    sb.append("    module: ").append(toIndentedString(module)).append("\n");
    sb.append("    title: ").append(toIndentedString(title)).append("\n");
    sb.append("    formats: ").append(toIndentedString(formats)).append("\n");
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

