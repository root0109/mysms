// Owned API contract of lms.
package com.mysms.learning.lms.api.model;

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
 * Fields of RelatedMaterial that can be updated (PATCH: a property left out keeps its value, null clears it, a value sets it).
 */

public class RelatedMaterialUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull @Size(max = 500) String> title;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull @Size(max = 1000) String> url;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<String> summary;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@Size(max = 100) String> source;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@Size(max = 40) String> mediaType;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@Pattern(regexp = "^-?[0-9]+$") String> contentLength;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@Size(max = 40) String> contentLengthBand;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@Pattern(regexp = "^-?[0-9]+$") String> videoLengthSeconds;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@Size(max = 40) String> videoLengthBand;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<Integer> initialRank;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<Integer> interactivityLevel;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@Size(max = 40) String> interactivityBand;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@Size(max = 40) String> conceptualLevel;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@Pattern(regexp = "^-?\\d+(\\.\\d+)?$") String> languageComplexity;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@Size(max = 40) String> languageComplexityBand;

  public RelatedMaterialUpdate title(@Nullable Optional<String> title) {
    this.title = title;
    return this;
  }

  /**
   * Page title.
   * @return title
   */
  
  @JsonProperty("title")
  public @Nullable Optional<String> getTitle() {
    return title;
  }

  @JsonProperty("title")
  public void setTitle(@Nullable Optional<String> title) {
    this.title = title;
  }

  public RelatedMaterialUpdate url(@Nullable Optional<String> url) {
    this.url = url;
    return this;
  }

  /**
   * Page URL.
   * @return url
   */
  
  @JsonProperty("url")
  public @Nullable Optional<String> getUrl() {
    return url;
  }

  @JsonProperty("url")
  public void setUrl(@Nullable Optional<String> url) {
    this.url = url;
  }

  public RelatedMaterialUpdate summary(@Nullable Optional<String> summary) {
    this.summary = summary;
    return this;
  }

  /**
   * Page description.
   * @return summary
   */
  
  @JsonProperty("summary")
  public @Nullable Optional<String> getSummary() {
    return summary;
  }

  @JsonProperty("summary")
  public void setSummary(@Nullable Optional<String> summary) {
    this.summary = summary;
  }

  public RelatedMaterialUpdate source(@Nullable Optional<String> source) {
    this.source = source;
    return this;
  }

  /**
   * Source (search engine, video site...).
   * @return source
   */
  
  @JsonProperty("source")
  public @Nullable Optional<String> getSource() {
    return source;
  }

  @JsonProperty("source")
  public void setSource(@Nullable Optional<String> source) {
    this.source = source;
  }

  public RelatedMaterialUpdate mediaType(@Nullable Optional<String> mediaType) {
    this.mediaType = mediaType;
    return this;
  }

  /**
   * Media type (article, video...).
   * @return mediaType
   */
  
  @JsonProperty("mediaType")
  public @Nullable Optional<String> getMediaType() {
    return mediaType;
  }

  @JsonProperty("mediaType")
  public void setMediaType(@Nullable Optional<String> mediaType) {
    this.mediaType = mediaType;
  }

  public RelatedMaterialUpdate contentLength(@Nullable Optional<String> contentLength) {
    this.contentLength = contentLength;
    return this;
  }

  /**
   * Length of the text content.
   * @return contentLength
   */
  
  @JsonProperty("contentLength")
  public @Nullable Optional<String> getContentLength() {
    return contentLength;
  }

  @JsonProperty("contentLength")
  public void setContentLength(@Nullable Optional<String> contentLength) {
    this.contentLength = contentLength;
  }

  public RelatedMaterialUpdate contentLengthBand(@Nullable Optional<String> contentLengthBand) {
    this.contentLengthBand = contentLengthBand;
    return this;
  }

  /**
   * Length band (short, medium, long).
   * @return contentLengthBand
   */
  
  @JsonProperty("contentLengthBand")
  public @Nullable Optional<String> getContentLengthBand() {
    return contentLengthBand;
  }

  @JsonProperty("contentLengthBand")
  public void setContentLengthBand(@Nullable Optional<String> contentLengthBand) {
    this.contentLengthBand = contentLengthBand;
  }

  public RelatedMaterialUpdate videoLengthSeconds(@Nullable Optional<String> videoLengthSeconds) {
    this.videoLengthSeconds = videoLengthSeconds;
    return this;
  }

  /**
   * Video length.
   * @return videoLengthSeconds
   */
  
  @JsonProperty("videoLengthSeconds")
  public @Nullable Optional<String> getVideoLengthSeconds() {
    return videoLengthSeconds;
  }

  @JsonProperty("videoLengthSeconds")
  public void setVideoLengthSeconds(@Nullable Optional<String> videoLengthSeconds) {
    this.videoLengthSeconds = videoLengthSeconds;
  }

  public RelatedMaterialUpdate videoLengthBand(@Nullable Optional<String> videoLengthBand) {
    this.videoLengthBand = videoLengthBand;
    return this;
  }

  /**
   * Video length band.
   * @return videoLengthBand
   */
  
  @JsonProperty("videoLengthBand")
  public @Nullable Optional<String> getVideoLengthBand() {
    return videoLengthBand;
  }

  @JsonProperty("videoLengthBand")
  public void setVideoLengthBand(@Nullable Optional<String> videoLengthBand) {
    this.videoLengthBand = videoLengthBand;
  }

  public RelatedMaterialUpdate initialRank(@Nullable Optional<Integer> initialRank) {
    this.initialRank = initialRank;
    return this;
  }

  /**
   * Rank before personalisation.
   * @return initialRank
   */
  
  @JsonProperty("initialRank")
  public @Nullable Optional<Integer> getInitialRank() {
    return initialRank;
  }

  @JsonProperty("initialRank")
  public void setInitialRank(@Nullable Optional<Integer> initialRank) {
    this.initialRank = initialRank;
  }

  public RelatedMaterialUpdate interactivityLevel(@Nullable Optional<Integer> interactivityLevel) {
    this.interactivityLevel = interactivityLevel;
    return this;
  }

  /**
   * Interactivity score.
   * @return interactivityLevel
   */
  
  @JsonProperty("interactivityLevel")
  public @Nullable Optional<Integer> getInteractivityLevel() {
    return interactivityLevel;
  }

  @JsonProperty("interactivityLevel")
  public void setInteractivityLevel(@Nullable Optional<Integer> interactivityLevel) {
    this.interactivityLevel = interactivityLevel;
  }

  public RelatedMaterialUpdate interactivityBand(@Nullable Optional<String> interactivityBand) {
    this.interactivityBand = interactivityBand;
    return this;
  }

  /**
   * Interactivity band.
   * @return interactivityBand
   */
  
  @JsonProperty("interactivityBand")
  public @Nullable Optional<String> getInteractivityBand() {
    return interactivityBand;
  }

  @JsonProperty("interactivityBand")
  public void setInteractivityBand(@Nullable Optional<String> interactivityBand) {
    this.interactivityBand = interactivityBand;
  }

  public RelatedMaterialUpdate conceptualLevel(@Nullable Optional<String> conceptualLevel) {
    this.conceptualLevel = conceptualLevel;
    return this;
  }

  /**
   * Conceptual level.
   * @return conceptualLevel
   */
  
  @JsonProperty("conceptualLevel")
  public @Nullable Optional<String> getConceptualLevel() {
    return conceptualLevel;
  }

  @JsonProperty("conceptualLevel")
  public void setConceptualLevel(@Nullable Optional<String> conceptualLevel) {
    this.conceptualLevel = conceptualLevel;
  }

  public RelatedMaterialUpdate languageComplexity(@Nullable Optional<String> languageComplexity) {
    this.languageComplexity = languageComplexity;
    return this;
  }

  /**
   * Language complexity score.
   * @return languageComplexity
   */
  
  @JsonProperty("languageComplexity")
  public @Nullable Optional<String> getLanguageComplexity() {
    return languageComplexity;
  }

  @JsonProperty("languageComplexity")
  public void setLanguageComplexity(@Nullable Optional<String> languageComplexity) {
    this.languageComplexity = languageComplexity;
  }

  public RelatedMaterialUpdate languageComplexityBand(@Nullable Optional<String> languageComplexityBand) {
    this.languageComplexityBand = languageComplexityBand;
    return this;
  }

  /**
   * Language complexity band.
   * @return languageComplexityBand
   */
  
  @JsonProperty("languageComplexityBand")
  public @Nullable Optional<String> getLanguageComplexityBand() {
    return languageComplexityBand;
  }

  @JsonProperty("languageComplexityBand")
  public void setLanguageComplexityBand(@Nullable Optional<String> languageComplexityBand) {
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

