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
 * Fields of Theme that can be updated (PATCH: a property left out keeps its value, null clears it, a value sets it).
 */

public class ThemeUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull @Size(max = 120) String> name;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<String> description;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<UUID> thumbnailFileId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<UUID> thumbnailAltFileId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<UUID> logoFileId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull Boolean> isDefault;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<Object> tokens;

  public ThemeUpdate name(@Nullable Optional<String> name) {
    this.name = name;
    return this;
  }

  /**
   * Theme name.
   * @return name
   */
  
  @JsonProperty("name")
  public @Nullable Optional<String> getName() {
    return name;
  }

  @JsonProperty("name")
  public void setName(@Nullable Optional<String> name) {
    this.name = name;
  }

  public ThemeUpdate description(@Nullable Optional<String> description) {
    this.description = description;
    return this;
  }

  /**
   * Description.
   * @return description
   */
  
  @JsonProperty("description")
  public @Nullable Optional<String> getDescription() {
    return description;
  }

  @JsonProperty("description")
  public void setDescription(@Nullable Optional<String> description) {
    this.description = description;
  }

  public ThemeUpdate thumbnailFileId(@Nullable Optional<UUID> thumbnailFileId) {
    this.thumbnailFileId = thumbnailFileId;
    return this;
  }

  /**
   * Preview image.
   * @return thumbnailFileId
   */
  
  @JsonProperty("thumbnailFileId")
  public @Nullable Optional<UUID> getThumbnailFileId() {
    return thumbnailFileId;
  }

  @JsonProperty("thumbnailFileId")
  public void setThumbnailFileId(@Nullable Optional<UUID> thumbnailFileId) {
    this.thumbnailFileId = thumbnailFileId;
  }

  public ThemeUpdate thumbnailAltFileId(@Nullable Optional<UUID> thumbnailAltFileId) {
    this.thumbnailAltFileId = thumbnailAltFileId;
    return this;
  }

  /**
   * Second preview image (e.g. dark variant).
   * @return thumbnailAltFileId
   */
  
  @JsonProperty("thumbnailAltFileId")
  public @Nullable Optional<UUID> getThumbnailAltFileId() {
    return thumbnailAltFileId;
  }

  @JsonProperty("thumbnailAltFileId")
  public void setThumbnailAltFileId(@Nullable Optional<UUID> thumbnailAltFileId) {
    this.thumbnailAltFileId = thumbnailAltFileId;
  }

  public ThemeUpdate logoFileId(@Nullable Optional<UUID> logoFileId) {
    this.logoFileId = logoFileId;
    return this;
  }

  /**
   * Logo shown on sign-in pages, the app shell and the PWA manifest (the branding of the tenant's default theme).
   * @return logoFileId
   */
  
  @JsonProperty("logoFileId")
  public @Nullable Optional<UUID> getLogoFileId() {
    return logoFileId;
  }

  @JsonProperty("logoFileId")
  public void setLogoFileId(@Nullable Optional<UUID> logoFileId) {
    this.logoFileId = logoFileId;
  }

  public ThemeUpdate isDefault(@Nullable Optional<Boolean> isDefault) {
    this.isDefault = isDefault;
    return this;
  }

  /**
   * The tenant's own theme: the one public branding uses. At most one per tenant; setting it moves the mark.
   * @return isDefault
   */
  
  @JsonProperty("isDefault")
  public @Nullable Optional<Boolean> getIsDefault() {
    return isDefault;
  }

  @JsonProperty("isDefault")
  public void setIsDefault(@Nullable Optional<Boolean> isDefault) {
    this.isDefault = isDefault;
  }

  public ThemeUpdate tokens(@Nullable Optional<Object> tokens) {
    this.tokens = tokens;
    return this;
  }

  /**
   * Design tokens (colours, fonts) applied by the web and mobile apps.
   * @return tokens
   */
  
  @JsonProperty("tokens")
  public @Nullable Optional<Object> getTokens() {
    return tokens;
  }

  @JsonProperty("tokens")
  public void setTokens(@Nullable Optional<Object> tokens) {
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

