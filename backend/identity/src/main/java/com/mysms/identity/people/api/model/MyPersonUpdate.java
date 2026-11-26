// Owned API contract of people.
package com.mysms.identity.people.api.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import org.springframework.lang.Nullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;


import java.util.*;

/**
 * Fields of their own Person a user may change themselves (PATCH: a property left out keeps its value, null clears it, a value sets it). Names, identity and statutory fields are changed by administrators with people.UpdatePerson. The profile photo follows once uploads (platform.CreateUploadUrl) are served, so a user can only set a file they uploaded.
 */

public class MyPersonUpdate {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@Size(max = 320) String> primaryEmail;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@Size(max = 20) String> primaryPhone;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@Size(max = 20) String> alternatePhone;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@Size(max = 30) String> workPhone;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@Size(max = 500) String> websiteUrl;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<String> aboutMe;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Optional<@Size(max = 60) String> motherTongue;

  public MyPersonUpdate primaryEmail(@Nullable Optional<String> primaryEmail) {
    this.primaryEmail = primaryEmail;
    return this;
  }

  /**
   * Personal e-mail.
   * @return primaryEmail
   */
  
  @JsonProperty("primaryEmail")
  public @Nullable Optional<String> getPrimaryEmail() {
    return primaryEmail;
  }

  @JsonProperty("primaryEmail")
  public void setPrimaryEmail(@Nullable Optional<String> primaryEmail) {
    this.primaryEmail = primaryEmail;
  }

  public MyPersonUpdate primaryPhone(@Nullable Optional<String> primaryPhone) {
    this.primaryPhone = primaryPhone;
    return this;
  }

  /**
   * Personal phone.
   * @return primaryPhone
   */
  
  @JsonProperty("primaryPhone")
  public @Nullable Optional<String> getPrimaryPhone() {
    return primaryPhone;
  }

  @JsonProperty("primaryPhone")
  public void setPrimaryPhone(@Nullable Optional<String> primaryPhone) {
    this.primaryPhone = primaryPhone;
  }

  public MyPersonUpdate alternatePhone(@Nullable Optional<String> alternatePhone) {
    this.alternatePhone = alternatePhone;
    return this;
  }

  /**
   * Alternate phone.
   * @return alternatePhone
   */
  
  @JsonProperty("alternatePhone")
  public @Nullable Optional<String> getAlternatePhone() {
    return alternatePhone;
  }

  @JsonProperty("alternatePhone")
  public void setAlternatePhone(@Nullable Optional<String> alternatePhone) {
    this.alternatePhone = alternatePhone;
  }

  public MyPersonUpdate workPhone(@Nullable Optional<String> workPhone) {
    this.workPhone = workPhone;
    return this;
  }

  /**
   * Office / work phone.
   * @return workPhone
   */
  
  @JsonProperty("workPhone")
  public @Nullable Optional<String> getWorkPhone() {
    return workPhone;
  }

  @JsonProperty("workPhone")
  public void setWorkPhone(@Nullable Optional<String> workPhone) {
    this.workPhone = workPhone;
  }

  public MyPersonUpdate websiteUrl(@Nullable Optional<String> websiteUrl) {
    this.websiteUrl = websiteUrl;
    return this;
  }

  /**
   * Personal web page.
   * @return websiteUrl
   */
  
  @JsonProperty("websiteUrl")
  public @Nullable Optional<String> getWebsiteUrl() {
    return websiteUrl;
  }

  @JsonProperty("websiteUrl")
  public void setWebsiteUrl(@Nullable Optional<String> websiteUrl) {
    this.websiteUrl = websiteUrl;
  }

  public MyPersonUpdate aboutMe(@Nullable Optional<String> aboutMe) {
    this.aboutMe = aboutMe;
    return this;
  }

  /**
   * Short self-description shown on the profile.
   * @return aboutMe
   */
  
  @JsonProperty("aboutMe")
  public @Nullable Optional<String> getAboutMe() {
    return aboutMe;
  }

  @JsonProperty("aboutMe")
  public void setAboutMe(@Nullable Optional<String> aboutMe) {
    this.aboutMe = aboutMe;
  }

  public MyPersonUpdate motherTongue(@Nullable Optional<String> motherTongue) {
    this.motherTongue = motherTongue;
    return this;
  }

  /**
   * Mother tongue.
   * @return motherTongue
   */
  
  @JsonProperty("motherTongue")
  public @Nullable Optional<String> getMotherTongue() {
    return motherTongue;
  }

  @JsonProperty("motherTongue")
  public void setMotherTongue(@Nullable Optional<String> motherTongue) {
    this.motherTongue = motherTongue;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    MyPersonUpdate myPersonUpdate = (MyPersonUpdate) o;
    return Objects.equals(this.primaryEmail, myPersonUpdate.primaryEmail) &&
        Objects.equals(this.primaryPhone, myPersonUpdate.primaryPhone) &&
        Objects.equals(this.alternatePhone, myPersonUpdate.alternatePhone) &&
        Objects.equals(this.workPhone, myPersonUpdate.workPhone) &&
        Objects.equals(this.websiteUrl, myPersonUpdate.websiteUrl) &&
        Objects.equals(this.aboutMe, myPersonUpdate.aboutMe) &&
        Objects.equals(this.motherTongue, myPersonUpdate.motherTongue);
  }

  @Override
  public int hashCode() {
    return Objects.hash(primaryEmail, primaryPhone, alternatePhone, workPhone, websiteUrl, aboutMe, motherTongue);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class MyPersonUpdate {\n");
    sb.append("    primaryEmail: ").append(toIndentedString(primaryEmail)).append("\n");
    sb.append("    primaryPhone: ").append(toIndentedString(primaryPhone)).append("\n");
    sb.append("    alternatePhone: ").append(toIndentedString(alternatePhone)).append("\n");
    sb.append("    workPhone: ").append(toIndentedString(workPhone)).append("\n");
    sb.append("    websiteUrl: ").append(toIndentedString(websiteUrl)).append("\n");
    sb.append("    aboutMe: ").append(toIndentedString(aboutMe)).append("\n");
    sb.append("    motherTongue: ").append(toIndentedString(motherTongue)).append("\n");
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

