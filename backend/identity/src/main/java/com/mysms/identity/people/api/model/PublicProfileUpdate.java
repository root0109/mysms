// Owned API contract of people.
package com.mysms.identity.people.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.UUID;
import org.springframework.lang.Nullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;

/**
 * Fields of PublicProfile that can be updated (PATCH: a property left out keeps its value, null clears it, a value sets it).
 */

public class PublicProfileUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@Size(max = 200) String> headline;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@Size(max = 120) String> profession;

  /**
   * Self-identified persona.
   */
  public enum PersonaCategoryEnum {
    BUSINESS_OWNER("business_owner"),
    
    MANAGER("manager"),
    
    EMPLOYEE("employee"),
    
    STUDENT("student"),
    
    FREELANCER("freelancer"),
    
    OTHER("other");

    private final String value;

    PersonaCategoryEnum(String value) {
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
    public static PersonaCategoryEnum fromValue(String value) {
      for (PersonaCategoryEnum b : PersonaCategoryEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<PersonaCategoryEnum> personaCategory;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<UUID> coverImageFileId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@Size(max = 120) String> city;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<UUID> countryId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@Size(max = 200) String> college;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@Size(max = 120) String> branch;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@Size(max = 500) String> linkedinUrl;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@Size(max = 500) String> githubUrl;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@Size(max = 500) String> twitterUrl;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@Size(max = 500) String> mediumUrl;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@NotNull Boolean> hidePrivateInfo;

  public PublicProfileUpdate headline(@Nullable Optional<String> headline) {
    this.headline = headline;
    return this;
  }

  /**
   * One-line headline.
   * @return headline
   */
  
  @JsonProperty("headline")
  public @Nullable Optional<String> getHeadline() {
    return headline;
  }

  @JsonProperty("headline")
  public void setHeadline(@Nullable Optional<String> headline) {
    this.headline = headline;
  }

  public PublicProfileUpdate profession(@Nullable Optional<String> profession) {
    this.profession = profession;
    return this;
  }

  /**
   * Profession.
   * @return profession
   */
  
  @JsonProperty("profession")
  public @Nullable Optional<String> getProfession() {
    return profession;
  }

  @JsonProperty("profession")
  public void setProfession(@Nullable Optional<String> profession) {
    this.profession = profession;
  }

  public PublicProfileUpdate personaCategory(@Nullable Optional<PersonaCategoryEnum> personaCategory) {
    this.personaCategory = personaCategory;
    return this;
  }

  /**
   * Self-identified persona.
   * @return personaCategory
   */
  
  @JsonProperty("personaCategory")
  public @Nullable Optional<PersonaCategoryEnum> getPersonaCategory() {
    return personaCategory;
  }

  @JsonProperty("personaCategory")
  public void setPersonaCategory(@Nullable Optional<PersonaCategoryEnum> personaCategory) {
    this.personaCategory = personaCategory;
  }

  public PublicProfileUpdate coverImageFileId(@Nullable Optional<UUID> coverImageFileId) {
    this.coverImageFileId = coverImageFileId;
    return this;
  }

  /**
   * Profile cover image.
   * @return coverImageFileId
   */
  
  @JsonProperty("coverImageFileId")
  public @Nullable Optional<UUID> getCoverImageFileId() {
    return coverImageFileId;
  }

  @JsonProperty("coverImageFileId")
  public void setCoverImageFileId(@Nullable Optional<UUID> coverImageFileId) {
    this.coverImageFileId = coverImageFileId;
  }

  public PublicProfileUpdate city(@Nullable Optional<String> city) {
    this.city = city;
    return this;
  }

  /**
   * City.
   * @return city
   */
  
  @JsonProperty("city")
  public @Nullable Optional<String> getCity() {
    return city;
  }

  @JsonProperty("city")
  public void setCity(@Nullable Optional<String> city) {
    this.city = city;
  }

  public PublicProfileUpdate countryId(@Nullable Optional<UUID> countryId) {
    this.countryId = countryId;
    return this;
  }

  /**
   * Country.
   * @return countryId
   */
  
  @JsonProperty("countryId")
  public @Nullable Optional<UUID> getCountryId() {
    return countryId;
  }

  @JsonProperty("countryId")
  public void setCountryId(@Nullable Optional<UUID> countryId) {
    this.countryId = countryId;
  }

  public PublicProfileUpdate college(@Nullable Optional<String> college) {
    this.college = college;
    return this;
  }

  /**
   * College.
   * @return college
   */
  
  @JsonProperty("college")
  public @Nullable Optional<String> getCollege() {
    return college;
  }

  @JsonProperty("college")
  public void setCollege(@Nullable Optional<String> college) {
    this.college = college;
  }

  public PublicProfileUpdate branch(@Nullable Optional<String> branch) {
    this.branch = branch;
    return this;
  }

  /**
   * Branch / stream.
   * @return branch
   */
  
  @JsonProperty("branch")
  public @Nullable Optional<String> getBranch() {
    return branch;
  }

  @JsonProperty("branch")
  public void setBranch(@Nullable Optional<String> branch) {
    this.branch = branch;
  }

  public PublicProfileUpdate linkedinUrl(@Nullable Optional<String> linkedinUrl) {
    this.linkedinUrl = linkedinUrl;
    return this;
  }

  /**
   * LinkedIn URL.
   * @return linkedinUrl
   */
  
  @JsonProperty("linkedinUrl")
  public @Nullable Optional<String> getLinkedinUrl() {
    return linkedinUrl;
  }

  @JsonProperty("linkedinUrl")
  public void setLinkedinUrl(@Nullable Optional<String> linkedinUrl) {
    this.linkedinUrl = linkedinUrl;
  }

  public PublicProfileUpdate githubUrl(@Nullable Optional<String> githubUrl) {
    this.githubUrl = githubUrl;
    return this;
  }

  /**
   * GitHub URL.
   * @return githubUrl
   */
  
  @JsonProperty("githubUrl")
  public @Nullable Optional<String> getGithubUrl() {
    return githubUrl;
  }

  @JsonProperty("githubUrl")
  public void setGithubUrl(@Nullable Optional<String> githubUrl) {
    this.githubUrl = githubUrl;
  }

  public PublicProfileUpdate twitterUrl(@Nullable Optional<String> twitterUrl) {
    this.twitterUrl = twitterUrl;
    return this;
  }

  /**
   * X / Twitter URL.
   * @return twitterUrl
   */
  
  @JsonProperty("twitterUrl")
  public @Nullable Optional<String> getTwitterUrl() {
    return twitterUrl;
  }

  @JsonProperty("twitterUrl")
  public void setTwitterUrl(@Nullable Optional<String> twitterUrl) {
    this.twitterUrl = twitterUrl;
  }

  public PublicProfileUpdate mediumUrl(@Nullable Optional<String> mediumUrl) {
    this.mediumUrl = mediumUrl;
    return this;
  }

  /**
   * Medium URL.
   * @return mediumUrl
   */
  
  @JsonProperty("mediumUrl")
  public @Nullable Optional<String> getMediumUrl() {
    return mediumUrl;
  }

  @JsonProperty("mediumUrl")
  public void setMediumUrl(@Nullable Optional<String> mediumUrl) {
    this.mediumUrl = mediumUrl;
  }

  public PublicProfileUpdate hidePrivateInfo(@Nullable Optional<Boolean> hidePrivateInfo) {
    this.hidePrivateInfo = hidePrivateInfo;
    return this;
  }

  /**
   * Hide contact details from the public profile.
   * @return hidePrivateInfo
   */
  
  @JsonProperty("hidePrivateInfo")
  public @Nullable Optional<Boolean> getHidePrivateInfo() {
    return hidePrivateInfo;
  }

  @JsonProperty("hidePrivateInfo")
  public void setHidePrivateInfo(@Nullable Optional<Boolean> hidePrivateInfo) {
    this.hidePrivateInfo = hidePrivateInfo;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    PublicProfileUpdate publicProfileUpdate = (PublicProfileUpdate) o;
    return Objects.equals(this.headline, publicProfileUpdate.headline) &&
        Objects.equals(this.profession, publicProfileUpdate.profession) &&
        Objects.equals(this.personaCategory, publicProfileUpdate.personaCategory) &&
        Objects.equals(this.coverImageFileId, publicProfileUpdate.coverImageFileId) &&
        Objects.equals(this.city, publicProfileUpdate.city) &&
        Objects.equals(this.countryId, publicProfileUpdate.countryId) &&
        Objects.equals(this.college, publicProfileUpdate.college) &&
        Objects.equals(this.branch, publicProfileUpdate.branch) &&
        Objects.equals(this.linkedinUrl, publicProfileUpdate.linkedinUrl) &&
        Objects.equals(this.githubUrl, publicProfileUpdate.githubUrl) &&
        Objects.equals(this.twitterUrl, publicProfileUpdate.twitterUrl) &&
        Objects.equals(this.mediumUrl, publicProfileUpdate.mediumUrl) &&
        Objects.equals(this.hidePrivateInfo, publicProfileUpdate.hidePrivateInfo);
  }

  @Override
  public int hashCode() {
    return Objects.hash(headline, profession, personaCategory, coverImageFileId, city, countryId, college, branch, linkedinUrl, githubUrl, twitterUrl, mediumUrl, hidePrivateInfo);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class PublicProfileUpdate {\n");
    sb.append("    headline: ").append(toIndentedString(headline)).append("\n");
    sb.append("    profession: ").append(toIndentedString(profession)).append("\n");
    sb.append("    personaCategory: ").append(toIndentedString(personaCategory)).append("\n");
    sb.append("    coverImageFileId: ").append(toIndentedString(coverImageFileId)).append("\n");
    sb.append("    city: ").append(toIndentedString(city)).append("\n");
    sb.append("    countryId: ").append(toIndentedString(countryId)).append("\n");
    sb.append("    college: ").append(toIndentedString(college)).append("\n");
    sb.append("    branch: ").append(toIndentedString(branch)).append("\n");
    sb.append("    linkedinUrl: ").append(toIndentedString(linkedinUrl)).append("\n");
    sb.append("    githubUrl: ").append(toIndentedString(githubUrl)).append("\n");
    sb.append("    twitterUrl: ").append(toIndentedString(twitterUrl)).append("\n");
    sb.append("    mediumUrl: ").append(toIndentedString(mediumUrl)).append("\n");
    sb.append("    hidePrivateInfo: ").append(toIndentedString(hidePrivateInfo)).append("\n");
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

