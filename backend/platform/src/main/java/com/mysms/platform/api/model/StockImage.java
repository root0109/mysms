// Owned API contract of platform (JAVA-CODING-RULES §7); first materialized from api/openapi/platform.yaml on 2026-11-10.
package com.mysms.platform.api.model;

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
 * StockImage
 */

public class StockImage {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private String url;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private String thumbUrl;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String author;

  public StockImage() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public StockImage(String url, String thumbUrl) {
    this.url = url;
    this.thumbUrl = thumbUrl;
  }

  public StockImage url(String url) {
    this.url = url;
    return this;
  }

  /**
   * Image URL.
   * @return url
   */
  @NotNull 
  @JsonProperty("url")
  public String getUrl() {
    return url;
  }

  @JsonProperty("url")
  public void setUrl(String url) {
    this.url = url;
  }

  public StockImage thumbUrl(String thumbUrl) {
    this.thumbUrl = thumbUrl;
    return this;
  }

  /**
   * Thumbnail.
   * @return thumbUrl
   */
  @NotNull 
  @JsonProperty("thumbUrl")
  public String getThumbUrl() {
    return thumbUrl;
  }

  @JsonProperty("thumbUrl")
  public void setThumbUrl(String thumbUrl) {
    this.thumbUrl = thumbUrl;
  }

  public StockImage author(@Nullable String author) {
    this.author = author;
    return this;
  }

  /**
   * Photographer credit.
   * @return author
   */
  
  @JsonProperty("author")
  public @Nullable String getAuthor() {
    return author;
  }

  @JsonProperty("author")
  public void setAuthor(@Nullable String author) {
    this.author = author;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    StockImage stockImage = (StockImage) o;
    return Objects.equals(this.url, stockImage.url) &&
        Objects.equals(this.thumbUrl, stockImage.thumbUrl) &&
        Objects.equals(this.author, stockImage.author);
  }

  @Override
  public int hashCode() {
    return Objects.hash(url, thumbUrl, author);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class StockImage {\n");
    sb.append("    url: ").append(toIndentedString(url)).append("\n");
    sb.append("    thumbUrl: ").append(toIndentedString(thumbUrl)).append("\n");
    sb.append("    author: ").append(toIndentedString(author)).append("\n");
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

