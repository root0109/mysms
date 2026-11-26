// Owned API contract of sis.
package com.mysms.students.sis.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.mysms.students.sis.api.model.AttendanceSummary;
import com.mysms.students.sis.api.model.Person;
import com.mysms.students.sis.api.model.ProgramEnrollment;
import com.mysms.students.sis.api.model.Student;
import com.mysms.students.sis.api.model.StudentEnrollment;
import com.mysms.students.sis.api.model.StudentGuardian;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.springframework.lang.Nullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;

/**
 * StudentSummary
 */

public class StudentSummary {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private Student student;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private Person person;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable StudentEnrollment currentEnrollment;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable List<@Valid ProgramEnrollment> programEnrollments = new ArrayList<>();

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable List<@Valid StudentGuardian> guardians = new ArrayList<>();

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable AttendanceSummary attendance;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String outstandingFees;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Integer openLoans;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Integer openIncidents;

  public StudentSummary() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public StudentSummary(Student student, Person person) {
    this.student = student;
    this.person = person;
  }

  public StudentSummary student(Student student) {
    this.student = student;
    return this;
  }

  /**
   * Get student
   * @return student
   */
  @NotNull @Valid 
  @JsonProperty("student")
  public Student getStudent() {
    return student;
  }

  @JsonProperty("student")
  public void setStudent(Student student) {
    this.student = student;
  }

  public StudentSummary person(Person person) {
    this.person = person;
    return this;
  }

  /**
   * Get person
   * @return person
   */
  @NotNull @Valid 
  @JsonProperty("person")
  public Person getPerson() {
    return person;
  }

  @JsonProperty("person")
  public void setPerson(Person person) {
    this.person = person;
  }

  public StudentSummary currentEnrollment(@Nullable StudentEnrollment currentEnrollment) {
    this.currentEnrollment = currentEnrollment;
    return this;
  }

  /**
   * Get currentEnrollment
   * @return currentEnrollment
   */
  @Valid 
  @JsonProperty("currentEnrollment")
  public @Nullable StudentEnrollment getCurrentEnrollment() {
    return currentEnrollment;
  }

  @JsonProperty("currentEnrollment")
  public void setCurrentEnrollment(@Nullable StudentEnrollment currentEnrollment) {
    this.currentEnrollment = currentEnrollment;
  }

  public StudentSummary programEnrollments(List<@Valid ProgramEnrollment> programEnrollments) {
    this.programEnrollments = programEnrollments;
    return this;
  }

  public StudentSummary addProgramEnrollmentsItem(ProgramEnrollment programEnrollmentsItem) {
    if (this.programEnrollments == null) {
      this.programEnrollments = new ArrayList<>();
    }
    this.programEnrollments.add(programEnrollmentsItem);
    return this;
  }

  /**
   * Course registrations.
   * @return programEnrollments
   */
  
  @JsonProperty("programEnrollments")
  public List<@Valid ProgramEnrollment> getProgramEnrollments() {
    return programEnrollments;
  }

  @JsonProperty("programEnrollments")
  public void setProgramEnrollments(List<@Valid ProgramEnrollment> programEnrollments) {
    this.programEnrollments = programEnrollments;
  }

  public StudentSummary guardians(List<@Valid StudentGuardian> guardians) {
    this.guardians = guardians;
    return this;
  }

  public StudentSummary addGuardiansItem(StudentGuardian guardiansItem) {
    if (this.guardians == null) {
      this.guardians = new ArrayList<>();
    }
    this.guardians.add(guardiansItem);
    return this;
  }

  /**
   * Guardian links.
   * @return guardians
   */
  
  @JsonProperty("guardians")
  public List<@Valid StudentGuardian> getGuardians() {
    return guardians;
  }

  @JsonProperty("guardians")
  public void setGuardians(List<@Valid StudentGuardian> guardians) {
    this.guardians = guardians;
  }

  public StudentSummary attendance(@Nullable AttendanceSummary attendance) {
    this.attendance = attendance;
    return this;
  }

  /**
   * Get attendance
   * @return attendance
   */
  @Valid 
  @JsonProperty("attendance")
  public @Nullable AttendanceSummary getAttendance() {
    return attendance;
  }

  @JsonProperty("attendance")
  public void setAttendance(@Nullable AttendanceSummary attendance) {
    this.attendance = attendance;
  }

  public StudentSummary outstandingFees(@Nullable String outstandingFees) {
    this.outstandingFees = outstandingFees;
    return this;
  }

  /**
   * Unpaid fee total.
   * @return outstandingFees
   */
  @Pattern(regexp = "^-?\\d+(\\.\\d+)?$") 
  @JsonProperty("outstandingFees")
  public @Nullable String getOutstandingFees() {
    return outstandingFees;
  }

  @JsonProperty("outstandingFees")
  public void setOutstandingFees(@Nullable String outstandingFees) {
    this.outstandingFees = outstandingFees;
  }

  public StudentSummary openLoans(@Nullable Integer openLoans) {
    this.openLoans = openLoans;
    return this;
  }

  /**
   * Library items on loan.
   * @return openLoans
   */
  
  @JsonProperty("openLoans")
  public @Nullable Integer getOpenLoans() {
    return openLoans;
  }

  @JsonProperty("openLoans")
  public void setOpenLoans(@Nullable Integer openLoans) {
    this.openLoans = openLoans;
  }

  public StudentSummary openIncidents(@Nullable Integer openIncidents) {
    this.openIncidents = openIncidents;
    return this;
  }

  /**
   * Discipline incidents without action.
   * @return openIncidents
   */
  
  @JsonProperty("openIncidents")
  public @Nullable Integer getOpenIncidents() {
    return openIncidents;
  }

  @JsonProperty("openIncidents")
  public void setOpenIncidents(@Nullable Integer openIncidents) {
    this.openIncidents = openIncidents;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    StudentSummary studentSummary = (StudentSummary) o;
    return Objects.equals(this.student, studentSummary.student) &&
        Objects.equals(this.person, studentSummary.person) &&
        Objects.equals(this.currentEnrollment, studentSummary.currentEnrollment) &&
        Objects.equals(this.programEnrollments, studentSummary.programEnrollments) &&
        Objects.equals(this.guardians, studentSummary.guardians) &&
        Objects.equals(this.attendance, studentSummary.attendance) &&
        Objects.equals(this.outstandingFees, studentSummary.outstandingFees) &&
        Objects.equals(this.openLoans, studentSummary.openLoans) &&
        Objects.equals(this.openIncidents, studentSummary.openIncidents);
  }

  @Override
  public int hashCode() {
    return Objects.hash(student, person, currentEnrollment, programEnrollments, guardians, attendance, outstandingFees, openLoans, openIncidents);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class StudentSummary {\n");
    sb.append("    student: ").append(toIndentedString(student)).append("\n");
    sb.append("    person: ").append(toIndentedString(person)).append("\n");
    sb.append("    currentEnrollment: ").append(toIndentedString(currentEnrollment)).append("\n");
    sb.append("    programEnrollments: ").append(toIndentedString(programEnrollments)).append("\n");
    sb.append("    guardians: ").append(toIndentedString(guardians)).append("\n");
    sb.append("    attendance: ").append(toIndentedString(attendance)).append("\n");
    sb.append("    outstandingFees: ").append(toIndentedString(outstandingFees)).append("\n");
    sb.append("    openLoans: ").append(toIndentedString(openLoans)).append("\n");
    sb.append("    openIncidents: ").append(toIndentedString(openIncidents)).append("\n");
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

