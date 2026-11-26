// Owned API contract of lms.
package com.mysms.learning.lms.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import org.springframework.lang.Nullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;

/**
 * Fields of ProgrammingExercise that can be updated (PATCH: a property left out keeps its value, null clears it, a value sets it).
 */

public class ProgrammingExerciseUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull @Size(max = 200) String> title;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull String> problemStatement;

  /**
   * Programming language.
   */
  public enum LanguageEnum {
    PYTHON("python"),
    
    JAVASCRIPT("javascript"),
    
    RUST("rust"),
    
    GO("go");

    private final String value;

    LanguageEnum(String value) {
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
    public static LanguageEnum fromValue(String value) {
      for (LanguageEnum b : LanguageEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull LanguageEnum> language;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<String> starterCode;

  public ProgrammingExerciseUpdate title(@Nullable Optional<String> title) {
    this.title = title;
    return this;
  }

  /**
   * Title.
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

  public ProgrammingExerciseUpdate problemStatement(@Nullable Optional<String> problemStatement) {
    this.problemStatement = problemStatement;
    return this;
  }

  /**
   * Problem statement.
   * @return problemStatement
   */
  
  @JsonProperty("problemStatement")
  public @Nullable Optional<String> getProblemStatement() {
    return problemStatement;
  }

  @JsonProperty("problemStatement")
  public void setProblemStatement(@Nullable Optional<String> problemStatement) {
    this.problemStatement = problemStatement;
  }

  public ProgrammingExerciseUpdate language(@Nullable Optional<LanguageEnum> language) {
    this.language = language;
    return this;
  }

  /**
   * Programming language.
   * @return language
   */
  
  @JsonProperty("language")
  public @Nullable Optional<LanguageEnum> getLanguage() {
    return language;
  }

  @JsonProperty("language")
  public void setLanguage(@Nullable Optional<LanguageEnum> language) {
    this.language = language;
  }

  public ProgrammingExerciseUpdate starterCode(@Nullable Optional<String> starterCode) {
    this.starterCode = starterCode;
    return this;
  }

  /**
   * Starter code.
   * @return starterCode
   */
  
  @JsonProperty("starterCode")
  public @Nullable Optional<String> getStarterCode() {
    return starterCode;
  }

  @JsonProperty("starterCode")
  public void setStarterCode(@Nullable Optional<String> starterCode) {
    this.starterCode = starterCode;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ProgrammingExerciseUpdate programmingExerciseUpdate = (ProgrammingExerciseUpdate) o;
    return Objects.equals(this.title, programmingExerciseUpdate.title) &&
        Objects.equals(this.problemStatement, programmingExerciseUpdate.problemStatement) &&
        Objects.equals(this.language, programmingExerciseUpdate.language) &&
        Objects.equals(this.starterCode, programmingExerciseUpdate.starterCode);
  }

  @Override
  public int hashCode() {
    return Objects.hash(title, problemStatement, language, starterCode);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ProgrammingExerciseUpdate {\n");
    sb.append("    title: ").append(toIndentedString(title)).append("\n");
    sb.append("    problemStatement: ").append(toIndentedString(problemStatement)).append("\n");
    sb.append("    language: ").append(toIndentedString(language)).append("\n");
    sb.append("    starterCode: ").append(toIndentedString(starterCode)).append("\n");
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

