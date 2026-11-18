// Owned API contract of sis.
package com.mysms.sis.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.mysms.sis.api.model.Person;
import com.mysms.sis.api.model.Student;
import org.springframework.lang.Nullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;

/**
 * ChildCard
 */

public class ChildCard {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private Student student;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private Person person;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String institutionName;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String classSectionLabel;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String relationship;

  public ChildCard() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public ChildCard(Student student, Person person) {
    this.student = student;
    this.person = person;
  }

  public ChildCard student(Student student) {
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

  public ChildCard person(Person person) {
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

  public ChildCard institutionName(@Nullable String institutionName) {
    this.institutionName = institutionName;
    return this;
  }

  /**
   * School.
   * @return institutionName
   */
  
  @JsonProperty("institutionName")
  public @Nullable String getInstitutionName() {
    return institutionName;
  }

  @JsonProperty("institutionName")
  public void setInstitutionName(@Nullable String institutionName) {
    this.institutionName = institutionName;
  }

  public ChildCard classSectionLabel(@Nullable String classSectionLabel) {
    this.classSectionLabel = classSectionLabel;
    return this;
  }

  /**
   * Class, e.g. Grade 5-A.
   * @return classSectionLabel
   */
  
  @JsonProperty("classSectionLabel")
  public @Nullable String getClassSectionLabel() {
    return classSectionLabel;
  }

  @JsonProperty("classSectionLabel")
  public void setClassSectionLabel(@Nullable String classSectionLabel) {
    this.classSectionLabel = classSectionLabel;
  }

  public ChildCard relationship(@Nullable String relationship) {
    this.relationship = relationship;
    return this;
  }

  /**
   * Relationship of the caller to the child.
   * @return relationship
   */
  
  @JsonProperty("relationship")
  public @Nullable String getRelationship() {
    return relationship;
  }

  @JsonProperty("relationship")
  public void setRelationship(@Nullable String relationship) {
    this.relationship = relationship;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ChildCard childCard = (ChildCard) o;
    return Objects.equals(this.student, childCard.student) &&
        Objects.equals(this.person, childCard.person) &&
        Objects.equals(this.institutionName, childCard.institutionName) &&
        Objects.equals(this.classSectionLabel, childCard.classSectionLabel) &&
        Objects.equals(this.relationship, childCard.relationship);
  }

  @Override
  public int hashCode() {
    return Objects.hash(student, person, institutionName, classSectionLabel, relationship);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ChildCard {\n");
    sb.append("    student: ").append(toIndentedString(student)).append("\n");
    sb.append("    person: ").append(toIndentedString(person)).append("\n");
    sb.append("    institutionName: ").append(toIndentedString(institutionName)).append("\n");
    sb.append("    classSectionLabel: ").append(toIndentedString(classSectionLabel)).append("\n");
    sb.append("    relationship: ").append(toIndentedString(relationship)).append("\n");
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

