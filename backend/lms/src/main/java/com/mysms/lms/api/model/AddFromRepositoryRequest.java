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
 * AddFromRepositoryRequest
 */

public class AddFromRepositoryRequest {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private UUID chapterId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private String itemKind;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private List<UUID> sourceIds = new ArrayList<>();

  public AddFromRepositoryRequest() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public AddFromRepositoryRequest(UUID chapterId, String itemKind, List<UUID> sourceIds) {
    this.chapterId = chapterId;
    this.itemKind = itemKind;
    this.sourceIds = sourceIds;
  }

  public AddFromRepositoryRequest chapterId(UUID chapterId) {
    this.chapterId = chapterId;
    return this;
  }

  /**
   * Chapter.
   * @return chapterId
   */
  @NotNull @Valid 
  @JsonProperty("chapterId")
  public UUID getChapterId() {
    return chapterId;
  }

  @JsonProperty("chapterId")
  public void setChapterId(UUID chapterId) {
    this.chapterId = chapterId;
  }

  public AddFromRepositoryRequest itemKind(String itemKind) {
    this.itemKind = itemKind;
    return this;
  }

  /**
   * Item kind.
   * @return itemKind
   */
  @NotNull 
  @JsonProperty("itemKind")
  public String getItemKind() {
    return itemKind;
  }

  @JsonProperty("itemKind")
  public void setItemKind(String itemKind) {
    this.itemKind = itemKind;
  }

  public AddFromRepositoryRequest sourceIds(List<UUID> sourceIds) {
    this.sourceIds = sourceIds;
    return this;
  }

  public AddFromRepositoryRequest addSourceIdsItem(UUID sourceIdsItem) {
    if (this.sourceIds == null) {
      this.sourceIds = new ArrayList<>();
    }
    this.sourceIds.add(sourceIdsItem);
    return this;
  }

  /**
   * Repository items (assignments, quizzes, prompts, resources...).
   * @return sourceIds
   */
  @NotNull 
  @JsonProperty("sourceIds")
  public List<UUID> getSourceIds() {
    return sourceIds;
  }

  @JsonProperty("sourceIds")
  public void setSourceIds(List<UUID> sourceIds) {
    this.sourceIds = sourceIds;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    AddFromRepositoryRequest addFromRepositoryRequest = (AddFromRepositoryRequest) o;
    return Objects.equals(this.chapterId, addFromRepositoryRequest.chapterId) &&
        Objects.equals(this.itemKind, addFromRepositoryRequest.itemKind) &&
        Objects.equals(this.sourceIds, addFromRepositoryRequest.sourceIds);
  }

  @Override
  public int hashCode() {
    return Objects.hash(chapterId, itemKind, sourceIds);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class AddFromRepositoryRequest {\n");
    sb.append("    chapterId: ").append(toIndentedString(chapterId)).append("\n");
    sb.append("    itemKind: ").append(toIndentedString(itemKind)).append("\n");
    sb.append("    sourceIds: ").append(toIndentedString(sourceIds)).append("\n");
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

