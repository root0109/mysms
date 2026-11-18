// Owned API contract of lms.
package com.mysms.lms.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import org.springframework.lang.Nullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;

/**
 * CancelImportExportJobRequest
 */

public class CancelImportExportJobRequest {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String etag;

  public CancelImportExportJobRequest etag(@Nullable String etag) {
    this.etag = etag;
    return this;
  }

  /**
   * Concurrency token from the last read (optional but recommended).
   * @return etag
   */
  
  @JsonProperty("etag")
  public @Nullable String getEtag() {
    return etag;
  }

  @JsonProperty("etag")
  public void setEtag(@Nullable String etag) {
    this.etag = etag;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    CancelImportExportJobRequest cancelImportExportJobRequest = (CancelImportExportJobRequest) o;
    return Objects.equals(this.etag, cancelImportExportJobRequest.etag);
  }

  @Override
  public int hashCode() {
    return Objects.hash(etag);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class CancelImportExportJobRequest {\n");
    sb.append("    etag: ").append(toIndentedString(etag)).append("\n");
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

