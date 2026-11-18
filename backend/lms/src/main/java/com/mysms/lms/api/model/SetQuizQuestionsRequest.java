// Owned API contract of lms.
package com.mysms.lms.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.springframework.lang.Nullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;

/**
 * SetQuizQuestionsRequest
 */

public class SetQuizQuestionsRequest {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable List<UUID> addQuestionIds = new ArrayList<>();

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable List<UUID> removeQuestionIds = new ArrayList<>();

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID questionPoolId;

  public SetQuizQuestionsRequest addQuestionIds(List<UUID> addQuestionIds) {
    this.addQuestionIds = addQuestionIds;
    return this;
  }

  public SetQuizQuestionsRequest addAddQuestionIdsItem(UUID addQuestionIdsItem) {
    if (this.addQuestionIds == null) {
      this.addQuestionIds = new ArrayList<>();
    }
    this.addQuestionIds.add(addQuestionIdsItem);
    return this;
  }

  /**
   * Questions to add.
   * @return addQuestionIds
   */
  
  @JsonProperty("addQuestionIds")
  public List<UUID> getAddQuestionIds() {
    return addQuestionIds;
  }

  @JsonProperty("addQuestionIds")
  public void setAddQuestionIds(List<UUID> addQuestionIds) {
    this.addQuestionIds = addQuestionIds;
  }

  public SetQuizQuestionsRequest removeQuestionIds(List<UUID> removeQuestionIds) {
    this.removeQuestionIds = removeQuestionIds;
    return this;
  }

  public SetQuizQuestionsRequest addRemoveQuestionIdsItem(UUID removeQuestionIdsItem) {
    if (this.removeQuestionIds == null) {
      this.removeQuestionIds = new ArrayList<>();
    }
    this.removeQuestionIds.add(removeQuestionIdsItem);
    return this;
  }

  /**
   * Questions to remove.
   * @return removeQuestionIds
   */
  
  @JsonProperty("removeQuestionIds")
  public List<UUID> getRemoveQuestionIds() {
    return removeQuestionIds;
  }

  @JsonProperty("removeQuestionIds")
  public void setRemoveQuestionIds(List<UUID> removeQuestionIds) {
    this.removeQuestionIds = removeQuestionIds;
  }

  public SetQuizQuestionsRequest questionPoolId(@Nullable UUID questionPoolId) {
    this.questionPoolId = questionPoolId;
    return this;
  }

  /**
   * Add all questions of a pool.
   * @return questionPoolId
   */
  @Valid 
  @JsonProperty("questionPoolId")
  public @Nullable UUID getQuestionPoolId() {
    return questionPoolId;
  }

  @JsonProperty("questionPoolId")
  public void setQuestionPoolId(@Nullable UUID questionPoolId) {
    this.questionPoolId = questionPoolId;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    SetQuizQuestionsRequest setQuizQuestionsRequest = (SetQuizQuestionsRequest) o;
    return Objects.equals(this.addQuestionIds, setQuizQuestionsRequest.addQuestionIds) &&
        Objects.equals(this.removeQuestionIds, setQuizQuestionsRequest.removeQuestionIds) &&
        Objects.equals(this.questionPoolId, setQuizQuestionsRequest.questionPoolId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(addQuestionIds, removeQuestionIds, questionPoolId);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class SetQuizQuestionsRequest {\n");
    sb.append("    addQuestionIds: ").append(toIndentedString(addQuestionIds)).append("\n");
    sb.append("    removeQuestionIds: ").append(toIndentedString(removeQuestionIds)).append("\n");
    sb.append("    questionPoolId: ").append(toIndentedString(questionPoolId)).append("\n");
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

