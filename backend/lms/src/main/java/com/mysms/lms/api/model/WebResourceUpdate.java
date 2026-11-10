// Owned API contract of lms (JAVA-CODING-RULES §7); first materialized from api/openapi/lms.yaml on 2026-11-10.
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
 * Fields of WebResource that can be updated (partial update; see updateMask).
 */

public class WebResourceUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID institutionId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String title;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String description;

  /**
   * External URL or uploaded package.
   */
  public enum ResourceTypeEnum {
    URL("url"),
    
    PACKAGE("package");

    private final String value;

    ResourceTypeEnum(String value) {
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
    public static ResourceTypeEnum fromValue(String value) {
      for (ResourceTypeEnum b : ResourceTypeEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable ResourceTypeEnum resourceType;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String url;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String launchFile;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String windowFeatures;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID categoryId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Boolean isInRepository;

  public WebResourceUpdate institutionId(@Nullable UUID institutionId) {
    this.institutionId = institutionId;
    return this;
  }

  /**
   * Owning institution.
   * @return institutionId
   */
  @Valid 
  @JsonProperty("institutionId")
  public @Nullable UUID getInstitutionId() {
    return institutionId;
  }

  @JsonProperty("institutionId")
  public void setInstitutionId(@Nullable UUID institutionId) {
    this.institutionId = institutionId;
  }

  public WebResourceUpdate title(@Nullable String title) {
    this.title = title;
    return this;
  }

  /**
   * Title.
   * @return title
   */
  @Size(max = 255) 
  @JsonProperty("title")
  public @Nullable String getTitle() {
    return title;
  }

  @JsonProperty("title")
  public void setTitle(@Nullable String title) {
    this.title = title;
  }

  public WebResourceUpdate description(@Nullable String description) {
    this.description = description;
    return this;
  }

  /**
   * Description.
   * @return description
   */
  
  @JsonProperty("description")
  public @Nullable String getDescription() {
    return description;
  }

  @JsonProperty("description")
  public void setDescription(@Nullable String description) {
    this.description = description;
  }

  public WebResourceUpdate resourceType(@Nullable ResourceTypeEnum resourceType) {
    this.resourceType = resourceType;
    return this;
  }

  /**
   * External URL or uploaded package.
   * @return resourceType
   */
  
  @JsonProperty("resourceType")
  public @Nullable ResourceTypeEnum getResourceType() {
    return resourceType;
  }

  @JsonProperty("resourceType")
  public void setResourceType(@Nullable ResourceTypeEnum resourceType) {
    this.resourceType = resourceType;
  }

  public WebResourceUpdate url(@Nullable String url) {
    this.url = url;
    return this;
  }

  /**
   * URL (resource_type = url).
   * @return url
   */
  @Size(max = 1000) 
  @JsonProperty("url")
  public @Nullable String getUrl() {
    return url;
  }

  @JsonProperty("url")
  public void setUrl(@Nullable String url) {
    this.url = url;
  }

  public WebResourceUpdate launchFile(@Nullable String launchFile) {
    this.launchFile = launchFile;
    return this;
  }

  /**
   * Entry file inside the package.
   * @return launchFile
   */
  @Size(max = 500) 
  @JsonProperty("launchFile")
  public @Nullable String getLaunchFile() {
    return launchFile;
  }

  @JsonProperty("launchFile")
  public void setLaunchFile(@Nullable String launchFile) {
    this.launchFile = launchFile;
  }

  public WebResourceUpdate windowFeatures(@Nullable String windowFeatures) {
    this.windowFeatures = windowFeatures;
    return this;
  }

  /**
   * Launch window options (size, toolbar...).
   * @return windowFeatures
   */
  @Size(max = 500) 
  @JsonProperty("windowFeatures")
  public @Nullable String getWindowFeatures() {
    return windowFeatures;
  }

  @JsonProperty("windowFeatures")
  public void setWindowFeatures(@Nullable String windowFeatures) {
    this.windowFeatures = windowFeatures;
  }

  public WebResourceUpdate categoryId(@Nullable UUID categoryId) {
    this.categoryId = categoryId;
    return this;
  }

  /**
   * Category.
   * @return categoryId
   */
  @Valid 
  @JsonProperty("categoryId")
  public @Nullable UUID getCategoryId() {
    return categoryId;
  }

  @JsonProperty("categoryId")
  public void setCategoryId(@Nullable UUID categoryId) {
    this.categoryId = categoryId;
  }

  public WebResourceUpdate isInRepository(@Nullable Boolean isInRepository) {
    this.isInRepository = isInRepository;
    return this;
  }

  /**
   * Listed in the shared content repository.
   * @return isInRepository
   */
  
  @JsonProperty("isInRepository")
  public @Nullable Boolean getIsInRepository() {
    return isInRepository;
  }

  @JsonProperty("isInRepository")
  public void setIsInRepository(@Nullable Boolean isInRepository) {
    this.isInRepository = isInRepository;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    WebResourceUpdate webResourceUpdate = (WebResourceUpdate) o;
    return Objects.equals(this.institutionId, webResourceUpdate.institutionId) &&
        Objects.equals(this.title, webResourceUpdate.title) &&
        Objects.equals(this.description, webResourceUpdate.description) &&
        Objects.equals(this.resourceType, webResourceUpdate.resourceType) &&
        Objects.equals(this.url, webResourceUpdate.url) &&
        Objects.equals(this.launchFile, webResourceUpdate.launchFile) &&
        Objects.equals(this.windowFeatures, webResourceUpdate.windowFeatures) &&
        Objects.equals(this.categoryId, webResourceUpdate.categoryId) &&
        Objects.equals(this.isInRepository, webResourceUpdate.isInRepository);
  }

  @Override
  public int hashCode() {
    return Objects.hash(institutionId, title, description, resourceType, url, launchFile, windowFeatures, categoryId, isInRepository);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class WebResourceUpdate {\n");
    sb.append("    institutionId: ").append(toIndentedString(institutionId)).append("\n");
    sb.append("    title: ").append(toIndentedString(title)).append("\n");
    sb.append("    description: ").append(toIndentedString(description)).append("\n");
    sb.append("    resourceType: ").append(toIndentedString(resourceType)).append("\n");
    sb.append("    url: ").append(toIndentedString(url)).append("\n");
    sb.append("    launchFile: ").append(toIndentedString(launchFile)).append("\n");
    sb.append("    windowFeatures: ").append(toIndentedString(windowFeatures)).append("\n");
    sb.append("    categoryId: ").append(toIndentedString(categoryId)).append("\n");
    sb.append("    isInRepository: ").append(toIndentedString(isInRepository)).append("\n");
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

