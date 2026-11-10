// Owned API contract of lms (JAVA-CODING-RULES §7); first materialized from api/openapi/lms.yaml on 2026-11-10.
package com.mysms.lms.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.mysms.lms.api.model.QuizQuestion;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.springframework.lang.Nullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;

/**
 * QuizQuestionList
 */

public class QuizQuestionList {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private List<@Valid QuizQuestion> items = new ArrayList<>();

  public QuizQuestionList() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public QuizQuestionList(List<@Valid QuizQuestion> items) {
    this.items = items;
  }

  public QuizQuestionList items(List<@Valid QuizQuestion> items) {
    this.items = items;
    return this;
  }

  public QuizQuestionList addItemsItem(QuizQuestion itemsItem) {
    if (this.items == null) {
      this.items = new ArrayList<>();
    }
    this.items.add(itemsItem);
    return this;
  }

  /**
   * Get items
   * @return items
   */
  @NotNull 
  @JsonProperty("items")
  public List<@Valid QuizQuestion> getItems() {
    return items;
  }

  @JsonProperty("items")
  public void setItems(List<@Valid QuizQuestion> items) {
    this.items = items;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    QuizQuestionList quizQuestionList = (QuizQuestionList) o;
    return Objects.equals(this.items, quizQuestionList.items);
  }

  @Override
  public int hashCode() {
    return Objects.hash(items);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class QuizQuestionList {\n");
    sb.append("    items: ").append(toIndentedString(items)).append("\n");
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

