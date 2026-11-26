// Owned API contract of privacy.
package com.mysms.privacy.api.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.Objects;
import java.util.Optional;
import org.springframework.lang.Nullable;

/**
 * Fields of NoticeTranslation that can be updated while its version is a draft (PATCH: a property left out keeps its value, null clears it, a value sets it).
 */

public class NoticeTranslationUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull @Pattern(regexp = "^[a-z]{2,3}(-[A-Z]{2})?$") String> language;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull @Size(min = 1) String> bodyMarkdown;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<String> summaryForChildren;

  public NoticeTranslationUpdate language(@Nullable Optional<String> language) {
    this.language = language;
    return this;
  }

  /**
   * BCP 47 language code: en, hi, ta, mr-IN ...; one per version.
   * @return language
   */
  
  @JsonProperty("language")
  public @Nullable Optional<String> getLanguage() {
    return language;
  }

  @JsonProperty("language")
  public void setLanguage(@Nullable Optional<String> language) {
    this.language = language;
  }

  public NoticeTranslationUpdate bodyMarkdown(@Nullable Optional<String> bodyMarkdown) {
    this.bodyMarkdown = bodyMarkdown;
    return this;
  }

  /**
   * The notice in Markdown: the data processed, the purposes, how to withdraw and how to complain.
   * @return bodyMarkdown
   */
  
  @JsonProperty("bodyMarkdown")
  public @Nullable Optional<String> getBodyMarkdown() {
    return bodyMarkdown;
  }

  @JsonProperty("bodyMarkdown")
  public void setBodyMarkdown(@Nullable Optional<String> bodyMarkdown) {
    this.bodyMarkdown = bodyMarkdown;
  }

  public NoticeTranslationUpdate summaryForChildren(@Nullable Optional<String> summaryForChildren) {
    this.summaryForChildren = summaryForChildren;
    return this;
  }

  /**
   * Short plain version shown to a minor alongside the notice.
   * @return summaryForChildren
   */
  
  @JsonProperty("summaryForChildren")
  public @Nullable Optional<String> getSummaryForChildren() {
    return summaryForChildren;
  }

  @JsonProperty("summaryForChildren")
  public void setSummaryForChildren(@Nullable Optional<String> summaryForChildren) {
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
