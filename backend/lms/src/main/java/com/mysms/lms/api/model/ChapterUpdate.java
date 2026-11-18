// Owned API contract of lms.
package com.mysms.lms.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.lang.Nullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;

/**
 * Fields of Chapter that can be updated (partial update; see updateMask).
 */

public class ChapterUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String title;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Integer sortOrder;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Boolean isScormPackage;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID scormPackageFileId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String scormPackagePath;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String scormManifestFile;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String scormLaunchFile;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID cohortId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID parentChapterId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String description;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Integer chapterNumber;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Integer startDay;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String startTime;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Integer dueDay;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String dueTime;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private @Nullable OffsetDateTime startsAt;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private @Nullable OffsetDateTime endsAt;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Boolean allowDelete;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Boolean allowEditDetails;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Boolean allowEditDates;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID copiedFromId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Boolean isOriginal;

  public ChapterUpdate title(@Nullable String title) {
    this.title = title;
    return this;
  }

  /**
   * Chapter title.
   * @return title
   */
  @Size(max = 200) 
  @JsonProperty("title")
  public @Nullable String getTitle() {
    return title;
  }

  @JsonProperty("title")
  public void setTitle(@Nullable String title) {
    this.title = title;
  }

  public ChapterUpdate sortOrder(@Nullable Integer sortOrder) {
    this.sortOrder = sortOrder;
    return this;
  }

  /**
   * Position in the course.
   * @return sortOrder
   */
  
  @JsonProperty("sortOrder")
  public @Nullable Integer getSortOrder() {
    return sortOrder;
  }

  @JsonProperty("sortOrder")
  public void setSortOrder(@Nullable Integer sortOrder) {
    this.sortOrder = sortOrder;
  }

  public ChapterUpdate isScormPackage(@Nullable Boolean isScormPackage) {
    this.isScormPackage = isScormPackage;
    return this;
  }

  /**
   * Chapter is delivered as a SCORM package.
   * @return isScormPackage
   */
  
  @JsonProperty("isScormPackage")
  public @Nullable Boolean getIsScormPackage() {
    return isScormPackage;
  }

  @JsonProperty("isScormPackage")
  public void setIsScormPackage(@Nullable Boolean isScormPackage) {
    this.isScormPackage = isScormPackage;
  }

  public ChapterUpdate scormPackageFileId(@Nullable UUID scormPackageFileId) {
    this.scormPackageFileId = scormPackageFileId;
    return this;
  }

  /**
   * Uploaded SCORM package.
   * @return scormPackageFileId
   */
  @Valid 
  @JsonProperty("scormPackageFileId")
  public @Nullable UUID getScormPackageFileId() {
    return scormPackageFileId;
  }

  @JsonProperty("scormPackageFileId")
  public void setScormPackageFileId(@Nullable UUID scormPackageFileId) {
    this.scormPackageFileId = scormPackageFileId;
  }

  public ChapterUpdate scormPackagePath(@Nullable String scormPackagePath) {
    this.scormPackagePath = scormPackagePath;
    return this;
  }

  /**
   * Extracted package path.
   * @return scormPackagePath
   */
  
  @JsonProperty("scormPackagePath")
  public @Nullable String getScormPackagePath() {
    return scormPackagePath;
  }

  @JsonProperty("scormPackagePath")
  public void setScormPackagePath(@Nullable String scormPackagePath) {
    this.scormPackagePath = scormPackagePath;
  }

  public ChapterUpdate scormManifestFile(@Nullable String scormManifestFile) {
    this.scormManifestFile = scormManifestFile;
    return this;
  }

  /**
   * Manifest file path inside the package.
   * @return scormManifestFile
   */
  
  @JsonProperty("scormManifestFile")
  public @Nullable String getScormManifestFile() {
    return scormManifestFile;
  }

  @JsonProperty("scormManifestFile")
  public void setScormManifestFile(@Nullable String scormManifestFile) {
    this.scormManifestFile = scormManifestFile;
  }

  public ChapterUpdate scormLaunchFile(@Nullable String scormLaunchFile) {
    this.scormLaunchFile = scormLaunchFile;
    return this;
  }

  /**
   * Launch file path inside the package.
   * @return scormLaunchFile
   */
  
  @JsonProperty("scormLaunchFile")
  public @Nullable String getScormLaunchFile() {
    return scormLaunchFile;
  }

  @JsonProperty("scormLaunchFile")
  public void setScormLaunchFile(@Nullable String scormLaunchFile) {
    this.scormLaunchFile = scormLaunchFile;
  }

  public ChapterUpdate cohortId(@Nullable UUID cohortId) {
    this.cohortId = cohortId;
    return this;
  }

  /**
   * Cohort copy of the chapter (NULL = course template chapter).
   * @return cohortId
   */
  @Valid 
  @JsonProperty("cohortId")
  public @Nullable UUID getCohortId() {
    return cohortId;
  }

  @JsonProperty("cohortId")
  public void setCohortId(@Nullable UUID cohortId) {
    this.cohortId = cohortId;
  }

  public ChapterUpdate parentChapterId(@Nullable UUID parentChapterId) {
    this.parentChapterId = parentChapterId;
    return this;
  }

  /**
   * Parent chapter (nested folders).
   * @return parentChapterId
   */
  @Valid 
  @JsonProperty("parentChapterId")
  public @Nullable UUID getParentChapterId() {
    return parentChapterId;
  }

  @JsonProperty("parentChapterId")
  public void setParentChapterId(@Nullable UUID parentChapterId) {
    this.parentChapterId = parentChapterId;
  }

  public ChapterUpdate description(@Nullable String description) {
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

  public ChapterUpdate chapterNumber(@Nullable Integer chapterNumber) {
    this.chapterNumber = chapterNumber;
    return this;
  }

  /**
   * Display number.
   * @return chapterNumber
   */
  
  @JsonProperty("chapterNumber")
  public @Nullable Integer getChapterNumber() {
    return chapterNumber;
  }

  @JsonProperty("chapterNumber")
  public void setChapterNumber(@Nullable Integer chapterNumber) {
    this.chapterNumber = chapterNumber;
  }

  public ChapterUpdate startDay(@Nullable Integer startDay) {
    this.startDay = startDay;
    return this;
  }

  /**
   * Template: opens on day N of the cohort.
   * @return startDay
   */
  
  @JsonProperty("startDay")
  public @Nullable Integer getStartDay() {
    return startDay;
  }

  @JsonProperty("startDay")
  public void setStartDay(@Nullable Integer startDay) {
    this.startDay = startDay;
  }

  public ChapterUpdate startTime(@Nullable String startTime) {
    this.startTime = startTime;
    return this;
  }

  /**
   * Template: time on the start day.
   * @return startTime
   */
  @Pattern(regexp = "^\\d{2}:\\d{2}(:\\d{2})?$") 
  @JsonProperty("startTime")
  public @Nullable String getStartTime() {
    return startTime;
  }

  @JsonProperty("startTime")
  public void setStartTime(@Nullable String startTime) {
    this.startTime = startTime;
  }

  public ChapterUpdate dueDay(@Nullable Integer dueDay) {
    this.dueDay = dueDay;
    return this;
  }

  /**
   * Template: due on day N of the cohort.
   * @return dueDay
   */
  
  @JsonProperty("dueDay")
  public @Nullable Integer getDueDay() {
    return dueDay;
  }

  @JsonProperty("dueDay")
  public void setDueDay(@Nullable Integer dueDay) {
    this.dueDay = dueDay;
  }

  public ChapterUpdate dueTime(@Nullable String dueTime) {
    this.dueTime = dueTime;
    return this;
  }

  /**
   * Template: time on the due day.
   * @return dueTime
   */
  @Pattern(regexp = "^\\d{2}:\\d{2}(:\\d{2})?$") 
  @JsonProperty("dueTime")
  public @Nullable String getDueTime() {
    return dueTime;
  }

  @JsonProperty("dueTime")
  public void setDueTime(@Nullable String dueTime) {
    this.dueTime = dueTime;
  }

  public ChapterUpdate startsAt(@Nullable OffsetDateTime startsAt) {
    this.startsAt = startsAt;
    return this;
  }

  /**
   * Cohort: opens.
   * @return startsAt
   */
  @Valid 
  @JsonProperty("startsAt")
  public @Nullable OffsetDateTime getStartsAt() {
    return startsAt;
  }

  @JsonProperty("startsAt")
  public void setStartsAt(@Nullable OffsetDateTime startsAt) {
    this.startsAt = startsAt;
  }

  public ChapterUpdate endsAt(@Nullable OffsetDateTime endsAt) {
    this.endsAt = endsAt;
    return this;
  }

  /**
   * Cohort: due / closes.
   * @return endsAt
   */
  @Valid 
  @JsonProperty("endsAt")
  public @Nullable OffsetDateTime getEndsAt() {
    return endsAt;
  }

  @JsonProperty("endsAt")
  public void setEndsAt(@Nullable OffsetDateTime endsAt) {
    this.endsAt = endsAt;
  }

  public ChapterUpdate allowDelete(@Nullable Boolean allowDelete) {
    this.allowDelete = allowDelete;
    return this;
  }

  /**
   * Cohort teachers may remove the chapter.
   * @return allowDelete
   */
  
  @JsonProperty("allowDelete")
  public @Nullable Boolean getAllowDelete() {
    return allowDelete;
  }

  @JsonProperty("allowDelete")
  public void setAllowDelete(@Nullable Boolean allowDelete) {
    this.allowDelete = allowDelete;
  }

  public ChapterUpdate allowEditDetails(@Nullable Boolean allowEditDetails) {
    this.allowEditDetails = allowEditDetails;
    return this;
  }

  /**
   * Cohort teachers may edit details.
   * @return allowEditDetails
   */
  
  @JsonProperty("allowEditDetails")
  public @Nullable Boolean getAllowEditDetails() {
    return allowEditDetails;
  }

  @JsonProperty("allowEditDetails")
  public void setAllowEditDetails(@Nullable Boolean allowEditDetails) {
    this.allowEditDetails = allowEditDetails;
  }

  public ChapterUpdate allowEditDates(@Nullable Boolean allowEditDates) {
    this.allowEditDates = allowEditDates;
    return this;
  }

  /**
   * Cohort teachers may change dates.
   * @return allowEditDates
   */
  
  @JsonProperty("allowEditDates")
  public @Nullable Boolean getAllowEditDates() {
    return allowEditDates;
  }

  @JsonProperty("allowEditDates")
  public void setAllowEditDates(@Nullable Boolean allowEditDates) {
    this.allowEditDates = allowEditDates;
  }

  public ChapterUpdate copiedFromId(@Nullable UUID copiedFromId) {
    this.copiedFromId = copiedFromId;
    return this;
  }

  /**
   * Template chapter this cohort copy was made from.
   * @return copiedFromId
   */
  @Valid 
  @JsonProperty("copiedFromId")
  public @Nullable UUID getCopiedFromId() {
    return copiedFromId;
  }

  @JsonProperty("copiedFromId")
  public void setCopiedFromId(@Nullable UUID copiedFromId) {
    this.copiedFromId = copiedFromId;
  }

  public ChapterUpdate isOriginal(@Nullable Boolean isOriginal) {
    this.isOriginal = isOriginal;
    return this;
  }

  /**
   * Created in this course/cohort (not copied).
   * @return isOriginal
   */
  
  @JsonProperty("isOriginal")
  public @Nullable Boolean getIsOriginal() {
    return isOriginal;
  }

  @JsonProperty("isOriginal")
  public void setIsOriginal(@Nullable Boolean isOriginal) {
    this.isOriginal = isOriginal;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ChapterUpdate chapterUpdate = (ChapterUpdate) o;
    return Objects.equals(this.title, chapterUpdate.title) &&
        Objects.equals(this.sortOrder, chapterUpdate.sortOrder) &&
        Objects.equals(this.isScormPackage, chapterUpdate.isScormPackage) &&
        Objects.equals(this.scormPackageFileId, chapterUpdate.scormPackageFileId) &&
        Objects.equals(this.scormPackagePath, chapterUpdate.scormPackagePath) &&
        Objects.equals(this.scormManifestFile, chapterUpdate.scormManifestFile) &&
        Objects.equals(this.scormLaunchFile, chapterUpdate.scormLaunchFile) &&
        Objects.equals(this.cohortId, chapterUpdate.cohortId) &&
        Objects.equals(this.parentChapterId, chapterUpdate.parentChapterId) &&
        Objects.equals(this.description, chapterUpdate.description) &&
        Objects.equals(this.chapterNumber, chapterUpdate.chapterNumber) &&
        Objects.equals(this.startDay, chapterUpdate.startDay) &&
        Objects.equals(this.startTime, chapterUpdate.startTime) &&
        Objects.equals(this.dueDay, chapterUpdate.dueDay) &&
        Objects.equals(this.dueTime, chapterUpdate.dueTime) &&
        Objects.equals(this.startsAt, chapterUpdate.startsAt) &&
        Objects.equals(this.endsAt, chapterUpdate.endsAt) &&
        Objects.equals(this.allowDelete, chapterUpdate.allowDelete) &&
        Objects.equals(this.allowEditDetails, chapterUpdate.allowEditDetails) &&
        Objects.equals(this.allowEditDates, chapterUpdate.allowEditDates) &&
        Objects.equals(this.copiedFromId, chapterUpdate.copiedFromId) &&
        Objects.equals(this.isOriginal, chapterUpdate.isOriginal);
  }

  @Override
  public int hashCode() {
    return Objects.hash(title, sortOrder, isScormPackage, scormPackageFileId, scormPackagePath, scormManifestFile, scormLaunchFile, cohortId, parentChapterId, description, chapterNumber, startDay, startTime, dueDay, dueTime, startsAt, endsAt, allowDelete, allowEditDetails, allowEditDates, copiedFromId, isOriginal);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ChapterUpdate {\n");
    sb.append("    title: ").append(toIndentedString(title)).append("\n");
    sb.append("    sortOrder: ").append(toIndentedString(sortOrder)).append("\n");
    sb.append("    isScormPackage: ").append(toIndentedString(isScormPackage)).append("\n");
    sb.append("    scormPackageFileId: ").append(toIndentedString(scormPackageFileId)).append("\n");
    sb.append("    scormPackagePath: ").append(toIndentedString(scormPackagePath)).append("\n");
    sb.append("    scormManifestFile: ").append(toIndentedString(scormManifestFile)).append("\n");
    sb.append("    scormLaunchFile: ").append(toIndentedString(scormLaunchFile)).append("\n");
    sb.append("    cohortId: ").append(toIndentedString(cohortId)).append("\n");
    sb.append("    parentChapterId: ").append(toIndentedString(parentChapterId)).append("\n");
    sb.append("    description: ").append(toIndentedString(description)).append("\n");
    sb.append("    chapterNumber: ").append(toIndentedString(chapterNumber)).append("\n");
    sb.append("    startDay: ").append(toIndentedString(startDay)).append("\n");
    sb.append("    startTime: ").append(toIndentedString(startTime)).append("\n");
    sb.append("    dueDay: ").append(toIndentedString(dueDay)).append("\n");
    sb.append("    dueTime: ").append(toIndentedString(dueTime)).append("\n");
    sb.append("    startsAt: ").append(toIndentedString(startsAt)).append("\n");
    sb.append("    endsAt: ").append(toIndentedString(endsAt)).append("\n");
    sb.append("    allowDelete: ").append(toIndentedString(allowDelete)).append("\n");
    sb.append("    allowEditDetails: ").append(toIndentedString(allowEditDetails)).append("\n");
    sb.append("    allowEditDates: ").append(toIndentedString(allowEditDates)).append("\n");
    sb.append("    copiedFromId: ").append(toIndentedString(copiedFromId)).append("\n");
    sb.append("    isOriginal: ").append(toIndentedString(isOriginal)).append("\n");
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

