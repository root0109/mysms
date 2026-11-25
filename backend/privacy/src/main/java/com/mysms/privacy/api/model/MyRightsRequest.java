// Owned API contract of privacy.
package com.mysms.privacy.api.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.lang.Nullable;

/**
 * One of the caller's own rights requests as the caller sees it: what was asked, where it stands, by when it is due and how it ended. Who handles it, how identity was checked and the staff's notes are not shown.
 */

public class MyRightsRequest {

  /**
   * The right exercised.
   */
  public enum TypeEnum {
    ACCESS("access"),
    
    CORRECTION("correction"),
    
    ERASURE("erasure"),
    
    EXPORT("export"),
    
    GRIEVANCE("grievance"),
    
    NOMINATION("nomination"),
    
    WITHDRAW_ALL("withdraw_all");

    private final String value;

    TypeEnum(String value) {
      this.value = value;
    }

    @JsonValue
    public String getValue() {
      return value;
    }

    @Override
    public String toString() {
      return String.valueOf(value);
    }

    @JsonCreator
    public static TypeEnum fromValue(String value) {
      for (TypeEnum b : TypeEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }

  /**
   * self (for oneself as an adult) or guardian (for a minor).
   */
  public enum RequesterCapacityEnum {
    GUARDIAN("guardian"),
    
    NOMINEE("nominee"),
    
    SELF("self");

    private final String value;

    RequesterCapacityEnum(String value) {
      this.value = value;
    }

    @JsonValue
    public String getValue() {
      return value;
    }

    @Override
    public String toString() {
      return String.valueOf(value);
    }

    @JsonCreator
    public static RequesterCapacityEnum fromValue(String value) {
      for (RequesterCapacityEnum b : RequesterCapacityEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }

  /**
   * submitted, verified, in_progress, action_required (a module's task failed), rejected or closed.
   */
  public enum StatusEnum {
    ACTION_REQUIRED("action_required"),
    
    CLOSED("closed"),
    
    IN_PROGRESS("in_progress"),
    
    REJECTED("rejected"),
    
    SUBMITTED("submitted"),
    
    VERIFIED("verified");

    private final String value;

    StatusEnum(String value) {
      this.value = value;
    }

    @JsonValue
    public String getValue() {
      return value;
    }

    @Override
    public String toString() {
      return String.valueOf(value);
    }

    @JsonCreator
    public static StatusEnum fromValue(String value) {
      for (StatusEnum b : StatusEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }

  /**
   * How it ended: fulfilled, partially_fulfilled or declined (closed); identity_not_verified, not_entitled, duplicate or other (rejected, which may be appealed as a grievance). Absent while open.
   */
  public enum OutcomeCodeEnum {
    DECLINED("declined"),
    
    DUPLICATE("duplicate"),
    
    FULFILLED("fulfilled"),
    
    IDENTITY_NOT_VERIFIED("identity_not_verified"),
    
    NOT_ENTITLED("not_entitled"),
    
    OTHER("other"),
    
    PARTIALLY_FULFILLED("partially_fulfilled");

    private final String value;

    OutcomeCodeEnum(String value) {
      this.value = value;
    }

    @JsonValue
    public String getValue() {
      return value;
    }

    @Override
    public String toString() {
      return String.valueOf(value);
    }

    @JsonCreator
    public static OutcomeCodeEnum fromValue(String value) {
      for (OutcomeCodeEnum b : OutcomeCodeEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }

  /**
   * For a grievance: what it is about.
   */
  public enum GrievanceCategoryEnum {
    ACCESS("access"),
    
    COMMUNICATION("communication"),
    
    CONSENT("consent"),
    
    CORRECTION("correction"),
    
    DATA_SECURITY("data_security"),
    
    OTHER("other");

    private final String value;

    GrievanceCategoryEnum(String value) {
      this.value = value;
    }

    @JsonValue
    public String getValue() {
      return value;
    }

    @Override
    public String toString() {
      return String.valueOf(value);
    }

    @JsonCreator
    public static GrievanceCategoryEnum fromValue(String value) {
      for (GrievanceCategoryEnum b : GrievanceCategoryEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private UUID id;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private String number;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private TypeEnum type;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private UUID subjectPersonId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private RequesterCapacityEnum requesterCapacity;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private StatusEnum status;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private String description;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private OffsetDateTime submittedAt;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private OffsetDateTime dueAt;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private Boolean overdue;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private @Nullable OffsetDateTime closedAt;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable OutcomeCodeEnum outcomeCode;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable RightsRequestCorrection correction;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable GrievanceCategoryEnum grievanceCategory;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String response;

  public MyRightsRequest() {
    super();
  }

  public MyRightsRequest id(UUID id) {
    this.id = id;
    return this;
  }

  /**
   * The request's id. Output only.
   * @return id
   */
  @NotNull @Valid 
  @JsonProperty("id")
  public UUID getId() {
    return id;
  }

  @JsonProperty("id")
  public void setId(UUID id) {
    this.id = id;
  }

  public MyRightsRequest number(String number) {
    this.number = number;
    return this;
  }

  /**
   * The request's number, PRV-<year>-<six digits>, quoted in every letter about it.
   * @return number
   */
  @NotNull 
  @JsonProperty("number")
  public String getNumber() {
    return number;
  }

  @JsonProperty("number")
  public void setNumber(String number) {
    this.number = number;
  }

  public MyRightsRequest type(TypeEnum type) {
    this.type = type;
    return this;
  }

  /**
   * The right exercised.
   * @return type
   */
  @NotNull 
  @JsonProperty("type")
  public TypeEnum getType() {
    return type;
  }

  @JsonProperty("type")
  public void setType(TypeEnum type) {
    this.type = type;
  }

  public MyRightsRequest subjectPersonId(UUID subjectPersonId) {
    this.subjectPersonId = subjectPersonId;
    return this;
  }

  /**
   * The person the request is about: the caller, or a child the caller is the legal guardian of.
   * @return subjectPersonId
   */
  @NotNull @Valid 
  @JsonProperty("subjectPersonId")
  public UUID getSubjectPersonId() {
    return subjectPersonId;
  }

  @JsonProperty("subjectPersonId")
  public void setSubjectPersonId(UUID subjectPersonId) {
    this.subjectPersonId = subjectPersonId;
  }

  public MyRightsRequest requesterCapacity(RequesterCapacityEnum requesterCapacity) {
    this.requesterCapacity = requesterCapacity;
    return this;
  }

  /**
   * self (for oneself as an adult) or guardian (for a minor).
   * @return requesterCapacity
   */
  @NotNull 
  @JsonProperty("requesterCapacity")
  public RequesterCapacityEnum getRequesterCapacity() {
    return requesterCapacity;
  }

  @JsonProperty("requesterCapacity")
  public void setRequesterCapacity(RequesterCapacityEnum requesterCapacity) {
    this.requesterCapacity = requesterCapacity;
  }

  public MyRightsRequest status(StatusEnum status) {
    this.status = status;
    return this;
  }

  /**
   * submitted, verified, in_progress, action_required (a module's task failed), rejected or closed.
   * @return status
   */
  @NotNull 
  @JsonProperty("status")
  public StatusEnum getStatus() {
    return status;
  }

  @JsonProperty("status")
  public void setStatus(StatusEnum status) {
    this.status = status;
  }

  public MyRightsRequest description(String description) {
    this.description = description;
    return this;
  }

  /**
   * What the caller asked.
   * @return description
   */
  @NotNull 
  @JsonProperty("description")
  public String getDescription() {
    return description;
  }

  @JsonProperty("description")
  public void setDescription(String description) {
    this.description = description;
  }

  public MyRightsRequest submittedAt(OffsetDateTime submittedAt) {
    this.submittedAt = submittedAt;
    return this;
  }

  /**
   * When the request was submitted.
   * @return submittedAt
   */
  @NotNull @Valid 
  @JsonProperty("submittedAt")
  public OffsetDateTime getSubmittedAt() {
    return submittedAt;
  }

  @JsonProperty("submittedAt")
  public void setSubmittedAt(OffsetDateTime submittedAt) {
    this.submittedAt = submittedAt;
  }

  public MyRightsRequest dueAt(OffsetDateTime dueAt) {
    this.dueAt = dueAt;
    return this;
  }

  /**
   * By when the school must respond.
   * @return dueAt
   */
  @NotNull @Valid 
  @JsonProperty("dueAt")
  public OffsetDateTime getDueAt() {
    return dueAt;
  }

  @JsonProperty("dueAt")
  public void setDueAt(OffsetDateTime dueAt) {
    this.dueAt = dueAt;
  }

  public MyRightsRequest overdue(Boolean overdue) {
    this.overdue = overdue;
    return this;
  }

  /**
   * Still open past dueAt.
   * @return overdue
   */
  @NotNull 
  @JsonProperty("overdue")
  public Boolean getOverdue() {
    return overdue;
  }

  @JsonProperty("overdue")
  public void setOverdue(Boolean overdue) {
    this.overdue = overdue;
  }

  public MyRightsRequest closedAt(@Nullable OffsetDateTime closedAt) {
    this.closedAt = closedAt;
    return this;
  }

  /**
   * When the request was closed or rejected; absent while open.
   * @return closedAt
   */
  @Valid 
  @JsonProperty("closedAt")
  public @Nullable OffsetDateTime getClosedAt() {
    return closedAt;
  }

  @JsonProperty("closedAt")
  public void setClosedAt(@Nullable OffsetDateTime closedAt) {
    this.closedAt = closedAt;
  }

  public MyRightsRequest outcomeCode(@Nullable OutcomeCodeEnum outcomeCode) {
    this.outcomeCode = outcomeCode;
    return this;
  }

  /**
   * How it ended: fulfilled, partially_fulfilled or declined (closed); identity_not_verified, not_entitled, duplicate or other (rejected, which may be appealed as a grievance). Absent while open.
   * @return outcomeCode
   */
   
  @JsonProperty("outcomeCode")
  public @Nullable OutcomeCodeEnum getOutcomeCode() {
    return outcomeCode;
  }

  @JsonProperty("outcomeCode")
  public void setOutcomeCode(@Nullable OutcomeCodeEnum outcomeCode) {
    this.outcomeCode = outcomeCode;
  }

  public MyRightsRequest correction(@Nullable RightsRequestCorrection correction) {
    this.correction = correction;
    return this;
  }

  /**
   * For a correction: the record and the fields to correct.
   * @return correction
   */
  @Valid 
  @JsonProperty("correction")
  public @Nullable RightsRequestCorrection getCorrection() {
    return correction;
  }

  @JsonProperty("correction")
  public void setCorrection(@Nullable RightsRequestCorrection correction) {
    this.correction = correction;
  }

  public MyRightsRequest grievanceCategory(@Nullable GrievanceCategoryEnum grievanceCategory) {
    this.grievanceCategory = grievanceCategory;
    return this;
  }

  /**
   * For a grievance: what it is about.
   * @return grievanceCategory
   */
   
  @JsonProperty("grievanceCategory")
  public @Nullable GrievanceCategoryEnum getGrievanceCategory() {
    return grievanceCategory;
  }

  @JsonProperty("grievanceCategory")
  public void setGrievanceCategory(@Nullable GrievanceCategoryEnum grievanceCategory) {
    this.grievanceCategory = grievanceCategory;
  }

  public MyRightsRequest response(@Nullable String response) {
    this.response = response;
    return this;
  }

  /**
   * The school's answer to the request, given when it was closed; absent while open and when none was given.
   * @return response
   */
   
  @JsonProperty("response")
  public @Nullable String getResponse() {
    return response;
  }

  @JsonProperty("response")
  public void setResponse(@Nullable String response) {
    this.response = response;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    MyRightsRequest other = (MyRightsRequest) o;
    return Objects.equals(this.id, other.id) &&
        Objects.equals(this.number, other.number) &&
        Objects.equals(this.type, other.type) &&
        Objects.equals(this.subjectPersonId, other.subjectPersonId) &&
        Objects.equals(this.requesterCapacity, other.requesterCapacity) &&
        Objects.equals(this.status, other.status) &&
        Objects.equals(this.description, other.description) &&
        Objects.equals(this.submittedAt, other.submittedAt) &&
        Objects.equals(this.dueAt, other.dueAt) &&
        Objects.equals(this.overdue, other.overdue) &&
        Objects.equals(this.closedAt, other.closedAt) &&
        Objects.equals(this.outcomeCode, other.outcomeCode) &&
        Objects.equals(this.correction, other.correction) &&
        Objects.equals(this.grievanceCategory, other.grievanceCategory) &&
        Objects.equals(this.response, other.response);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, number, type, subjectPersonId, requesterCapacity, status, description, submittedAt, dueAt, overdue, closedAt, outcomeCode, correction, grievanceCategory, response);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class MyRightsRequest {\n");
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    number: ").append(toIndentedString(number)).append("\n");
    sb.append("    type: ").append(toIndentedString(type)).append("\n");
    sb.append("    subjectPersonId: ").append(toIndentedString(subjectPersonId)).append("\n");
    sb.append("    requesterCapacity: ").append(toIndentedString(requesterCapacity)).append("\n");
    sb.append("    status: ").append(toIndentedString(status)).append("\n");
    sb.append("    description: ").append(toIndentedString(description)).append("\n");
    sb.append("    submittedAt: ").append(toIndentedString(submittedAt)).append("\n");
    sb.append("    dueAt: ").append(toIndentedString(dueAt)).append("\n");
    sb.append("    overdue: ").append(toIndentedString(overdue)).append("\n");
    sb.append("    closedAt: ").append(toIndentedString(closedAt)).append("\n");
    sb.append("    outcomeCode: ").append(toIndentedString(outcomeCode)).append("\n");
    sb.append("    correction: ").append(toIndentedString(correction)).append("\n");
    sb.append("    grievanceCategory: ").append(toIndentedString(grievanceCategory)).append("\n");
    sb.append("    response: ").append(toIndentedString(response)).append("\n");
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
