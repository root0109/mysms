// Owned API contract of people.
package com.mysms.identity.people.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.lang.Nullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;

/**
 * Fields of WorkExperience that can be updated (PATCH: a property left out keeps its value, null clears it, a value sets it).
 */

public class WorkExperienceUpdate {

  /**
   * Job or internship.
   */
  public enum ExperienceTypeEnum {
    JOB("job"),
    
    INTERNSHIP("internship");

    private final String value;

    ExperienceTypeEnum(String value) {
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
    public static ExperienceTypeEnum fromValue(String value) {
      for (ExperienceTypeEnum b : ExperienceTypeEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull ExperienceTypeEnum> experienceType;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull @Size(max = 150) String> title;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull @Size(max = 200) String> company;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull @Size(max = 150) String> location;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  private @Nullable Optional<@NotNull LocalDate> fromDate;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  private @Nullable Optional<LocalDate> toDate;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull Boolean> isCurrent;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<String> description;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@Size(max = 500) String> companyWebsite;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@Size(max = 150) String> industry;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<String> responsibilities;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull Integer> sortOrder;

  public WorkExperienceUpdate experienceType(@Nullable Optional<ExperienceTypeEnum> experienceType) {
    this.experienceType = experienceType;
    return this;
  }

  /**
   * Job or internship.
   * @return experienceType
   */
  
  @JsonProperty("experienceType")
  public @Nullable Optional<ExperienceTypeEnum> getExperienceType() {
    return experienceType;
  }

  @JsonProperty("experienceType")
  public void setExperienceType(@Nullable Optional<ExperienceTypeEnum> experienceType) {
    this.experienceType = experienceType;
  }

  public WorkExperienceUpdate title(@Nullable Optional<String> title) {
    this.title = title;
    return this;
  }

  /**
   * Job title.
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

  public WorkExperienceUpdate company(@Nullable Optional<String> company) {
    this.company = company;
    return this;
  }

  /**
   * Employer.
   * @return company
   */
  
  @JsonProperty("company")
  public @Nullable Optional<String> getCompany() {
    return company;
  }

  @JsonProperty("company")
  public void setCompany(@Nullable Optional<String> company) {
    this.company = company;
  }

  public WorkExperienceUpdate location(@Nullable Optional<String> location) {
    this.location = location;
    return this;
  }

  /**
   * Location.
   * @return location
   */
  
  @JsonProperty("location")
  public @Nullable Optional<String> getLocation() {
    return location;
  }

  @JsonProperty("location")
  public void setLocation(@Nullable Optional<String> location) {
    this.location = location;
  }

  public WorkExperienceUpdate fromDate(@Nullable Optional<LocalDate> fromDate) {
    this.fromDate = fromDate;
    return this;
  }

  /**
   * Start date.
   * @return fromDate
   */
  
  @JsonProperty("fromDate")
  public @Nullable Optional<LocalDate> getFromDate() {
    return fromDate;
  }

  @JsonProperty("fromDate")
  public void setFromDate(@Nullable Optional<LocalDate> fromDate) {
    this.fromDate = fromDate;
  }

  public WorkExperienceUpdate toDate(@Nullable Optional<LocalDate> toDate) {
    this.toDate = toDate;
    return this;
  }

  /**
   * End date.
   * @return toDate
   */
  
  @JsonProperty("toDate")
  public @Nullable Optional<LocalDate> getToDate() {
    return toDate;
  }

  @JsonProperty("toDate")
  public void setToDate(@Nullable Optional<LocalDate> toDate) {
    this.toDate = toDate;
  }

  public WorkExperienceUpdate isCurrent(@Nullable Optional<Boolean> isCurrent) {
    this.isCurrent = isCurrent;
    return this;
  }

  /**
   * Currently in this role.
   * @return isCurrent
   */
  
  @JsonProperty("isCurrent")
  public @Nullable Optional<Boolean> getIsCurrent() {
    return isCurrent;
  }

  @JsonProperty("isCurrent")
  public void setIsCurrent(@Nullable Optional<Boolean> isCurrent) {
    this.isCurrent = isCurrent;
  }

  public WorkExperienceUpdate description(@Nullable Optional<String> description) {
    this.description = description;
    return this;
  }

  /**
   * Responsibilities / achievements.
   * @return description
   */
  
  @JsonProperty("description")
  public @Nullable Optional<String> getDescription() {
    return description;
  }

  @JsonProperty("description")
  public void setDescription(@Nullable Optional<String> description) {
    this.description = description;
  }

  public WorkExperienceUpdate companyWebsite(@Nullable Optional<String> companyWebsite) {
    this.companyWebsite = companyWebsite;
    return this;
  }

  /**
   * Company web site.
   * @return companyWebsite
   */
  
  @JsonProperty("companyWebsite")
  public @Nullable Optional<String> getCompanyWebsite() {
    return companyWebsite;
  }

  @JsonProperty("companyWebsite")
  public void setCompanyWebsite(@Nullable Optional<String> companyWebsite) {
    this.companyWebsite = companyWebsite;
  }

  public WorkExperienceUpdate industry(@Nullable Optional<String> industry) {
    this.industry = industry;
    return this;
  }

  /**
   * Industry.
   * @return industry
   */
  
  @JsonProperty("industry")
  public @Nullable Optional<String> getIndustry() {
    return industry;
  }

  @JsonProperty("industry")
  public void setIndustry(@Nullable Optional<String> industry) {
    this.industry = industry;
  }

  public WorkExperienceUpdate responsibilities(@Nullable Optional<String> responsibilities) {
    this.responsibilities = responsibilities;
    return this;
  }

  /**
   * Responsibilities.
   * @return responsibilities
   */
  
  @JsonProperty("responsibilities")
  public @Nullable Optional<String> getResponsibilities() {
    return responsibilities;
  }

  @JsonProperty("responsibilities")
  public void setResponsibilities(@Nullable Optional<String> responsibilities) {
    this.responsibilities = responsibilities;
  }

  public WorkExperienceUpdate sortOrder(@Nullable Optional<Integer> sortOrder) {
    this.sortOrder = sortOrder;
    return this;
  }

  /**
   * Display order.
   * @return sortOrder
   */
  
  @JsonProperty("sortOrder")
  public @Nullable Optional<Integer> getSortOrder() {
    return sortOrder;
  }

  @JsonProperty("sortOrder")
  public void setSortOrder(@Nullable Optional<Integer> sortOrder) {
    this.sortOrder = sortOrder;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    WorkExperienceUpdate workExperienceUpdate = (WorkExperienceUpdate) o;
    return Objects.equals(this.experienceType, workExperienceUpdate.experienceType) &&
        Objects.equals(this.title, workExperienceUpdate.title) &&
        Objects.equals(this.company, workExperienceUpdate.company) &&
        Objects.equals(this.location, workExperienceUpdate.location) &&
        Objects.equals(this.fromDate, workExperienceUpdate.fromDate) &&
        Objects.equals(this.toDate, workExperienceUpdate.toDate) &&
        Objects.equals(this.isCurrent, workExperienceUpdate.isCurrent) &&
        Objects.equals(this.description, workExperienceUpdate.description) &&
        Objects.equals(this.companyWebsite, workExperienceUpdate.companyWebsite) &&
        Objects.equals(this.industry, workExperienceUpdate.industry) &&
        Objects.equals(this.responsibilities, workExperienceUpdate.responsibilities) &&
        Objects.equals(this.sortOrder, workExperienceUpdate.sortOrder);
  }

  @Override
  public int hashCode() {
    return Objects.hash(experienceType, title, company, location, fromDate, toDate, isCurrent, description, companyWebsite, industry, responsibilities, sortOrder);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class WorkExperienceUpdate {\n");
    sb.append("    experienceType: ").append(toIndentedString(experienceType)).append("\n");
    sb.append("    title: ").append(toIndentedString(title)).append("\n");
    sb.append("    company: ").append(toIndentedString(company)).append("\n");
    sb.append("    location: ").append(toIndentedString(location)).append("\n");
    sb.append("    fromDate: ").append(toIndentedString(fromDate)).append("\n");
    sb.append("    toDate: ").append(toIndentedString(toDate)).append("\n");
    sb.append("    isCurrent: ").append(toIndentedString(isCurrent)).append("\n");
    sb.append("    description: ").append(toIndentedString(description)).append("\n");
    sb.append("    companyWebsite: ").append(toIndentedString(companyWebsite)).append("\n");
    sb.append("    industry: ").append(toIndentedString(industry)).append("\n");
    sb.append("    responsibilities: ").append(toIndentedString(responsibilities)).append("\n");
    sb.append("    sortOrder: ").append(toIndentedString(sortOrder)).append("\n");
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

