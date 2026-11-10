// Owned API contract of platform (JAVA-CODING-RULES §7); first materialized from api/openapi/platform.yaml on 2026-11-10.
package com.mysms.platform.api.model;

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
 * SearchHit
 */

public class SearchHit {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private String entityType;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private UUID entityId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private String title;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String subtitle;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String urlPath;

  public SearchHit() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public SearchHit(String entityType, UUID entityId, String title) {
    this.entityType = entityType;
    this.entityId = entityId;
    this.title = title;
  }

  public SearchHit entityType(String entityType) {
    this.entityType = entityType;
    return this;
  }

  /**
   * Resource type, e.g. sis.student.
   * @return entityType
   */
  @NotNull 
  @JsonProperty("entityType")
  public String getEntityType() {
    return entityType;
  }

  @JsonProperty("entityType")
  public void setEntityType(String entityType) {
    this.entityType = entityType;
  }

  public SearchHit entityId(UUID entityId) {
    this.entityId = entityId;
    return this;
  }

  /**
   * Record id.
   * @return entityId
   */
  @NotNull @Valid 
  @JsonProperty("entityId")
  public UUID getEntityId() {
    return entityId;
  }

  @JsonProperty("entityId")
  public void setEntityId(UUID entityId) {
    this.entityId = entityId;
  }

  public SearchHit title(String title) {
    this.title = title;
    return this;
  }

  /**
   * Display title.
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

  public SearchHit subtitle(@Nullable String subtitle) {
    this.subtitle = subtitle;
    return this;
  }

  /**
   * Secondary text.
   * @return subtitle
   */
  
  @JsonProperty("subtitle")
  public @Nullable String getSubtitle() {
    return subtitle;
  }

  @JsonProperty("subtitle")
  public void setSubtitle(@Nullable String subtitle) {
    this.subtitle = subtitle;
  }

  public SearchHit urlPath(@Nullable String urlPath) {
    this.urlPath = urlPath;
    return this;
  }

  /**
   * App route.
   * @return urlPath
   */
  
  @JsonProperty("urlPath")
  public @Nullable String getUrlPath() {
    return urlPath;
  }

  @JsonProperty("urlPath")
  public void setUrlPath(@Nullable String urlPath) {
    this.urlPath = urlPath;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    SearchHit searchHit = (SearchHit) o;
    return Objects.equals(this.entityType, searchHit.entityType) &&
        Objects.equals(this.entityId, searchHit.entityId) &&
        Objects.equals(this.title, searchHit.title) &&
        Objects.equals(this.subtitle, searchHit.subtitle) &&
        Objects.equals(this.urlPath, searchHit.urlPath);
  }

  @Override
  public int hashCode() {
    return Objects.hash(entityType, entityId, title, subtitle, urlPath);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class SearchHit {\n");
    sb.append("    entityType: ").append(toIndentedString(entityType)).append("\n");
    sb.append("    entityId: ").append(toIndentedString(entityId)).append("\n");
    sb.append("    title: ").append(toIndentedString(title)).append("\n");
    sb.append("    subtitle: ").append(toIndentedString(subtitle)).append("\n");
    sb.append("    urlPath: ").append(toIndentedString(urlPath)).append("\n");
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

