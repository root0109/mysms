// Owned API contract of privacy.
package com.mysms.privacy.api.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.Objects;
import org.springframework.lang.Nullable;

/**
 * Fields of NoticeTranslation that can be updated while its version is a draft (partial update; see updateMask).
 */

public class NoticeTranslationUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String language;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String bodyMarkdown;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String summaryForChildren;

  public NoticeTranslationUpdate language(@Nullable String language) {
    this.language = language;
    return this;
  }

  /**
   * BCP 47 language code: en, hi, ta, mr-IN ...; one per version.
   * @return language
   */
  @Pattern(regexp = "^[a-z]{2,3}(-[A-Z]{2})?$") 
  @JsonProperty("language")
  public @Nullable String getLanguage() {
    return language;
  }

  @JsonProperty("language")
  public void setLanguage(@Nullable String language) {
    this.language = language;
  }

  public NoticeTranslationUpdate bodyMarkdown(@Nullable String bodyMarkdown) {
    this.bodyMarkdown = bodyMarkdown;
    return this;
  }

  /**
   * The notice in Markdown: the data processed, the purposes, how to withdraw and how to complain.
   * @return bodyMarkdown
   */
  @Size(min = 1) 
  @JsonProperty("bodyMarkdown")
  public @Nullable String getBodyMarkdown() {
    return bodyMarkdown;
  }

  @JsonProperty("bodyMarkdown")
  public void setBodyMarkdown(@Nullable String bodyMarkdown) {
    this.bodyMarkdown = bodyMarkdown;
  }

  public NoticeTranslationUpdate summaryForChildren(@Nullable String summaryForChildren) {
    this.summaryForChildren = summaryForChildren;
    return this;
  }

  /**
   * Short plain version shown to a minor alongside the notice.
   * @return summaryForChildren
   */
   
  @JsonProperty("summaryForChildren")
  public @Nullable String getSummaryForChildren() {
    return summaryForChildren;
  }

  @JsonProperty("summaryForChildren")
  public void setSummaryForChildren(@Nullable String summaryForChildren) {
    this.summaryForChildren = summaryForChildren;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    NoticeTranslationUpdate other = (NoticeTranslationUpdate) o;
    return Objects.equals(this.language, other.language) &&
        Objects.equals(this.bodyMarkdown, other.bodyMarkdown) &&
        Objects.equals(this.summaryForChildren, other.summaryForChildren);
  }

  @Override
  public int hashCode() {
    return Objects.hash(language, bodyMarkdown, summaryForChildren);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class NoticeTranslationUpdate {\n");
    sb.append("    language: ").append(toIndentedString(language)).append("\n");
    sb.append("    bodyMarkdown: ").append(toIndentedString(bodyMarkdown)).append("\n");
    sb.append("    summaryForChildren: ").append(toIndentedString(summaryForChildren)).append("\n");
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
