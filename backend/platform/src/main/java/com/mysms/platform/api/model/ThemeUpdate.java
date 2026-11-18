// Owned API contract of platform.
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
 * Fields of Theme that can be updated (partial update; see updateMask).
 */

public class ThemeUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String name;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String description;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID thumbnailFileId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID thumbnailAltFileId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID logoFileId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Boolean isDefault;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Object tokens = null;

  public ThemeUpdate name(@Nullable String name) {
    this.name = name;
    return this;
  }

  /**
   * Theme name.
   * @return name
   */
  @Size(max = 120) 
  @JsonProperty("name")
  public @Nullable String getName() {
    return name;
  }

  @JsonProperty("name")
  public void setName(@Nullable String name) {
    this.name = name;
  }

  public ThemeUpdate description(@Nullable String description) {
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

  public ThemeUpdate thumbnailFileId(@Nullable UUID thumbnailFileId) {
    this.thumbnailFileId = thumbnailFileId;
    return this;
  }

  /**
   * Preview image.
   * @return thumbnailFileId
   */
  @Valid 
  @JsonProperty("thumbnailFileId")
  public @Nullable UUID getThumbnailFileId() {
    return thumbnailFileId;
  }

  @JsonProperty("thumbnailFileId")
  public void setThumbnailFileId(@Nullable UUID thumbnailFileId) {
    this.thumbnailFileId = thumbnailFileId;
  }

  public ThemeUpdate thumbnailAltFileId(@Nullable UUID thumbnailAltFileId) {
    this.thumbnailAltFileId = thumbnailAltFileId;
    return this;
  }

  /**
   * Second preview image (e.g. dark variant).
   * @return thumbnailAltFileId
   */
  @Valid 
  @JsonProperty("thumbnailAltFileId")
  public @Nullable UUID getThumbnailAltFileId() {
    return thumbnailAltFileId;
  }

  @JsonProperty("thumbnailAltFileId")
  public void setThumbnailAltFileId(@Nullable UUID thumbnailAltFileId) {
    this.thumbnailAltFileId = thumbnailAltFileId;
  }

  public ThemeUpdate logoFileId(@Nullable UUID logoFileId) {
    this.logoFileId = logoFileId;
    return this;
  }

  /**
   * Logo shown on sign-in pages, the app shell and the PWA manifest (the branding of the tenant's default theme).
   * @return logoFileId
   */
  @Valid 
  @JsonProperty("logoFileId")
  public @Nullable UUID getLogoFileId() {
    return logoFileId;
  }

  @JsonProperty("logoFileId")
  public void setLogoFileId(@Nullable UUID logoFileId) {
    this.logoFileId = logoFileId;
  }

  public ThemeUpdate isDefault(@Nullable Boolean isDefault) {
    this.isDefault = isDefault;
    return this;
  }

  /**
   * The tenant's own theme: the one public branding uses. At most one per tenant; setting it moves the mark.
   * @return isDefault
   */
  
  @JsonProperty("isDefault")
  public @Nullable Boolean getIsDefault() {
    return isDefault;
  }

  @JsonProperty("isDefault")
  public void setIsDefault(@Nullable Boolean isDefault) {
    this.isDefault = isDefault;
  }

  public ThemeUpdate tokens(@Nullable Object tokens) {
    this.tokens = tokens;
    return this;
  }

  /**
   * Design tokens (colours, fonts) applied by the web and mobile apps.
   * @return tokens
   */
  
  @JsonProperty("tokens")
  public @Nullable Object getTokens() {
    return tokens;
  }

  @JsonProperty("tokens")
  public void setTokens(@Nullable Object tokens) {
    this.tokens = tokens;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ThemeUpdate themeUpdate = (ThemeUpdate) o;
    return Objects.equals(this.name, themeUpdate.name) &&
        Objects.equals(this.description, themeUpdate.description) &&
        Objects.equals(this.thumbnailFileId, themeUpdate.thumbnailFileId) &&
        Objects.equals(this.thumbnailAltFileId, themeUpdate.thumbnailAltFileId) &&
        Objects.equals(this.logoFileId, themeUpdate.logoFileId) &&
        Objects.equals(this.isDefault, themeUpdate.isDefault) &&
        Objects.equals(this.tokens, themeUpdate.tokens);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, description, thumbnailFileId, thumbnailAltFileId, logoFileId, isDefault, tokens);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ThemeUpdate {\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    description: ").append(toIndentedString(description)).append("\n");
    sb.append("    thumbnailFileId: ").append(toIndentedString(thumbnailFileId)).append("\n");
    sb.append("    thumbnailAltFileId: ").append(toIndentedString(thumbnailAltFileId)).append("\n");
    sb.append("    logoFileId: ").append(toIndentedString(logoFileId)).append("\n");
    sb.append("    isDefault: ").append(toIndentedString(isDefault)).append("\n");
    sb.append("    tokens: ").append(toIndentedString(tokens)).append("\n");
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

