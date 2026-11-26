// Owned API contract of privacy.
package com.mysms.privacy.api.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.lang.Nullable;

/**
 * What a correction request is about: the record, in the module that holds it, and the fields to correct. privacy never writes another module's data: the school's staff correct the record there.
 */

public class RightsRequestCorrection {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private String module;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private String recordType;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID recordId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private List<RightsRequestCorrectionField> fields = new ArrayList<>();

  public RightsRequestCorrection() {
    super();
  }

  public RightsRequestCorrection module(String module) {
    this.module = module;
    return this;
  }

  /**
   * The module that holds the record (people, sis, hr, admission …).
   * @return module
   */
  @NotNull @Size(max = 30) @Pattern(regexp = "^[a-z]+$") 
  @JsonProperty("module")
  public String getModule() {
    return module;
  }

  @JsonProperty("module")
  public void setModule(String module) {
    this.module = module;
  }

  public RightsRequestCorrection recordType(String recordType) {
    this.recordType = recordType;
    return this;
  }

  /**
   * The kind of record (person, student, guardian, staff …).
   * @return recordType
   */
  @NotNull @Size(min = 1, max = 60) 
  @JsonProperty("recordType")
  public String getRecordType() {
    return recordType;
  }

  @JsonProperty("recordType")
  public void setRecordType(String recordType) {
    this.recordType = recordType;
  }

  public RightsRequestCorrection recordId(@Nullable UUID recordId) {
    this.recordId = recordId;
    return this;
  }

  /**
   * The record's id when the requester knows it.
   * @return recordId
   */
  @Valid 
  @JsonProperty("recordId")
  public @Nullable UUID getRecordId() {
    return recordId;
  }

  @JsonProperty("recordId")
  public void setRecordId(@Nullable UUID recordId) {
    this.recordId = recordId;
  }

  public RightsRequestCorrection fields(List<RightsRequestCorrectionField> fields) {
    this.fields = fields;
    return this;
  }

  public RightsRequestCorrection addFieldsItem(RightsRequestCorrectionField fieldsItem) {
    if (this.fields == null) {
      this.fields = new ArrayList<>();
    }
    this.fields.add(fieldsItem);
    return this;
  }

  /**
   * The fields to correct (1 to 20).
   * @return fields
   */
  @NotNull @Size(min = 1, max = 20) 
  @JsonProperty("fields")
  public List<@Valid RightsRequestCorrectionField> getFields() {
    return fields;
  }

  @JsonProperty("fields")
  public void setFields(List<RightsRequestCorrectionField> fields) {
    this.fields = fields;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    RightsRequestCorrection other = (RightsRequestCorrection) o;
    return Objects.equals(this.module, other.module) &&
        Objects.equals(this.recordType, other.recordType) &&
        Objects.equals(this.recordId, other.recordId) &&
        Objects.equals(this.fields, other.fields);
  }

  @Override
  public int hashCode() {
    return Objects.hash(module, recordType, recordId, fields);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class RightsRequestCorrection {\n");
    sb.append("    module: ").append(toIndentedString(module)).append("\n");
    sb.append("    recordType: ").append(toIndentedString(recordType)).append("\n");
    sb.append("    recordId: ").append(toIndentedString(recordId)).append("\n");
    sb.append("    fields: ").append(toIndentedString(fields)).append("\n");
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
