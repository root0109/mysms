// Owned API contract of iam.
package com.mysms.iam.api.model;

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
 * ImportUsersRequest
 */

public class ImportUsersRequest {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID fileId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable List<@Valid ImportUserRow> rows;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Boolean validateOnly;

  public ImportUsersRequest() {
    super();
  }

  public ImportUsersRequest fileId(@Nullable UUID fileId) {
    this.fileId = fileId;
    return this;
  }

  /**
   * Uploaded CSV / spreadsheet (platform file). Not available: iam cannot read the platform's file store; send rows.
   * @return fileId
   */
  @Valid 
  @JsonProperty("fileId")
  public @Nullable UUID getFileId() {
    return fileId;
  }

  @JsonProperty("fileId")
  public void setFileId(@Nullable UUID fileId) {
    this.fileId = fileId;
  }

  public ImportUsersRequest rows(@Nullable List<@Valid ImportUserRow> rows) {
    this.rows = rows;
    return this;
  }

  /**
   * The users to create, inline (1 to 500): give either rows or fileId.
   * @return rows
   */
  @Valid @Size(min = 1, max = 500) 
  @JsonProperty("rows")
  public @Nullable List<@Valid ImportUserRow> getRows() {
    return rows;
  }

  @JsonProperty("rows")
  public void setRows(@Nullable List<@Valid ImportUserRow> rows) {
    this.rows = rows;
  }

  public ImportUsersRequest validateOnly(@Nullable Boolean validateOnly) {
    this.validateOnly = validateOnly;
    return this;
  }

  /**
   * Check without importing.
   * @return validateOnly
   */
  
  @JsonProperty("validateOnly")
  public @Nullable Boolean getValidateOnly() {
    return validateOnly;
  }

  @JsonProperty("validateOnly")
  public void setValidateOnly(@Nullable Boolean validateOnly) {
    this.validateOnly = validateOnly;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ImportUsersRequest importUsersRequest = (ImportUsersRequest) o;
    return Objects.equals(this.fileId, importUsersRequest.fileId) &&
        Objects.equals(this.rows, importUsersRequest.rows) &&
        Objects.equals(this.validateOnly, importUsersRequest.validateOnly);
  }

  @Override
  public int hashCode() {
    return Objects.hash(fileId, rows, validateOnly);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ImportUsersRequest {\n");
    sb.append("    fileId: ").append(toIndentedString(fileId)).append("\n");
    sb.append("    rows: ").append(toIndentedString(rows)).append("\n");
    sb.append("    validateOnly: ").append(toIndentedString(validateOnly)).append("\n");
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

