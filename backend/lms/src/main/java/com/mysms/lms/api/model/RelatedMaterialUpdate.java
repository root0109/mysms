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
 * Fields of RelatedMaterial that can be updated (partial update; see updateMask).
 */

public class RelatedMaterialUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String title;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String url;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String summary;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String source;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String mediaType;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String contentLength;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String contentLengthBand;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String videoLengthSeconds;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String videoLengthBand;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Integer initialRank;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Integer interactivityLevel;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String interactivityBand;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String conceptualLevel;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String languageComplexity;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String languageComplexityBand;

  public RelatedMaterialUpdate title(@Nullable String title) {
    this.title = title;
    return this;
  }

  /**
   * Page title.
   * @return title
   */
  @Size(max = 500) 
  @JsonProperty("title")
  public @Nullable String getTitle() {
    return title;
  }

  @JsonProperty("title")
  public void setTitle(@Nullable String title) {
    this.title = title;
  }

  public RelatedMaterialUpdate url(@Nullable String url) {
    this.url = url;
    return this;
  }

  /**
   * Page URL.
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

  public RelatedMaterialUpdate summary(@Nullable String summary) {
    this.summary = summary;
    return this;
  }

  /**
   * Page description.
   * @return summary
   */
  
  @JsonProperty("summary")
  public @Nullable String getSummary() {
    return summary;
  }

  @JsonProperty("summary")
  public void setSummary(@Nullable String summary) {
    this.summary = summary;
  }

  public RelatedMaterialUpdate source(@Nullable String source) {
    this.source = source;
    return this;
  }

  /**
   * Source (search engine, video site...).
   * @return source
   */
  @Size(max = 100) 
  @JsonProperty("source")
  public @Nullable String getSource() {
    return source;
  }

  @JsonProperty("source")
  public void setSource(@Nullable String source) {
    this.source = source;
  }

  public RelatedMaterialUpdate mediaType(@Nullable String mediaType) {
    this.mediaType = mediaType;
    return this;
  }

  /**
   * Media type (article, video...).
   * @return mediaType
   */
  @Size(max = 40) 
  @JsonProperty("mediaType")
  public @Nullable String getMediaType() {
    return mediaType;
  }

  @JsonProperty("mediaType")
  public void setMediaType(@Nullable String mediaType) {
    this.mediaType = mediaType;
  }

  public RelatedMaterialUpdate contentLength(@Nullable String contentLength) {
    this.contentLength = contentLength;
    return this;
  }

  /**
   * Length of the text content.
   * @return contentLength
   */
  @Pattern(regexp = "^-?[0-9]+$") 
  @JsonProperty("contentLength")
  public @Nullable String getContentLength() {
    return contentLength;
  }

  @JsonProperty("contentLength")
  public void setContentLength(@Nullable String contentLength) {
    this.contentLength = contentLength;
  }

  public RelatedMaterialUpdate contentLengthBand(@Nullable String contentLengthBand) {
    this.contentLengthBand = contentLengthBand;
    return this;
  }

  /**
   * Length band (short, medium, long).
   * @return contentLengthBand
   */
  @Size(max = 40) 
  @JsonProperty("contentLengthBand")
  public @Nullable String getContentLengthBand() {
    return contentLengthBand;
  }

  @JsonProperty("contentLengthBand")
  public void setContentLengthBand(@Nullable String contentLengthBand) {
    this.contentLengthBand = contentLengthBand;
  }

  public RelatedMaterialUpdate videoLengthSeconds(@Nullable String videoLengthSeconds) {
    this.videoLengthSeconds = videoLengthSeconds;
    return this;
  }

  /**
   * Video length.
   * @return videoLengthSeconds
   */
  @Pattern(regexp = "^-?[0-9]+$") 
  @JsonProperty("videoLengthSeconds")
  public @Nullable String getVideoLengthSeconds() {
    return videoLengthSeconds;
  }

  @JsonProperty("videoLengthSeconds")
  public void setVideoLengthSeconds(@Nullable String videoLengthSeconds) {
    this.videoLengthSeconds = videoLengthSeconds;
  }

  public RelatedMaterialUpdate videoLengthBand(@Nullable String videoLengthBand) {
    this.videoLengthBand = videoLengthBand;
    return this;
  }

  /**
   * Video length band.
   * @return videoLengthBand
   */
  @Size(max = 40) 
  @JsonProperty("videoLengthBand")
  public @Nullable String getVideoLengthBand() {
    return videoLengthBand;
  }

  @JsonProperty("videoLengthBand")
  public void setVideoLengthBand(@Nullable String videoLengthBand) {
    this.videoLengthBand = videoLengthBand;
  }

  public RelatedMaterialUpdate initialRank(@Nullable Integer initialRank) {
    this.initialRank = initialRank;
    return this;
  }

  /**
   * Rank before personalisation.
   * @return initialRank
   */
  
  @JsonProperty("initialRank")
  public @Nullable Integer getInitialRank() {
    return initialRank;
  }

  @JsonProperty("initialRank")
  public void setInitialRank(@Nullable Integer initialRank) {
    this.initialRank = initialRank;
  }

  public RelatedMaterialUpdate interactivityLevel(@Nullable Integer interactivityLevel) {
    this.interactivityLevel = interactivityLevel;
    return this;
  }

  /**
   * Interactivity score.
   * @return interactivityLevel
   */
  
  @JsonProperty("interactivityLevel")
  public @Nullable Integer getInteractivityLevel() {
    return interactivityLevel;
  }

  @JsonProperty("interactivityLevel")
  public void setInteractivityLevel(@Nullable Integer interactivityLevel) {
    this.interactivityLevel = interactivityLevel;
  }

  public RelatedMaterialUpdate interactivityBand(@Nullable String interactivityBand) {
    this.interactivityBand = interactivityBand;
    return this;
  }

  /**
   * Interactivity band.
   * @return interactivityBand
   */
  @Size(max = 40) 
  @JsonProperty("interactivityBand")
  public @Nullable String getInteractivityBand() {
    return interactivityBand;
  }

  @JsonProperty("interactivityBand")
  public void setInteractivityBand(@Nullable String interactivityBand) {
    this.interactivityBand = interactivityBand;
  }

  public RelatedMaterialUpdate conceptualLevel(@Nullable String conceptualLevel) {
    this.conceptualLevel = conceptualLevel;
    return this;
  }

  /**
   * Conceptual level.
   * @return conceptualLevel
   */
  @Size(max = 40) 
  @JsonProperty("conceptualLevel")
  public @Nullable String getConceptualLevel() {
    return conceptualLevel;
  }

  @JsonProperty("conceptualLevel")
  public void setConceptualLevel(@Nullable String conceptualLevel) {
    this.conceptualLevel = conceptualLevel;
  }

  public RelatedMaterialUpdate languageComplexity(@Nullable String languageComplexity) {
    this.languageComplexity = languageComplexity;
    return this;
  }

  /**
   * Language complexity score.
   * @return languageComplexity
   */
  @Pattern(regexp = "^-?\\d+(\\.\\d+)?$") 
  @JsonProperty("languageComplexity")
  public @Nullable String getLanguageComplexity() {
    return languageComplexity;
  }

  @JsonProperty("languageComplexity")
  public void setLanguageComplexity(@Nullable String languageComplexity) {
    this.languageComplexity = languageComplexity;
  }

  public RelatedMaterialUpdate languageComplexityBand(@Nullable String languageComplexityBand) {
    this.languageComplexityBand = languageComplexityBand;
    return this;
  }

  /**
   * Language complexity band.
   * @return languageComplexityBand
   */
  @Size(max = 40) 
  @JsonProperty("languageComplexityBand")
  public @Nullable String getLanguageComplexityBand() {
    return languageComplexityBand;
  }

  @JsonProperty("languageComplexityBand")
  public void setLanguageComplexityBand(@Nullable String languageComplexityBand) {
    this.languageComplexityBand = languageComplexityBand;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    RelatedMaterialUpdate relatedMaterialUpdate = (RelatedMaterialUpdate) o;
    return Objects.equals(this.title, relatedMaterialUpdate.title) &&
        Objects.equals(this.url, relatedMaterialUpdate.url) &&
        Objects.equals(this.summary, relatedMaterialUpdate.summary) &&
        Objects.equals(this.source, relatedMaterialUpdate.source) &&
        Objects.equals(this.mediaType, relatedMaterialUpdate.mediaType) &&
        Objects.equals(this.contentLength, relatedMaterialUpdate.contentLength) &&
        Objects.equals(this.contentLengthBand, relatedMaterialUpdate.contentLengthBand) &&
        Objects.equals(this.videoLengthSeconds, relatedMaterialUpdate.videoLengthSeconds) &&
        Objects.equals(this.videoLengthBand, relatedMaterialUpdate.videoLengthBand) &&
        Objects.equals(this.initialRank, relatedMaterialUpdate.initialRank) &&
        Objects.equals(this.interactivityLevel, relatedMaterialUpdate.interactivityLevel) &&
        Objects.equals(this.interactivityBand, relatedMaterialUpdate.interactivityBand) &&
        Objects.equals(this.conceptualLevel, relatedMaterialUpdate.conceptualLevel) &&
        Objects.equals(this.languageComplexity, relatedMaterialUpdate.languageComplexity) &&
        Objects.equals(this.languageComplexityBand, relatedMaterialUpdate.languageComplexityBand);
  }

  @Override
  public int hashCode() {
    return Objects.hash(title, url, summary, source, mediaType, contentLength, contentLengthBand, videoLengthSeconds, videoLengthBand, initialRank, interactivityLevel, interactivityBand, conceptualLevel, languageComplexity, languageComplexityBand);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class RelatedMaterialUpdate {\n");
    sb.append("    title: ").append(toIndentedString(title)).append("\n");
    sb.append("    url: ").append(toIndentedString(url)).append("\n");
    sb.append("    summary: ").append(toIndentedString(summary)).append("\n");
    sb.append("    source: ").append(toIndentedString(source)).append("\n");
    sb.append("    mediaType: ").append(toIndentedString(mediaType)).append("\n");
    sb.append("    contentLength: ").append(toIndentedString(contentLength)).append("\n");
    sb.append("    contentLengthBand: ").append(toIndentedString(contentLengthBand)).append("\n");
    sb.append("    videoLengthSeconds: ").append(toIndentedString(videoLengthSeconds)).append("\n");
    sb.append("    videoLengthBand: ").append(toIndentedString(videoLengthBand)).append("\n");
    sb.append("    initialRank: ").append(toIndentedString(initialRank)).append("\n");
    sb.append("    interactivityLevel: ").append(toIndentedString(interactivityLevel)).append("\n");
    sb.append("    interactivityBand: ").append(toIndentedString(interactivityBand)).append("\n");
    sb.append("    conceptualLevel: ").append(toIndentedString(conceptualLevel)).append("\n");
    sb.append("    languageComplexity: ").append(toIndentedString(languageComplexity)).append("\n");
    sb.append("    languageComplexityBand: ").append(toIndentedString(languageComplexityBand)).append("\n");
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

