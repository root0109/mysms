// Owned API contract of privacy.
package com.mysms.privacy.api.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;
import java.util.Objects;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.lang.Nullable;

/**
 * The processing register (RoPA) as CSV, one row per activity, for the data protection officer and auditors.
 */

public class ProcessingRegisterExport {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private String fileName;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private OffsetDateTime generatedAt;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private Integer activities;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private String csv;

  public ProcessingRegisterExport() {
    super();
  }

  public ProcessingRegisterExport fileName(String fileName) {
    this.fileName = fileName;
    return this;
  }

  /**
   * A file name for the CSV, processing-register-<date>.csv.
   * @return fileName
   */
  @NotNull 
  @JsonProperty("fileName")
  public String getFileName() {
    return fileName;
  }

  @JsonProperty("fileName")
  public void setFileName(String fileName) {
    this.fileName = fileName;
  }

  public ProcessingRegisterExport generatedAt(OffsetDateTime generatedAt) {
    this.generatedAt = generatedAt;
    return this;
  }

  /**
   * When the export was generated.
   * @return generatedAt
   */
  @NotNull @Valid 
  @JsonProperty("generatedAt")
  public OffsetDateTime getGeneratedAt() {
    return generatedAt;
  }

  @JsonProperty("generatedAt")
  public void setGeneratedAt(OffsetDateTime generatedAt) {
    this.generatedAt = generatedAt;
  }

  public ProcessingRegisterExport activities(Integer activities) {
    this.activities = activities;
    return this;
  }

  /**
   * How many activities (data rows) the CSV holds.
   * @return activities
   */
  @NotNull 
  @JsonProperty("activities")
  public Integer getActivities() {
    return activities;
  }

  @JsonProperty("activities")
  public void setActivities(Integer activities) {
    this.activities = activities;
  }

  public ProcessingRegisterExport csv(String csv) {
    this.csv = csv;
    return this;
  }

  /**
   * The register as RFC 4180 CSV (UTF-8, CRLF rows, a header row; a cell opening with =, +, - or @ is prefixed with an apostrophe).
   * @return csv
   */
  @NotNull 
  @JsonProperty("csv")
  public String getCsv() {
    return csv;
  }

  @JsonProperty("csv")
  public void setCsv(String csv) {
    this.csv = csv;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ProcessingRegisterExport other = (ProcessingRegisterExport) o;
    return Objects.equals(this.fileName, other.fileName) &&
        Objects.equals(this.generatedAt, other.generatedAt) &&
        Objects.equals(this.activities, other.activities) &&
        Objects.equals(this.csv, other.csv);
  }

  @Override
  public int hashCode() {
    return Objects.hash(fileName, generatedAt, activities, csv);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ProcessingRegisterExport {\n");
    sb.append("    fileName: ").append(toIndentedString(fileName)).append("\n");
    sb.append("    generatedAt: ").append(toIndentedString(generatedAt)).append("\n");
    sb.append("    activities: ").append(toIndentedString(activities)).append("\n");
    sb.append("    csv: ").append(toIndentedString(csv)).append("\n");
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
