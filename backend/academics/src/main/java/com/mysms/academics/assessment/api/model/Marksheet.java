// Owned API contract of assessment.
package com.mysms.academics.assessment.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.lang.Nullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;

/**
 * Generated marksheet register for an exam series.
 */

public class Marksheet {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private @Nullable OffsetDateTime publishedAt;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID publishedByUserId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Boolean visibleToGuardians;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID id;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private UUID examSessionId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private UUID resultTemplateId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private String name;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  private LocalDate generatedOn;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private UUID generatedByUserId;

  /**
   * State. Changed only through the workflow operations.
   */
  public enum StatusEnum {
    DRAFT("draft"),
    
    VALIDATED("validated"),
    
    CANCELLED("cancelled");

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

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable StatusEnum status;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Integer totalPassed;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Integer totalFailed;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private @Nullable OffsetDateTime createTime;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID createdBy;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private @Nullable OffsetDateTime updateTime;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID updatedBy;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String etag;

  public Marksheet() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public Marksheet(UUID examSessionId, UUID resultTemplateId, String name, LocalDate generatedOn, UUID generatedByUserId) {
    this.examSessionId = examSessionId;
    this.resultTemplateId = resultTemplateId;
    this.name = name;
    this.generatedOn = generatedOn;
    this.generatedByUserId = generatedByUserId;
  }

  public Marksheet publishedAt(@Nullable OffsetDateTime publishedAt) {
    this.publishedAt = publishedAt;
    return this;
  }

  /**
   * When results were released to students/guardians (NULL = not visible to them).
   * @return publishedAt
   */
  @Valid 
  @JsonProperty("publishedAt")
  public @Nullable OffsetDateTime getPublishedAt() {
    return publishedAt;
  }

  @JsonProperty("publishedAt")
  public void setPublishedAt(@Nullable OffsetDateTime publishedAt) {
    this.publishedAt = publishedAt;
  }

  public Marksheet publishedByUserId(@Nullable UUID publishedByUserId) {
    this.publishedByUserId = publishedByUserId;
    return this;
  }

  /**
   * User who published the marksheet.
   * @return publishedByUserId
   */
  @Valid 
  @JsonProperty("publishedByUserId")
  public @Nullable UUID getPublishedByUserId() {
    return publishedByUserId;
  }

  @JsonProperty("publishedByUserId")
  public void setPublishedByUserId(@Nullable UUID publishedByUserId) {
    this.publishedByUserId = publishedByUserId;
  }

  public Marksheet visibleToGuardians(@Nullable Boolean visibleToGuardians) {
    this.visibleToGuardians = visibleToGuardians;
    return this;
  }

  /**
   * Whether guardians can see it once published.
   * @return visibleToGuardians
   */
  
  @JsonProperty("visibleToGuardians")
  public @Nullable Boolean getVisibleToGuardians() {
    return visibleToGuardians;
  }

  @JsonProperty("visibleToGuardians")
  public void setVisibleToGuardians(@Nullable Boolean visibleToGuardians) {
    this.visibleToGuardians = visibleToGuardians;
  }

  public Marksheet id(@Nullable UUID id) {
    this.id = id;
    return this;
  }

  /**
   * Surrogate primary key (UUIDv7 generated by the application; database default is a v4 fallback).
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

  public Marksheet examSessionId(UUID examSessionId) {
    this.examSessionId = examSessionId;
    return this;
  }

  /**
   * Exam series.
   * @return examSessionId
   */
  @NotNull @Valid 
  @JsonProperty("examSessionId")
  public UUID getExamSessionId() {
    return examSessionId;
  }

  @JsonProperty("examSessionId")
  public void setExamSessionId(UUID examSessionId) {
    this.examSessionId = examSessionId;
  }

  public Marksheet resultTemplateId(UUID resultTemplateId) {
    this.resultTemplateId = resultTemplateId;
    return this;
  }

