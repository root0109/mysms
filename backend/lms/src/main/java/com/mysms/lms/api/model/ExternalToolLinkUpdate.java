// Owned API contract of lms (JAVA-CODING-RULES §7); first materialized from api/openapi/lms.yaml on 2026-11-10.
package com.mysms.lms.api.model;

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
 * Fields of ExternalToolLink that can be updated (partial update; see updateMask).
 */

public class ExternalToolLinkUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID externalToolId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String linkRef;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String linkTitle;

  public ExternalToolLinkUpdate externalToolId(@Nullable UUID externalToolId) {
    this.externalToolId = externalToolId;
    return this;
  }

  /**
   * Tool that serves the link.
   * @return externalToolId
   */
  @Valid 
  @JsonProperty("externalToolId")
  public @Nullable UUID getExternalToolId() {
    return externalToolId;
  }

  @JsonProperty("externalToolId")
  public void setExternalToolId(@Nullable UUID externalToolId) {
    this.externalToolId = externalToolId;
  }

  public ExternalToolLinkUpdate linkRef(@Nullable String linkRef) {
    this.linkRef = linkRef;
    return this;
  }

  /**
   * External resource-link identifier in the tool.
   * @return linkRef
   */
  @Size(max = 255) 
  @JsonProperty("linkRef")
  public @Nullable String getLinkRef() {
    return linkRef;
  }

  @JsonProperty("linkRef")
  public void setLinkRef(@Nullable String linkRef) {
    this.linkRef = linkRef;
  }

  public ExternalToolLinkUpdate linkTitle(@Nullable String linkTitle) {
    this.linkTitle = linkTitle;
    return this;
  }

  /**
   * Link title.
   * @return linkTitle
   */
  @Size(max = 255) 
  @JsonProperty("linkTitle")
  public @Nullable String getLinkTitle() {
    return linkTitle;
  }

  @JsonProperty("linkTitle")
  public void setLinkTitle(@Nullable String linkTitle) {
    this.linkTitle = linkTitle;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ExternalToolLinkUpdate externalToolLinkUpdate = (ExternalToolLinkUpdate) o;
    return Objects.equals(this.externalToolId, externalToolLinkUpdate.externalToolId) &&
        Objects.equals(this.linkRef, externalToolLinkUpdate.linkRef) &&
        Objects.equals(this.linkTitle, externalToolLinkUpdate.linkTitle);
  }

  @Override
  public int hashCode() {
    return Objects.hash(externalToolId, linkRef, linkTitle);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ExternalToolLinkUpdate {\n");
    sb.append("    externalToolId: ").append(toIndentedString(externalToolId)).append("\n");
    sb.append("    linkRef: ").append(toIndentedString(linkRef)).append("\n");
    sb.append("    linkTitle: ").append(toIndentedString(linkTitle)).append("\n");
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

