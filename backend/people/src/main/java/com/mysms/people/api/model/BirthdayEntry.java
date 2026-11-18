// Owned API contract of people.
package com.mysms.people.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.lang.Nullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;

/**
 * BirthdayEntry
 */

public class BirthdayEntry {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private UUID personId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private String displayName;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  private LocalDate date;

  /**
   * Occasion.
   */
  public enum KindEnum {
    BIRTHDAY("birthday"),
    
    WORK_ANNIVERSARY("work_anniversary");

    private final String value;

    KindEnum(String value) {
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
    public static KindEnum fromValue(String value) {
      for (KindEnum b : KindEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private KindEnum kind;

  /**
   * Role.
   */
  public enum RoleEnum {
    STUDENT("student"),
    
    STAFF("staff"),
    
    GUARDIAN("guardian");

    private final String value;

    RoleEnum(String value) {
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
    public static RoleEnum fromValue(String value) {
      for (RoleEnum b : RoleEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private RoleEnum role;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Integer years;

  public BirthdayEntry() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public BirthdayEntry(UUID personId, String displayName, LocalDate date, KindEnum kind, RoleEnum role) {
    this.personId = personId;
    this.displayName = displayName;
    this.date = date;
    this.kind = kind;
    this.role = role;
  }

  public BirthdayEntry personId(UUID personId) {
    this.personId = personId;
    return this;
  }

  /**
   * Person.
   * @return personId
   */
  @NotNull @Valid 
  @JsonProperty("personId")
  public UUID getPersonId() {
    return personId;
  }

  @JsonProperty("personId")
  public void setPersonId(UUID personId) {
    this.personId = personId;
  }

  public BirthdayEntry displayName(String displayName) {
    this.displayName = displayName;
    return this;
  }

  /**
   * Name.
   * @return displayName
   */
  @NotNull 
  @JsonProperty("displayName")
  public String getDisplayName() {
    return displayName;
  }

  @JsonProperty("displayName")
  public void setDisplayName(String displayName) {
    this.displayName = displayName;
  }

  public BirthdayEntry date(LocalDate date) {
    this.date = date;
    return this;
  }

  /**
   * Birthday / joining date.
   * @return date
   */
  @NotNull @Valid 
  @JsonProperty("date")
  public LocalDate getDate() {
    return date;
  }

  @JsonProperty("date")
  public void setDate(LocalDate date) {
    this.date = date;
  }

  public BirthdayEntry kind(KindEnum kind) {
    this.kind = kind;
    return this;
  }

  /**
   * Occasion.
   * @return kind
   */
  @NotNull 
  @JsonProperty("kind")
  public KindEnum getKind() {
    return kind;
  }

  @JsonProperty("kind")
  public void setKind(KindEnum kind) {
    this.kind = kind;
  }

  public BirthdayEntry role(RoleEnum role) {
    this.role = role;
    return this;
  }

  /**
   * Role.
   * @return role
   */
  @NotNull 
  @JsonProperty("role")
  public RoleEnum getRole() {
    return role;
  }

  @JsonProperty("role")
  public void setRole(RoleEnum role) {
    this.role = role;
  }

  public BirthdayEntry years(@Nullable Integer years) {
    this.years = years;
    return this;
  }

  /**
   * Age / years of service.
   * @return years
   */
  
  @JsonProperty("years")
  public @Nullable Integer getYears() {
    return years;
  }

  @JsonProperty("years")
  public void setYears(@Nullable Integer years) {
    this.years = years;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    BirthdayEntry birthdayEntry = (BirthdayEntry) o;
    return Objects.equals(this.personId, birthdayEntry.personId) &&
        Objects.equals(this.displayName, birthdayEntry.displayName) &&
        Objects.equals(this.date, birthdayEntry.date) &&
        Objects.equals(this.kind, birthdayEntry.kind) &&
        Objects.equals(this.role, birthdayEntry.role) &&
        Objects.equals(this.years, birthdayEntry.years);
  }

  @Override
  public int hashCode() {
    return Objects.hash(personId, displayName, date, kind, role, years);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class BirthdayEntry {\n");
    sb.append("    personId: ").append(toIndentedString(personId)).append("\n");
    sb.append("    displayName: ").append(toIndentedString(displayName)).append("\n");
    sb.append("    date: ").append(toIndentedString(date)).append("\n");
    sb.append("    kind: ").append(toIndentedString(kind)).append("\n");
    sb.append("    role: ").append(toIndentedString(role)).append("\n");
    sb.append("    years: ").append(toIndentedString(years)).append("\n");
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