  /**
   * Template used.
   * @return resultTemplateId
   */
  @NotNull @Valid 
  @JsonProperty("resultTemplateId")
  public UUID getResultTemplateId() {
    return resultTemplateId;
  }

  @JsonProperty("resultTemplateId")
  public void setResultTemplateId(UUID resultTemplateId) {
    this.resultTemplateId = resultTemplateId;
  }

  public Marksheet name(String name) {
    this.name = name;
    return this;
  }

  /**
   * Name.
   * @return name
   */
  @NotNull @Size(max = 150) 
  @JsonProperty("name")
  public String getName() {
    return name;
  }

  @JsonProperty("name")
  public void setName(String name) {
    this.name = name;
  }

  public Marksheet generatedOn(LocalDate generatedOn) {
    this.generatedOn = generatedOn;
    return this;
  }

  /**
   * Generation date.
   * @return generatedOn
   */
  @NotNull @Valid 
  @JsonProperty("generatedOn")
  public LocalDate getGeneratedOn() {
    return generatedOn;
  }

  @JsonProperty("generatedOn")
  public void setGeneratedOn(LocalDate generatedOn) {
    this.generatedOn = generatedOn;
  }

  public Marksheet generatedByUserId(UUID generatedByUserId) {
    this.generatedByUserId = generatedByUserId;
    return this;
  }

  /**
   * Generated by.
   * @return generatedByUserId
   */
  @NotNull @Valid 
  @JsonProperty("generatedByUserId")
  public UUID getGeneratedByUserId() {
    return generatedByUserId;
  }

  @JsonProperty("generatedByUserId")
  public void setGeneratedByUserId(UUID generatedByUserId) {
    this.generatedByUserId = generatedByUserId;
  }

  public Marksheet status(@Nullable StatusEnum status) {
    this.status = status;
    return this;
  }

  /**
   * State. Changed only through the workflow operations.
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

  public Marksheet totalPassed(@Nullable Integer totalPassed) {
    this.totalPassed = totalPassed;
    return this;
  }

  /**
   * Number passed.
   * @return totalPassed
   */
  
  @JsonProperty("totalPassed")
  public @Nullable Integer getTotalPassed() {
    return totalPassed;
  }

  @JsonProperty("totalPassed")
  public void setTotalPassed(@Nullable Integer totalPassed) {
    this.totalPassed = totalPassed;
  }

  public Marksheet totalFailed(@Nullable Integer totalFailed) {
    this.totalFailed = totalFailed;
    return this;
  }

  /**
   * Number failed.
   * @return totalFailed
   */
  
  @JsonProperty("totalFailed")
  public @Nullable Integer getTotalFailed() {
    return totalFailed;
  }

  @JsonProperty("totalFailed")
  public void setTotalFailed(@Nullable Integer totalFailed) {
    this.totalFailed = totalFailed;
  }

  public Marksheet createTime(@Nullable OffsetDateTime createTime) {
    this.createTime = createTime;
    return this;
  }

  /**
   * Row creation time.
   * @return createTime
   */
  @Valid 
  @JsonProperty("createTime")
  public @Nullable OffsetDateTime getCreateTime() {
    return createTime;
  }

  @JsonProperty("createTime")
  public void setCreateTime(@Nullable OffsetDateTime createTime) {
    this.createTime = createTime;
  }

  public Marksheet createdBy(@Nullable UUID createdBy) {
    this.createdBy = createdBy;
    return this;
  }

  /**
   * User account that created the row (audit reference to iam.user_account; not FK-constrained).
   * @return createdBy
   */
  @Valid 
  @JsonProperty("createdBy")
  public @Nullable UUID getCreatedBy() {
    return createdBy;
  }

  @JsonProperty("createdBy")
  public void setCreatedBy(@Nullable UUID createdBy) {
    this.createdBy = createdBy;
  }

  public Marksheet updateTime(@Nullable OffsetDateTime updateTime) {
    this.updateTime = updateTime;
    return this;
  }

