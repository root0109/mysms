// Owned API contract of lms.
package com.mysms.learning.lms.api.model;

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
 * Fields of Chapter that can be updated (PATCH: a property left out keeps its value, null clears it, a value sets it).
 */

public class ChapterUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull @Size(max = 200) String> title;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull Integer> sortOrder;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull Boolean> isScormPackage;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<UUID> scormPackageFileId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<String> scormPackagePath;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<String> scormManifestFile;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<String> scormLaunchFile;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<UUID> cohortId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<UUID> parentChapterId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<String> description;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<Integer> chapterNumber;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<Integer> startDay;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@Pattern(regexp = "^\\d{2}:\\d{2}(:\\d{2})?$") String> startTime;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<Integer> dueDay;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@Pattern(regexp = "^\\d{2}:\\d{2}(:\\d{2})?$") String> dueTime;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private @Nullable Optional<OffsetDateTime> startsAt;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private @Nullable Optional<OffsetDateTime> endsAt;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull Boolean> allowDelete;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull Boolean> allowEditDetails;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull Boolean> allowEditDates;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<UUID> copiedFromId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull Boolean> isOriginal;

  public ChapterUpdate title(@Nullable Optional<String> title) {
    this.title = title;
    return this;
  }

  /**
   * Chapter title.
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

  public ChapterUpdate sortOrder(@Nullable Optional<Integer> sortOrder) {
    this.sortOrder = sortOrder;
    return this;
  }

  /**
   * Position in the course.
   * @return sortOrder
   */
  
  @JsonProperty("sortOrder")
  public @Nullable Optional<Integer> getSortOrder() {
    return sortOrder;
  }

  @JsonProperty("sortOrder")
  public void setSortOrder(@Nullable Optional<Integer> sortOrder) {
    this.sortOrder = sortOrder;
  }

  public ChapterUpdate isScormPackage(@Nullable Optional<Boolean> isScormPackage) {
    this.isScormPackage = isScormPackage;
    return this;
  }

  /**
   * Chapter is delivered as a SCORM package.
   * @return isScormPackage
   */
  
  @JsonProperty("isScormPackage")
  public @Nullable Optional<Boolean> getIsScormPackage() {
    return isScormPackage;
  }

  @JsonProperty("isScormPackage")
  public void setIsScormPackage(@Nullable Optional<Boolean> isScormPackage) {
    this.isScormPackage = isScormPackage;
  }

  public ChapterUpdate scormPackageFileId(@Nullable Optional<UUID> scormPackageFileId) {
    this.scormPackageFileId = scormPackageFileId;
    return this;
  }

  /**
   * Uploaded SCORM package.
   * @return scormPackageFileId
   */
  
  @JsonProperty("scormPackageFileId")
  public @Nullable Optional<UUID> getScormPackageFileId() {
    return scormPackageFileId;
  }

  @JsonProperty("scormPackageFileId")
  public void setScormPackageFileId(@Nullable Optional<UUID> scormPackageFileId) {
    this.scormPackageFileId = scormPackageFileId;
  }

  public ChapterUpdate scormPackagePath(@Nullable Optional<String> scormPackagePath) {
    this.scormPackagePath = scormPackagePath;
    return this;
  }

  /**
   * Extracted package path.
   * @return scormPackagePath
   */
  
  @JsonProperty("scormPackagePath")
  public @Nullable Optional<String> getScormPackagePath() {
    return scormPackagePath;
  }

  @JsonProperty("scormPackagePath")
  public void setScormPackagePath(@Nullable Optional<String> scormPackagePath) {
    this.scormPackagePath = scormPackagePath;
  }

  public ChapterUpdate scormManifestFile(@Nullable Optional<String> scormManifestFile) {
    this.scormManifestFile = scormManifestFile;
    return this;
  }

  /**
   * Manifest file path inside the package.
   * @return scormManifestFile
   */
  
  @JsonProperty("scormManifestFile")
  public @Nullable Optional<String> getScormManifestFile() {
    return scormManifestFile;
  }

  @JsonProperty("scormManifestFile")
  public void setScormManifestFile(@Nullable Optional<String> scormManifestFile) {
    this.scormManifestFile = scormManifestFile;
  }

  public ChapterUpdate scormLaunchFile(@Nullable Optional<String> scormLaunchFile) {
    this.scormLaunchFile = scormLaunchFile;
    return this;
  }

  /**
   * Launch file path inside the package.
   * @return scormLaunchFile
   */
  
  @JsonProperty("scormLaunchFile")
  public @Nullable Optional<String> getScormLaunchFile() {
    return scormLaunchFile;
  }

  @JsonProperty("scormLaunchFile")
  public void setScormLaunchFile(@Nullable Optional<String> scormLaunchFile) {
    this.scormLaunchFile = scormLaunchFile;
  }

  public ChapterUpdate cohortId(@Nullable Optional<UUID> cohortId) {
    this.cohortId = cohortId;
    return this;
  }

  /**
   * Cohort copy of the chapter (NULL = course template chapter).
   * @return cohortId
   */
  
  @JsonProperty("cohortId")
  public @Nullable Optional<UUID> getCohortId() {
    return cohortId;
  }

  @JsonProperty("cohortId")
  public void setCohortId(@Nullable Optional<UUID> cohortId) {
    this.cohortId = cohortId;
  }

  public ChapterUpdate parentChapterId(@Nullable Optional<UUID> parentChapterId) {
    this.parentChapterId = parentChapterId;
    return this;
  }

  /**
   * Parent chapter (nested folders).
   * @return parentChapterId
   */
  
  @JsonProperty("parentChapterId")
  public @Nullable Optional<UUID> getParentChapterId() {
    return parentChapterId;
  }

  @JsonProperty("parentChapterId")
  public void setParentChapterId(@Nullable Optional<UUID> parentChapterId) {
    this.parentChapterId = parentChapterId;
  }

  public ChapterUpdate description(@Nullable Optional<String> description) {
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

  public ChapterUpdate chapterNumber(@Nullable Optional<Integer> chapterNumber) {
    this.chapterNumber = chapterNumber;
    return this;
  }

  /**
   * Display number.
   * @return chapterNumber
   */
  
  @JsonProperty("chapterNumber")
  public @Nullable Optional<Integer> getChapterNumber() {
    return chapterNumber;
  }

  @JsonProperty("chapterNumber")
  public void setChapterNumber(@Nullable Optional<Integer> chapterNumber) {
    this.chapterNumber = chapterNumber;
  }

  public ChapterUpdate startDay(@Nullable Optional<Integer> startDay) {
    this.startDay = startDay;
    return this;
  }

  /**
   * Template: opens on day N of the cohort.
   * @return startDay
   */
  
  @JsonProperty("startDay")
  public @Nullable Optional<Integer> getStartDay() {
    return startDay;
  }

  @JsonProperty("startDay")
  public void setStartDay(@Nullable Optional<Integer> startDay) {
    this.startDay = startDay;
  }

  public ChapterUpdate startTime(@Nullable Optional<String> startTime) {
    this.startTime = startTime;
    return this;
  }

  /**
   * Template: time on the start day.
   * @return startTime
   */
  
  @JsonProperty("startTime")
  public @Nullable Optional<String> getStartTime() {
    return startTime;
  }

  @JsonProperty("startTime")
  public void setStartTime(@Nullable Optional<String> startTime) {
    this.startTime = startTime;
  }

  public ChapterUpdate dueDay(@Nullable Optional<Integer> dueDay) {
    this.dueDay = dueDay;
    return this;
  }

  /**
   * Template: due on day N of the cohort.
   * @return dueDay
   */
  
  @JsonProperty("dueDay")
  public @Nullable Optional<Integer> getDueDay() {
    return dueDay;
  }

  @JsonProperty("dueDay")
  public void setDueDay(@Nullable Optional<Integer> dueDay) {
    this.dueDay = dueDay;
  }

  public ChapterUpdate dueTime(@Nullable Optional<String> dueTime) {
    this.dueTime = dueTime;
    return this;
  }

  /**
   * Template: time on the due day.
   * @return dueTime
   */
  
  @JsonProperty("dueTime")
  public @Nullable Optional<String> getDueTime() {
    return dueTime;
  }

  @JsonProperty("dueTime")
  public void setDueTime(@Nullable Optional<String> dueTime) {
    this.dueTime = dueTime;
  }

  public ChapterUpdate startsAt(@Nullable Optional<OffsetDateTime> startsAt) {
    this.startsAt = startsAt;
    return this;
  }

  /**
   * Cohort: opens.
   * @return startsAt
   */
  
  @JsonProperty("startsAt")
  public @Nullable Optional<OffsetDateTime> getStartsAt() {
    return startsAt;
  }

  @JsonProperty("startsAt")
  public void setStartsAt(@Nullable Optional<OffsetDateTime> startsAt) {
    this.startsAt = startsAt;
  }

  public ChapterUpdate endsAt(@Nullable Optional<OffsetDateTime> endsAt) {
    this.endsAt = endsAt;
    return this;
  }

  /**
   * Cohort: due / closes.
   * @return endsAt
   */
  
  @JsonProperty("endsAt")
  public @Nullable Optional<OffsetDateTime> getEndsAt() {
    return endsAt;
  }

  @JsonProperty("endsAt")
  public void setEndsAt(@Nullable Optional<OffsetDateTime> endsAt) {
    this.endsAt = endsAt;
  }

  public ChapterUpdate allowDelete(@Nullable Optional<Boolean> allowDelete) {
    this.allowDelete = allowDelete;
    return this;
  }

  /**
   * Cohort teachers may remove the chapter.
   * @return allowDelete
   */
  
  @JsonProperty("allowDelete")
  public @Nullable Optional<Boolean> getAllowDelete() {
    return allowDelete;
  }

  @JsonProperty("allowDelete")
  public void setAllowDelete(@Nullable Optional<Boolean> allowDelete) {
    this.allowDelete = allowDelete;
  }

  public ChapterUpdate allowEditDetails(@Nullable Optional<Boolean> allowEditDetails) {
    this.allowEditDetails = allowEditDetails;
    return this;
  }

  /**
   * Cohort teachers may edit details.
   * @return allowEditDetails
   */
  
  @JsonProperty("allowEditDetails")
  public @Nullable Optional<Boolean> getAllowEditDetails() {
    return allowEditDetails;
  }

  @JsonProperty("allowEditDetails")
  public void setAllowEditDetails(@Nullable Optional<Boolean> allowEditDetails) {
    this.allowEditDetails = allowEditDetails;
  }

  public ChapterUpdate allowEditDates(@Nullable Optional<Boolean> allowEditDates) {
    this.allowEditDates = allowEditDates;
    return this;
  }

  /**
   * Cohort teachers may change dates.
   * @return allowEditDates
   */
  
  @JsonProperty("allowEditDates")
  public @Nullable Optional<Boolean> getAllowEditDates() {
    return allowEditDates;
  }

  @JsonProperty("allowEditDates")
  public void setAllowEditDates(@Nullable Optional<Boolean> allowEditDates) {
    this.allowEditDates = allowEditDates;
  }

  public ChapterUpdate copiedFromId(@Nullable Optional<UUID> copiedFromId) {
    this.copiedFromId = copiedFromId;
    return this;
  }

  /**
   * Template chapter this cohort copy was made from.
   * @return copiedFromId
   */
  
  @JsonProperty("copiedFromId")
  public @Nullable Optional<UUID> getCopiedFromId() {
    return copiedFromId;
  }

  @JsonProperty("copiedFromId")
  public void setCopiedFromId(@Nullable Optional<UUID> copiedFromId) {
    this.copiedFromId = copiedFromId;
  }

  public ChapterUpdate isOriginal(@Nullable Optional<Boolean> isOriginal) {
    this.isOriginal = isOriginal;
    return this;
  }

  /**
   * Created in this course/cohort (not copied).
   * @return isOriginal
   */
  
  @JsonProperty("isOriginal")
  public @Nullable Optional<Boolean> getIsOriginal() {
    return isOriginal;
  }

  @JsonProperty("isOriginal")
  public void setIsOriginal(@Nullable Optional<Boolean> isOriginal) {
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

