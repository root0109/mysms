// Owned API contract of privacy.
package com.mysms.privacy.api.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.lang.Nullable;

/**
 * A privacy notice as the consent centre shows it: the active version of the tenant's notice in the asked language (English when it has no such translation).
 */

public class MyNotice {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private UUID noticeVersionId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private String noticeCode;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private Integer version;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private String language;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private List<String> purposeCodes = new ArrayList<>();

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private Boolean requiresReconsent;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private GrievanceContact grievanceContact;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private String bodyMarkdown;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String summaryForChildren;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private String contentSha256;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private OffsetDateTime publishedAt;

  public MyNotice() {
    super();
  }

  public MyNotice noticeVersionId(UUID noticeVersionId) {
    this.noticeVersionId = noticeVersionId;
    return this;
  }

  /**
   * The active version shown; send it back when deciding.
   * @return noticeVersionId
   */
  @NotNull @Valid 
  @JsonProperty("noticeVersionId")
  public UUID getNoticeVersionId() {
    return noticeVersionId;
  }

  @JsonProperty("noticeVersionId")
  public void setNoticeVersionId(UUID noticeVersionId) {
    this.noticeVersionId = noticeVersionId;
  }

  public MyNotice noticeCode(String noticeCode) {
    this.noticeCode = noticeCode;
    return this;
  }

  /**
   * Code of the notice within the tenant.
   * @return noticeCode
   */
  @NotNull 
  @JsonProperty("noticeCode")
  public String getNoticeCode() {
    return noticeCode;
  }

  @JsonProperty("noticeCode")
  public void setNoticeCode(String noticeCode) {
    this.noticeCode = noticeCode;
  }

  public MyNotice version(Integer version) {
    this.version = version;
    return this;
  }

  /**
   * Number of the version.
   * @return version
   */
  @NotNull 
  @JsonProperty("version")
  public Integer getVersion() {
    return version;
  }

  @JsonProperty("version")
  public void setVersion(Integer version) {
    this.version = version;
  }

  public MyNotice language(String language) {
    this.language = language;
    return this;
  }

  /**
   * The language of the wording returned (BCP 47); send it back when deciding.
   * @return language
   */
  @NotNull 
  @JsonProperty("language")
  public String getLanguage() {
    return language;
  }

  @JsonProperty("language")
  public void setLanguage(String language) {
    this.language = language;
  }

  public MyNotice purposeCodes(List<String> purposeCodes) {
    this.purposeCodes = purposeCodes;
    return this;
  }

  /**
   * The purposes the notice asks consent for.
   * @return purposeCodes
   */
  @NotNull 
  @JsonProperty("purposeCodes")
  public List<String> getPurposeCodes() {
    return purposeCodes;
  }

  @JsonProperty("purposeCodes")
  public void setPurposeCodes(List<String> purposeCodes) {
    this.purposeCodes = purposeCodes;
  }

  public MyNotice requiresReconsent(Boolean requiresReconsent) {
    this.requiresReconsent = requiresReconsent;
    return this;
  }

  /**
   * Whether consents given under earlier versions must be given again.
   * @return requiresReconsent
   */
  @NotNull 
  @JsonProperty("requiresReconsent")
  public Boolean getRequiresReconsent() {
    return requiresReconsent;
  }

  @JsonProperty("requiresReconsent")
  public void setRequiresReconsent(Boolean requiresReconsent) {
    this.requiresReconsent = requiresReconsent;
  }

  public MyNotice grievanceContact(GrievanceContact grievanceContact) {
    this.grievanceContact = grievanceContact;
    return this;
  }

  /**
   * The school's grievance officer.
   * @return grievanceContact
   */
  @NotNull @Valid 
  @JsonProperty("grievanceContact")
  public GrievanceContact getGrievanceContact() {
    return grievanceContact;
  }

  @JsonProperty("grievanceContact")
  public void setGrievanceContact(GrievanceContact grievanceContact) {
    this.grievanceContact = grievanceContact;
  }

  public MyNotice bodyMarkdown(String bodyMarkdown) {
    this.bodyMarkdown = bodyMarkdown;
    return this;
  }

  /**
   * The notice in Markdown.
   * @return bodyMarkdown
   */
  @NotNull 
  @JsonProperty("bodyMarkdown")
  public String getBodyMarkdown() {
    return bodyMarkdown;
  }

  @JsonProperty("bodyMarkdown")
  public void setBodyMarkdown(String bodyMarkdown) {
    this.bodyMarkdown = bodyMarkdown;
  }

  public MyNotice summaryForChildren(@Nullable String summaryForChildren) {
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

  public MyNotice contentSha256(String contentSha256) {
    this.contentSha256 = contentSha256;
    return this;
  }

  /**
   * Lowercase hex SHA-256 of the body, a blank line and the summary.
   * @return contentSha256
   */
  @NotNull 
  @JsonProperty("contentSha256")
  public String getContentSha256() {
    return contentSha256;
  }

  @JsonProperty("contentSha256")
  public void setContentSha256(String contentSha256) {
    this.contentSha256 = contentSha256;
  }

  public MyNotice publishedAt(OffsetDateTime publishedAt) {
    this.publishedAt = publishedAt;
    return this;
  }

  /**
   * When the version was published.
   * @return publishedAt
   */
  @NotNull @Valid 
  @JsonProperty("publishedAt")
  public OffsetDateTime getPublishedAt() {
    return publishedAt;
  }

  @JsonProperty("publishedAt")
  public void setPublishedAt(OffsetDateTime publishedAt) {
    this.publishedAt = publishedAt;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    MyNotice other = (MyNotice) o;
    return Objects.equals(this.noticeVersionId, other.noticeVersionId) &&
        Objects.equals(this.noticeCode, other.noticeCode) &&
        Objects.equals(this.version, other.version) &&
        Objects.equals(this.language, other.language) &&
        Objects.equals(this.purposeCodes, other.purposeCodes) &&
        Objects.equals(this.requiresReconsent, other.requiresReconsent) &&
        Objects.equals(this.grievanceContact, other.grievanceContact) &&
        Objects.equals(this.bodyMarkdown, other.bodyMarkdown) &&
        Objects.equals(this.summaryForChildren, other.summaryForChildren) &&
        Objects.equals(this.contentSha256, other.contentSha256) &&
        Objects.equals(this.publishedAt, other.publishedAt);
  }

  @Override
  public int hashCode() {
    return Objects.hash(noticeVersionId, noticeCode, version, language, purposeCodes, requiresReconsent, grievanceContact, bodyMarkdown, summaryForChildren, contentSha256, publishedAt);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class MyNotice {\n");
    sb.append("    noticeVersionId: ").append(toIndentedString(noticeVersionId)).append("\n");
    sb.append("    noticeCode: ").append(toIndentedString(noticeCode)).append("\n");
    sb.append("    version: ").append(toIndentedString(version)).append("\n");
    sb.append("    language: ").append(toIndentedString(language)).append("\n");
    sb.append("    purposeCodes: ").append(toIndentedString(purposeCodes)).append("\n");
    sb.append("    requiresReconsent: ").append(toIndentedString(requiresReconsent)).append("\n");
    sb.append("    grievanceContact: ").append(toIndentedString(grievanceContact)).append("\n");
    sb.append("    bodyMarkdown: ").append(toIndentedString(bodyMarkdown)).append("\n");
    sb.append("    summaryForChildren: ").append(toIndentedString(summaryForChildren)).append("\n");
    sb.append("    contentSha256: ").append(toIndentedString(contentSha256)).append("\n");
    sb.append("    publishedAt: ").append(toIndentedString(publishedAt)).append("\n");
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
