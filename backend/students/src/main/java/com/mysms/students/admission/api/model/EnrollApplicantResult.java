// Owned API contract of admission.
package com.mysms.students.admission.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.mysms.students.admission.api.model.Application;
import com.mysms.students.admission.api.model.Operation;
import java.util.UUID;
import org.springframework.lang.Nullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;

/**
 * Outcome of enrolling an applicant. The admission module records the enrollment and emits events; the student record itself lives in sis.
 */

public class EnrollApplicantResult {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private Application application;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private UUID studentId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Operation operation;

  public EnrollApplicantResult() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public EnrollApplicantResult(Application application, UUID studentId) {
    this.application = application;
    this.studentId = studentId;
  }

  public EnrollApplicantResult application(Application application) {
    this.application = application;
    return this;
  }

  /**
   * Get application
   * @return application
   */
  @NotNull @Valid 
  @JsonProperty("application")
  public Application getApplication() {
    return application;
  }

  @JsonProperty("application")
  public void setApplication(Application application) {
    this.application = application;
  }

  public EnrollApplicantResult studentId(UUID studentId) {
    this.studentId = studentId;
    return this;
  }

  /**
   * Student created in sis (fetch via sis when needed).
   * @return studentId
   */
  @NotNull @Valid 
  @JsonProperty("studentId")
  public UUID getStudentId() {
    return studentId;
  }

  @JsonProperty("studentId")
  public void setStudentId(UUID studentId) {
    this.studentId = studentId;
  }

  public EnrollApplicantResult operation(@Nullable Operation operation) {
    this.operation = operation;
    return this;
  }

  /**
   * Get operation
   * @return operation
   */
  @Valid 
  @JsonProperty("operation")
  public @Nullable Operation getOperation() {
    return operation;
  }

  @JsonProperty("operation")
  public void setOperation(@Nullable Operation operation) {
    this.operation = operation;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    EnrollApplicantResult enrollApplicantResult = (EnrollApplicantResult) o;
    return Objects.equals(this.application, enrollApplicantResult.application) &&
        Objects.equals(this.studentId, enrollApplicantResult.studentId) &&
        Objects.equals(this.operation, enrollApplicantResult.operation);
  }

  @Override
  public int hashCode() {
    return Objects.hash(application, studentId, operation);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class EnrollApplicantResult {\n");
    sb.append("    application: ").append(toIndentedString(application)).append("\n");
    sb.append("    studentId: ").append(toIndentedString(studentId)).append("\n");
    sb.append("    operation: ").append(toIndentedString(operation)).append("\n");
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

