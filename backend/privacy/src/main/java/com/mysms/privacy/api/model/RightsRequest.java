// Owned API contract of privacy.
package com.mysms.privacy.api.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import jakarta.validation.Valid;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.lang.Nullable;

/**
 * A rights request as the school's privacy staff see it: who asked what for whom, how identity was checked, who handles it, the deadline and the outcome.
 */

public class RightsRequest {

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
   * self (an adult for themself) or guardian (the active legal guardian of a minor).
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
   * How the requester's identity was established; absent while submitted.
   */
  public enum IdentityCheckEnum {
    DOCUMENT("document"),
    
    IN_PERSON("in_person"),
    
    SIGNED_IN_SESSION("signed_in_session");

    private final String value;

    IdentityCheckEnum(String value) {
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
    public static IdentityCheckEnum fromValue(String value) {
      for (IdentityCheckEnum b : IdentityCheckEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }

  /**
   * How it ended: fulfilled, partially_fulfilled or declined (closed); identity_not_verified, not_entitled, duplicate or other (rejected). Absent while open.
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
  private @Nullable UUID id;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String number;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable TypeEnum type;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID subjectPersonId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID requesterPersonId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable RequesterCapacityEnum requesterCapacity;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable StatusEnum status;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable IdentityCheckEnum identityCheck;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String description;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID assigneeUserId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private @Nullable OffsetDateTime submittedAt;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private @Nullable OffsetDateTime dueAt;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Boolean overdue;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String extendedReason;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private @Nullable OffsetDateTime closedAt;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable OutcomeCodeEnum outcomeCode;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String outcomeNote;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String etag;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable RightsRequestCorrection correction;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable GrievanceCategoryEnum grievanceCategory;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String response;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable List<RightsRequestTask> tasks;

  public RightsRequest() {
    super();
  }

  public RightsRequest id(@Nullable UUID id) {
    this.id = id;
    return this;
  }

  /**
   * Surrogate primary key (UUIDv7 generated by the application). Output only.
   * @return id
   */
  @Valid 
  @JsonProperty("id")
  public @Nullable UUID getId() {
    return id;
  }

  @JsonProperty("id")
  public void setId(@Nullable UUID id) {
    this.id = id;
  }

  public RightsRequest number(@Nullable String number) {
    this.number = number;
    return this;
  }

  /**
   * The request's number, PRV-<year>-<six digits>.
   * @return number
   */
   
  @JsonProperty("number")
  public @Nullable String getNumber() {
    return number;
  }

  @JsonProperty("number")
  public void setNumber(@Nullable String number) {
    this.number = number;
  }

  public RightsRequest type(@Nullable TypeEnum type) {
    this.type = type;
    return this;
  }

  /**
   * The right exercised.
   * @return type
   */
   
  @JsonProperty("type")
  public @Nullable TypeEnum getType() {
    return type;
  }

  @JsonProperty("type")
  public void setType(@Nullable TypeEnum type) {
    this.type = type;
  }

  public RightsRequest subjectPersonId(@Nullable UUID subjectPersonId) {
    this.subjectPersonId = subjectPersonId;
    return this;
  }

  /**
   * The data principal the request is about.
   * @return subjectPersonId
   */
  @Valid 
  @JsonProperty("subjectPersonId")
  public @Nullable UUID getSubjectPersonId() {
    return subjectPersonId;
  }

  @JsonProperty("subjectPersonId")
  public void setSubjectPersonId(@Nullable UUID subjectPersonId) {
    this.subjectPersonId = subjectPersonId;
  }

  public RightsRequest requesterPersonId(@Nullable UUID requesterPersonId) {
    this.requesterPersonId = requesterPersonId;
    return this;
  }

  /**
   * The person who submitted it.
   * @return requesterPersonId
   */
  @Valid 
  @JsonProperty("requesterPersonId")
  public @Nullable UUID getRequesterPersonId() {
    return requesterPersonId;
  }

  @JsonProperty("requesterPersonId")
  public void setRequesterPersonId(@Nullable UUID requesterPersonId) {
    this.requesterPersonId = requesterPersonId;
  }

  public RightsRequest requesterCapacity(@Nullable RequesterCapacityEnum requesterCapacity) {
    this.requesterCapacity = requesterCapacity;
    return this;
  }

  /**
   * self (an adult for themself) or guardian (the active legal guardian of a minor).
   * @return requesterCapacity
   */
   
  @JsonProperty("requesterCapacity")
  public @Nullable RequesterCapacityEnum getRequesterCapacity() {
    return requesterCapacity;
  }

  @JsonProperty("requesterCapacity")
  public void setRequesterCapacity(@Nullable RequesterCapacityEnum requesterCapacity) {
    this.requesterCapacity = requesterCapacity;
  }

  public RightsRequest status(@Nullable StatusEnum status) {
    this.status = status;
    return this;
  }

  /**
   * submitted, verified, in_progress, action_required (a module's task failed), rejected or closed.
   * @return status
   */
   
  @JsonProperty("status")
  public @Nullable StatusEnum getStatus() {
    return status;
  }

  @JsonProperty("status")
  public void setStatus(@Nullable StatusEnum status) {
    this.status = status;
  }

  public RightsRequest identityCheck(@Nullable IdentityCheckEnum identityCheck) {
    this.identityCheck = identityCheck;
    return this;
  }

  /**
   * How the requester's identity was established; absent while submitted.
   * @return identityCheck
   */
   
  @JsonProperty("identityCheck")
  public @Nullable IdentityCheckEnum getIdentityCheck() {
    return identityCheck;
  }

  @JsonProperty("identityCheck")
  public void setIdentityCheck(@Nullable IdentityCheckEnum identityCheck) {
    this.identityCheck = identityCheck;
  }

  public RightsRequest description(@Nullable String description) {
    this.description = description;
    return this;
  }

  /**
   * What the requester asked, in their own words.
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

  public RightsRequest assigneeUserId(@Nullable UUID assigneeUserId) {
    this.assigneeUserId = assigneeUserId;
    return this;
  }

  /**
   * The staff member handling the request; absent until assigned.
   * @return assigneeUserId
   */
  @Valid 
  @JsonProperty("assigneeUserId")
  public @Nullable UUID getAssigneeUserId() {
    return assigneeUserId;
  }

  @JsonProperty("assigneeUserId")
  public void setAssigneeUserId(@Nullable UUID assigneeUserId) {
    this.assigneeUserId = assigneeUserId;
  }

  public RightsRequest submittedAt(@Nullable OffsetDateTime submittedAt) {
    this.submittedAt = submittedAt;
    return this;
  }

  /**
   * When the request was submitted.
   * @return submittedAt
   */
  @Valid 
  @JsonProperty("submittedAt")
  public @Nullable OffsetDateTime getSubmittedAt() {
    return submittedAt;
  }

  @JsonProperty("submittedAt")
  public void setSubmittedAt(@Nullable OffsetDateTime submittedAt) {
    this.submittedAt = submittedAt;
  }

  public RightsRequest dueAt(@Nullable OffsetDateTime dueAt) {
    this.dueAt = dueAt;
    return this;
  }

  /**
   * The response deadline.
   * @return dueAt
   */
  @Valid 
  @JsonProperty("dueAt")
  public @Nullable OffsetDateTime getDueAt() {
    return dueAt;
  }

  @JsonProperty("dueAt")
  public void setDueAt(@Nullable OffsetDateTime dueAt) {
    this.dueAt = dueAt;
  }

  public RightsRequest overdue(@Nullable Boolean overdue) {
    this.overdue = overdue;
    return this;
  }

  /**
   * Still open past dueAt.
   * @return overdue
   */
   
  @JsonProperty("overdue")
  public @Nullable Boolean getOverdue() {
    return overdue;
  }

  @JsonProperty("overdue")
  public void setOverdue(@Nullable Boolean overdue) {
    this.overdue = overdue;
  }

  public RightsRequest extendedReason(@Nullable String extendedReason) {
    this.extendedReason = extendedReason;
    return this;
  }

  /**
   * Why the deadline was last extended; absent when never extended.
   * @return extendedReason
   */
   
  @JsonProperty("extendedReason")
  public @Nullable String getExtendedReason() {
    return extendedReason;
  }

  @JsonProperty("extendedReason")
  public void setExtendedReason(@Nullable String extendedReason) {
    this.extendedReason = extendedReason;
  }

  public RightsRequest closedAt(@Nullable OffsetDateTime closedAt) {
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

  public RightsRequest outcomeCode(@Nullable OutcomeCodeEnum outcomeCode) {
    this.outcomeCode = outcomeCode;
    return this;
  }

  /**
   * How it ended: fulfilled, partially_fulfilled or declined (closed); identity_not_verified, not_entitled, duplicate or other (rejected). Absent while open.
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

  public RightsRequest outcomeNote(@Nullable String outcomeNote) {
    this.outcomeNote = outcomeNote;
    return this;
  }

  /**
   * The staff's note on closing; absent when none.
   * @return outcomeNote
   */
   
  @JsonProperty("outcomeNote")
  public @Nullable String getOutcomeNote() {
    return outcomeNote;
  }

  @JsonProperty("outcomeNote")
  public void setOutcomeNote(@Nullable String outcomeNote) {
    this.outcomeNote = outcomeNote;
  }

  public RightsRequest etag(@Nullable String etag) {
    this.etag = etag;
    return this;
  }

  /**
   * Entity tag (row version); send back on each step.
   * @return etag
   */
   
  @JsonProperty("etag")
  public @Nullable String getEtag() {
    return etag;
  }

  @JsonProperty("etag")
  public void setEtag(@Nullable String etag) {
    this.etag = etag;
  }

  public RightsRequest correction(@Nullable RightsRequestCorrection correction) {
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

  public RightsRequest grievanceCategory(@Nullable GrievanceCategoryEnum grievanceCategory) {
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

  public RightsRequest response(@Nullable String response) {
    this.response = response;
    return this;
  }

  /**
   * The school's answer the requester sees, given when the request was closed.
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

  public RightsRequest tasks(@Nullable List<RightsRequestTask> tasks) {
    this.tasks = tasks;
    return this;
  }

  public RightsRequest addTasksItem(RightsRequestTask tasksItem) {
    if (this.tasks == null) {
      this.tasks = new ArrayList<>();
    }
    this.tasks.add(tasksItem);
    return this;
  }

  /**
   * The modules' tasks of an erasure or export request once its fulfilment started, wave by wave; empty for any other request. Output only.
   * @return tasks
   */
  
  @JsonProperty("tasks")
  public @Nullable List<@Valid RightsRequestTask> getTasks() {
    return tasks;
  }

  @JsonProperty("tasks")
  public void setTasks(@Nullable List<RightsRequestTask> tasks) {
    this.tasks = tasks;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    RightsRequest other = (RightsRequest) o;
    return Objects.equals(this.id, other.id) &&
        Objects.equals(this.number, other.number) &&
        Objects.equals(this.type, other.type) &&
        Objects.equals(this.subjectPersonId, other.subjectPersonId) &&
        Objects.equals(this.requesterPersonId, other.requesterPersonId) &&
        Objects.equals(this.requesterCapacity, other.requesterCapacity) &&
        Objects.equals(this.status, other.status) &&
        Objects.equals(this.identityCheck, other.identityCheck) &&
        Objects.equals(this.description, other.description) &&
        Objects.equals(this.assigneeUserId, other.assigneeUserId) &&
        Objects.equals(this.submittedAt, other.submittedAt) &&
        Objects.equals(this.dueAt, other.dueAt) &&
        Objects.equals(this.overdue, other.overdue) &&
        Objects.equals(this.extendedReason, other.extendedReason) &&
        Objects.equals(this.closedAt, other.closedAt) &&
        Objects.equals(this.outcomeCode, other.outcomeCode) &&
        Objects.equals(this.outcomeNote, other.outcomeNote) &&
        Objects.equals(this.etag, other.etag) &&
        Objects.equals(this.correction, other.correction) &&
        Objects.equals(this.grievanceCategory, other.grievanceCategory) &&
        Objects.equals(this.response, other.response) &&
        Objects.equals(this.tasks, other.tasks);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, number, type, subjectPersonId, requesterPersonId, requesterCapacity, status, identityCheck, description, assigneeUserId, submittedAt, dueAt, overdue, extendedReason, closedAt, outcomeCode, outcomeNote, etag, correction, grievanceCategory, response, tasks);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class RightsRequest {\n");
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    number: ").append(toIndentedString(number)).append("\n");
    sb.append("    type: ").append(toIndentedString(type)).append("\n");
    sb.append("    subjectPersonId: ").append(toIndentedString(subjectPersonId)).append("\n");
    sb.append("    requesterPersonId: ").append(toIndentedString(requesterPersonId)).append("\n");
    sb.append("    requesterCapacity: ").append(toIndentedString(requesterCapacity)).append("\n");
    sb.append("    status: ").append(toIndentedString(status)).append("\n");
    sb.append("    identityCheck: ").append(toIndentedString(identityCheck)).append("\n");
    sb.append("    description: ").append(toIndentedString(description)).append("\n");
    sb.append("    assigneeUserId: ").append(toIndentedString(assigneeUserId)).append("\n");
    sb.append("    submittedAt: ").append(toIndentedString(submittedAt)).append("\n");
    sb.append("    dueAt: ").append(toIndentedString(dueAt)).append("\n");
    sb.append("    overdue: ").append(toIndentedString(overdue)).append("\n");
    sb.append("    extendedReason: ").append(toIndentedString(extendedReason)).append("\n");
    sb.append("    closedAt: ").append(toIndentedString(closedAt)).append("\n");
    sb.append("    outcomeCode: ").append(toIndentedString(outcomeCode)).append("\n");
    sb.append("    outcomeNote: ").append(toIndentedString(outcomeNote)).append("\n");
    sb.append("    etag: ").append(toIndentedString(etag)).append("\n");
    sb.append("    correction: ").append(toIndentedString(correction)).append("\n");
    sb.append("    grievanceCategory: ").append(toIndentedString(grievanceCategory)).append("\n");
    sb.append("    response: ").append(toIndentedString(response)).append("\n");
    sb.append("    tasks: ").append(toIndentedString(tasks)).append("\n");
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
