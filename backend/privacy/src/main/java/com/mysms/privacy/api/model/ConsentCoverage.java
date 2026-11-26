// Owned API contract of privacy.
package com.mysms.privacy.api.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.lang.Nullable;

/**
 * How far a purpose is covered by consent in the tenant, or in one institution: counts per width, never
 * person ids.
 */

public class ConsentCoverage {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private String purposeCode;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID institutionId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private Integer subjects;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private List<ConsentCoverageScope> scopes = new ArrayList<>();

  public ConsentCoverage() {
    super();
  }

  public ConsentCoverage purposeCode(String purposeCode) {
    this.purposeCode = purposeCode;
    return this;
  }

  /**
   * The catalogue purpose.
   * @return purposeCode
   */
  @NotNull 
  @JsonProperty("purposeCode")
  public String getPurposeCode() {
    return purposeCode;
  }

  @JsonProperty("purposeCode")
  public void setPurposeCode(String purposeCode) {
    this.purposeCode = purposeCode;
  }

  public ConsentCoverage institutionId(@Nullable UUID institutionId) {
    this.institutionId = institutionId;
    return this;
  }

  /**
   * The institution the counts are narrowed to (its students); absent = every data principal of the tenant.
   * @return institutionId
   */
  @Valid 
  @JsonProperty("institutionId")
  public @Nullable UUID getInstitutionId() {
    return institutionId;
  }

  @JsonProperty("institutionId")
  public void setInstitutionId(@Nullable UUID institutionId) {
    this.institutionId = institutionId;
  }

  public ConsentCoverage subjects(Integer subjects) {
    this.subjects = subjects;
    return this;
  }

  /**
   * How many data principals are counted.
   * @return subjects
   */
  @NotNull 
  @JsonProperty("subjects")
  public Integer getSubjects() {
    return subjects;
  }

  @JsonProperty("subjects")
  public void setSubjects(Integer subjects) {
    this.subjects = subjects;
  }

  public ConsentCoverage scopes(List<ConsentCoverageScope> scopes) {
    this.scopes = scopes;
    return this;
  }

  public ConsentCoverage addScopesItem(ConsentCoverageScope scopesItem) {
    if (this.scopes == null) {
      this.scopes = new ArrayList<>();
    }
    this.scopes.add(scopesItem);
    return this;
  }

  /**
   * The counts per width the purpose takes (or the one asked for), narrowest first.
   * @return scopes
   */
  @NotNull 
  @JsonProperty("scopes")
  public List<@Valid ConsentCoverageScope> getScopes() {
    return scopes;
  }

  @JsonProperty("scopes")
  public void setScopes(List<ConsentCoverageScope> scopes) {
    this.scopes = scopes;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ConsentCoverage other = (ConsentCoverage) o;
    return Objects.equals(this.purposeCode, other.purposeCode) &&
        Objects.equals(this.institutionId, other.institutionId) &&
        Objects.equals(this.subjects, other.subjects) &&
        Objects.equals(this.scopes, other.scopes);
  }

  @Override
  public int hashCode() {
    return Objects.hash(purposeCode, institutionId, subjects, scopes);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ConsentCoverage {\n");
    sb.append("    purposeCode: ").append(toIndentedString(purposeCode)).append("\n");
    sb.append("    institutionId: ").append(toIndentedString(institutionId)).append("\n");
    sb.append("    subjects: ").append(toIndentedString(subjects)).append("\n");
    sb.append("    scopes: ").append(toIndentedString(scopes)).append("\n");
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
