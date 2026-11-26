// Owned API contract of academics.
package com.mysms.academics.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.lang.Nullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;

/**
 * Fields of LessonPlan that can be updated (PATCH: a property left out keeps its value, null clears it, a value sets it).
 */

public class LessonPlanUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull UUID> teachingAssignmentId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<UUID> curriculumUnitId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull @Size(max = 20) String> unitNo;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull @Size(max = 200) String> unitName;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull @Size(max = 200) String> title;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull @Pattern(regexp = "^-?\\d+(\\.\\d+)?s$") String> plannedDurationMinutes;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull String> description;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<String> objective;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<String> materialsRequired;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<String> introduction;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<String> procedureText;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<String> conclusion;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<String> assessmentText;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<String> modification;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<String> notes;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  private @Nullable Optional<LocalDate> startDate;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  private @Nullable Optional<LocalDate> endDate;

  public LessonPlanUpdate teachingAssignmentId(@Nullable Optional<UUID> teachingAssignmentId) {
    this.teachingAssignmentId = teachingAssignmentId;
    return this;
  }

  /**
   * Teaching assignment it belongs to.
   * @return teachingAssignmentId
   */
  
  @JsonProperty("teachingAssignmentId")
  public @Nullable Optional<UUID> getTeachingAssignmentId() {
    return teachingAssignmentId;
  }

  @JsonProperty("teachingAssignmentId")
  public void setTeachingAssignmentId(@Nullable Optional<UUID> teachingAssignmentId) {
    this.teachingAssignmentId = teachingAssignmentId;
  }

  public LessonPlanUpdate curriculumUnitId(@Nullable Optional<UUID> curriculumUnitId) {
    this.curriculumUnitId = curriculumUnitId;
    return this;
  }

  /**
   * Unit covered (when linked to the syllabus).
   * @return curriculumUnitId
   */
  
  @JsonProperty("curriculumUnitId")
  public @Nullable Optional<UUID> getCurriculumUnitId() {
    return curriculumUnitId;
  }

  @JsonProperty("curriculumUnitId")
  public void setCurriculumUnitId(@Nullable Optional<UUID> curriculumUnitId) {
    this.curriculumUnitId = curriculumUnitId;
  }

  public LessonPlanUpdate unitNo(@Nullable Optional<String> unitNo) {
    this.unitNo = unitNo;
    return this;
  }

  /**
   * Unit number.
   * @return unitNo
   */
  
  @JsonProperty("unitNo")
  public @Nullable Optional<String> getUnitNo() {
    return unitNo;
  }

  @JsonProperty("unitNo")
  public void setUnitNo(@Nullable Optional<String> unitNo) {
    this.unitNo = unitNo;
  }

  public LessonPlanUpdate unitName(@Nullable Optional<String> unitName) {
    this.unitName = unitName;
    return this;
  }

  /**
   * Unit name.
   * @return unitName
   */
  
  @JsonProperty("unitName")
  public @Nullable Optional<String> getUnitName() {
    return unitName;
  }

  @JsonProperty("unitName")
  public void setUnitName(@Nullable Optional<String> unitName) {
    this.unitName = unitName;
  }

  public LessonPlanUpdate title(@Nullable Optional<String> title) {
    this.title = title;
    return this;
  }

  /**
   * Plan title.
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

  public LessonPlanUpdate plannedDurationMinutes(@Nullable Optional<String> plannedDurationMinutes) {
    this.plannedDurationMinutes = plannedDurationMinutes;
    return this;
  }

  /**
   * Planned teaching time.
   * @return plannedDurationMinutes
   */
  
  @JsonProperty("plannedDurationMinutes")
  public @Nullable Optional<String> getPlannedDurationMinutes() {
    return plannedDurationMinutes;
  }

  @JsonProperty("plannedDurationMinutes")
  public void setPlannedDurationMinutes(@Nullable Optional<String> plannedDurationMinutes) {
    this.plannedDurationMinutes = plannedDurationMinutes;
  }

  public LessonPlanUpdate description(@Nullable Optional<String> description) {
    this.description = description;
    return this;
  }

  /**
   * Summary.
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

  public LessonPlanUpdate objective(@Nullable Optional<String> objective) {
    this.objective = objective;
    return this;
  }

  /**
   * Learning objectives.
   * @return objective
   */
  
  @JsonProperty("objective")
  public @Nullable Optional<String> getObjective() {
    return objective;
  }

  @JsonProperty("objective")
  public void setObjective(@Nullable Optional<String> objective) {
    this.objective = objective;
  }

  public LessonPlanUpdate materialsRequired(@Nullable Optional<String> materialsRequired) {
    this.materialsRequired = materialsRequired;
    return this;
  }

  /**
   * Materials.
   * @return materialsRequired
   */
  
  @JsonProperty("materialsRequired")
  public @Nullable Optional<String> getMaterialsRequired() {
    return materialsRequired;
  }

  @JsonProperty("materialsRequired")
  public void setMaterialsRequired(@Nullable Optional<String> materialsRequired) {
    this.materialsRequired = materialsRequired;
  }

  public LessonPlanUpdate introduction(@Nullable Optional<String> introduction) {
    this.introduction = introduction;
    return this;
  }

  /**
   * Introduction.
   * @return introduction
   */
  
  @JsonProperty("introduction")
  public @Nullable Optional<String> getIntroduction() {
    return introduction;
  }

  @JsonProperty("introduction")
  public void setIntroduction(@Nullable Optional<String> introduction) {
    this.introduction = introduction;
  }

  public LessonPlanUpdate procedureText(@Nullable Optional<String> procedureText) {
    this.procedureText = procedureText;
    return this;
  }

  /**
   * Teaching procedure.
   * @return procedureText
   */
  
  @JsonProperty("procedureText")
  public @Nullable Optional<String> getProcedureText() {
    return procedureText;
  }

  @JsonProperty("procedureText")
  public void setProcedureText(@Nullable Optional<String> procedureText) {
    this.procedureText = procedureText;
  }

  public LessonPlanUpdate conclusion(@Nullable Optional<String> conclusion) {
    this.conclusion = conclusion;
    return this;
  }

  /**
   * Conclusion.
   * @return conclusion
   */
  
  @JsonProperty("conclusion")
  public @Nullable Optional<String> getConclusion() {
    return conclusion;
  }

  @JsonProperty("conclusion")
  public void setConclusion(@Nullable Optional<String> conclusion) {
    this.conclusion = conclusion;
  }

  public LessonPlanUpdate assessmentText(@Nullable Optional<String> assessmentText) {
    this.assessmentText = assessmentText;
    return this;
  }

  /**
   * Assessment approach.
   * @return assessmentText
   */
  
  @JsonProperty("assessmentText")
  public @Nullable Optional<String> getAssessmentText() {
    return assessmentText;
  }

  @JsonProperty("assessmentText")
  public void setAssessmentText(@Nullable Optional<String> assessmentText) {
    this.assessmentText = assessmentText;
  }

  public LessonPlanUpdate modification(@Nullable Optional<String> modification) {
    this.modification = modification;
    return this;
  }

  /**
   * Adaptations / differentiation.
   * @return modification
   */
  
  @JsonProperty("modification")
  public @Nullable Optional<String> getModification() {
    return modification;
  }

  @JsonProperty("modification")
  public void setModification(@Nullable Optional<String> modification) {
    this.modification = modification;
  }

  public LessonPlanUpdate notes(@Nullable Optional<String> notes) {
    this.notes = notes;
    return this;
  }

  /**
   * Notes.
   * @return notes
   */
  
  @JsonProperty("notes")
  public @Nullable Optional<String> getNotes() {
    return notes;
  }

  @JsonProperty("notes")
  public void setNotes(@Nullable Optional<String> notes) {
    this.notes = notes;
  }

  public LessonPlanUpdate startDate(@Nullable Optional<LocalDate> startDate) {
    this.startDate = startDate;
    return this;
  }

  /**
   * Planned start.
   * @return startDate
   */
  
  @JsonProperty("startDate")
  public @Nullable Optional<LocalDate> getStartDate() {
    return startDate;
  }

  @JsonProperty("startDate")
  public void setStartDate(@Nullable Optional<LocalDate> startDate) {
    this.startDate = startDate;
  }

  public LessonPlanUpdate endDate(@Nullable Optional<LocalDate> endDate) {
    this.endDate = endDate;
    return this;
  }

  /**
   * Planned end.
   * @return endDate
   */
  
  @JsonProperty("endDate")
  public @Nullable Optional<LocalDate> getEndDate() {
    return endDate;
  }

  @JsonProperty("endDate")
  public void setEndDate(@Nullable Optional<LocalDate> endDate) {
    this.endDate = endDate;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    LessonPlanUpdate lessonPlanUpdate = (LessonPlanUpdate) o;
    return Objects.equals(this.teachingAssignmentId, lessonPlanUpdate.teachingAssignmentId) &&
        Objects.equals(this.curriculumUnitId, lessonPlanUpdate.curriculumUnitId) &&
        Objects.equals(this.unitNo, lessonPlanUpdate.unitNo) &&
        Objects.equals(this.unitName, lessonPlanUpdate.unitName) &&
        Objects.equals(this.title, lessonPlanUpdate.title) &&
        Objects.equals(this.plannedDurationMinutes, lessonPlanUpdate.plannedDurationMinutes) &&
        Objects.equals(this.description, lessonPlanUpdate.description) &&
        Objects.equals(this.objective, lessonPlanUpdate.objective) &&
        Objects.equals(this.materialsRequired, lessonPlanUpdate.materialsRequired) &&
        Objects.equals(this.introduction, lessonPlanUpdate.introduction) &&
        Objects.equals(this.procedureText, lessonPlanUpdate.procedureText) &&
        Objects.equals(this.conclusion, lessonPlanUpdate.conclusion) &&
        Objects.equals(this.assessmentText, lessonPlanUpdate.assessmentText) &&
        Objects.equals(this.modification, lessonPlanUpdate.modification) &&
        Objects.equals(this.notes, lessonPlanUpdate.notes) &&
        Objects.equals(this.startDate, lessonPlanUpdate.startDate) &&
        Objects.equals(this.endDate, lessonPlanUpdate.endDate);
  }

  @Override
  public int hashCode() {
    return Objects.hash(teachingAssignmentId, curriculumUnitId, unitNo, unitName, title, plannedDurationMinutes, description, objective, materialsRequired, introduction, procedureText, conclusion, assessmentText, modification, notes, startDate, endDate);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class LessonPlanUpdate {\n");
    sb.append("    teachingAssignmentId: ").append(toIndentedString(teachingAssignmentId)).append("\n");
    sb.append("    curriculumUnitId: ").append(toIndentedString(curriculumUnitId)).append("\n");
    sb.append("    unitNo: ").append(toIndentedString(unitNo)).append("\n");
    sb.append("    unitName: ").append(toIndentedString(unitName)).append("\n");
    sb.append("    title: ").append(toIndentedString(title)).append("\n");
    sb.append("    plannedDurationMinutes: ").append(toIndentedString(plannedDurationMinutes)).append("\n");
    sb.append("    description: ").append(toIndentedString(description)).append("\n");
    sb.append("    objective: ").append(toIndentedString(objective)).append("\n");
    sb.append("    materialsRequired: ").append(toIndentedString(materialsRequired)).append("\n");
    sb.append("    introduction: ").append(toIndentedString(introduction)).append("\n");
    sb.append("    procedureText: ").append(toIndentedString(procedureText)).append("\n");
    sb.append("    conclusion: ").append(toIndentedString(conclusion)).append("\n");
    sb.append("    assessmentText: ").append(toIndentedString(assessmentText)).append("\n");
    sb.append("    modification: ").append(toIndentedString(modification)).append("\n");
    sb.append("    notes: ").append(toIndentedString(notes)).append("\n");
    sb.append("    startDate: ").append(toIndentedString(startDate)).append("\n");
    sb.append("    endDate: ").append(toIndentedString(endDate)).append("\n");
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