  /**
   * Last update time.
   * @return updateTime
   */
  @Valid 
  @JsonProperty("updateTime")
  public @Nullable OffsetDateTime getUpdateTime() {
    return updateTime;
  }

  @JsonProperty("updateTime")
  public void setUpdateTime(@Nullable OffsetDateTime updateTime) {
    this.updateTime = updateTime;
  }

  public Marksheet updatedBy(@Nullable UUID updatedBy) {
    this.updatedBy = updatedBy;
    return this;
  }

  /**
   * User account that last updated the row (audit reference to iam.user_account; not FK-constrained).
   * @return updatedBy
   */
  @Valid 
  @JsonProperty("updatedBy")
  public @Nullable UUID getUpdatedBy() {
    return updatedBy;
  }

  @JsonProperty("updatedBy")
  public void setUpdatedBy(@Nullable UUID updatedBy) {
    this.updatedBy = updatedBy;
  }

  public Marksheet etag(@Nullable String etag) {
    this.etag = etag;
    return this;
  }

  /**
   * Entity tag (row version); send back in If-Match / etag.
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

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Marksheet marksheet = (Marksheet) o;
    return Objects.equals(this.publishedAt, marksheet.publishedAt) &&
        Objects.equals(this.publishedByUserId, marksheet.publishedByUserId) &&
        Objects.equals(this.visibleToGuardians, marksheet.visibleToGuardians) &&
        Objects.equals(this.id, marksheet.id) &&
        Objects.equals(this.examSessionId, marksheet.examSessionId) &&
        Objects.equals(this.resultTemplateId, marksheet.resultTemplateId) &&
        Objects.equals(this.name, marksheet.name) &&
        Objects.equals(this.generatedOn, marksheet.generatedOn) &&
        Objects.equals(this.generatedByUserId, marksheet.generatedByUserId) &&
        Objects.equals(this.status, marksheet.status) &&
        Objects.equals(this.totalPassed, marksheet.totalPassed) &&
        Objects.equals(this.totalFailed, marksheet.totalFailed) &&
        Objects.equals(this.createTime, marksheet.createTime) &&
        Objects.equals(this.createdBy, marksheet.createdBy) &&
        Objects.equals(this.updateTime, marksheet.updateTime) &&
        Objects.equals(this.updatedBy, marksheet.updatedBy) &&
        Objects.equals(this.etag, marksheet.etag);
  }

  @Override
  public int hashCode() {
    return Objects.hash(publishedAt, publishedByUserId, visibleToGuardians, id, examSessionId, resultTemplateId, name, generatedOn, generatedByUserId, status, totalPassed, totalFailed, createTime, createdBy, updateTime, updatedBy, etag);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class Marksheet {\n");
    sb.append("    publishedAt: ").append(toIndentedString(publishedAt)).append("\n");
    sb.append("    publishedByUserId: ").append(toIndentedString(publishedByUserId)).append("\n");
    sb.append("    visibleToGuardians: ").append(toIndentedString(visibleToGuardians)).append("\n");
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    examSessionId: ").append(toIndentedString(examSessionId)).append("\n");
    sb.append("    resultTemplateId: ").append(toIndentedString(resultTemplateId)).append("\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    generatedOn: ").append(toIndentedString(generatedOn)).append("\n");
    sb.append("    generatedByUserId: ").append(toIndentedString(generatedByUserId)).append("\n");
    sb.append("    status: ").append(toIndentedString(status)).append("\n");
    sb.append("    totalPassed: ").append(toIndentedString(totalPassed)).append("\n");
    sb.append("    totalFailed: ").append(toIndentedString(totalFailed)).append("\n");
    sb.append("    createTime: ").append(toIndentedString(createTime)).append("\n");
    sb.append("    createdBy: ").append(toIndentedString(createdBy)).append("\n");
    sb.append("    updateTime: ").append(toIndentedString(updateTime)).append("\n");
    sb.append("    updatedBy: ").append(toIndentedString(updatedBy)).append("\n");
    sb.append("    etag: ").append(toIndentedString(etag)).append("\n");
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

