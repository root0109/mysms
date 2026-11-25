// Owned API contract of privacy.
package com.mysms.privacy.api.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.lang.Nullable;

/**
 * One entry of the processing register (RoPA-lite): what a module does with personal data, for which purpose and on
 * which legal basis, whether it reaches minors, and which data categories it touches. Platform catalogue, read-only.
 */

public class ProcessingActivity {

  /**
   * The legal basis of the processing.
   */
  public enum LegalBasisEnum {
    CONSENT("consent"),
    
    CONTRACT("contract"),
    
    EDUCATION_EXEMPTION("education_exemption"),
    
    EMPLOYMENT("employment"),
    
    LEGAL_OBLIGATION("legal_obligation"),
    
    LEGITIMATE_USE("legitimate_use"),
    
    LEGITIMATE_USE_VOLUNTARY("legitimate_use_voluntary"),
    
    MEDICAL_EMERGENCY("medical_emergency");

    private final String value;

    LegalBasisEnum(String value) {
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
    public static LegalBasisEnum fromValue(String value) {
      for (LegalBasisEnum b : LegalBasisEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID id;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String module;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String code;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String purposeCode;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable LegalBasisEnum legalBasis;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String educationExemptionClause;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Boolean targetsMinors;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Boolean crossBorder;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String retentionPolicyCode;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable Integer version;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private List<String> categories = new ArrayList<>();

  public ProcessingActivity id(@Nullable UUID id) {
    this.id = id;
    return this;
  }

  /**
   * Stable id of the activity, the same in every environment.
   * @return id
   */
  
  @JsonProperty("id")
  public @Nullable UUID getId() {
    return id;
  }

  @JsonProperty("id")
  public void setId(@Nullable UUID id) {
    this.id = id;
  }

  public ProcessingActivity module(@Nullable String module) {
    this.module = module;
    return this;
  }

  /**
   * The module that processes the data.
   * @return module
   */
  
  @JsonProperty("module")
  public @Nullable String getModule() {
    return module;
  }

  @JsonProperty("module")
  public void setModule(@Nullable String module) {
    this.module = module;
  }

  public ProcessingActivity code(@Nullable String code) {
    this.code = code;
    return this;
  }

  /**
   * Activity code {@code <module>.<activity>}, e.g. comms.marketing_broadcast.
   * @return code
   */
  
  @JsonProperty("code")
  public @Nullable String getCode() {
    return code;
  }

  @JsonProperty("code")
  public void setCode(@Nullable String code) {
    this.code = code;
  }

  public ProcessingActivity purposeCode(@Nullable String purposeCode) {
    this.purposeCode = purposeCode;
    return this;
  }

  /**
   * Code of the catalogue purpose the activity serves.
   * @return purposeCode
   */
  
  @JsonProperty("purposeCode")
  public @Nullable String getPurposeCode() {
    return purposeCode;
  }

  @JsonProperty("purposeCode")
  public void setPurposeCode(@Nullable String purposeCode) {
    this.purposeCode = purposeCode;
  }

  public ProcessingActivity legalBasis(@Nullable LegalBasisEnum legalBasis) {
    this.legalBasis = legalBasis;
    return this;
  }

  /**
   * The legal basis of the processing.
   * @return legalBasis
   */
  
  @JsonProperty("legalBasis")
  public @Nullable LegalBasisEnum getLegalBasis() {
    return legalBasis;
  }

  @JsonProperty("legalBasis")
  public void setLegalBasis(@Nullable LegalBasisEnum legalBasis) {
    this.legalBasis = legalBasis;
  }

  public ProcessingActivity educationExemptionClause(@Nullable String educationExemptionClause) {
    this.educationExemptionClause = educationExemptionClause;
    return this;
  }

  /**
   * The Fourth Schedule clause relied on when the basis is education_exemption.
   * @return educationExemptionClause
   */
  
  @JsonProperty("educationExemptionClause")
  public @Nullable String getEducationExemptionClause() {
    return educationExemptionClause;
  }

  @JsonProperty("educationExemptionClause")
  public void setEducationExemptionClause(@Nullable String educationExemptionClause) {
    this.educationExemptionClause = educationExemptionClause;
  }

  public ProcessingActivity targetsMinors(@Nullable Boolean targetsMinors) {
    this.targetsMinors = targetsMinors;
    return this;
  }

  /**
   * Whether the activity processes data of minors.
   * @return targetsMinors
   */
  
  @JsonProperty("targetsMinors")
  public @Nullable Boolean getTargetsMinors() {
    return targetsMinors;
  }

  @JsonProperty("targetsMinors")
  public void setTargetsMinors(@Nullable Boolean targetsMinors) {
    this.targetsMinors = targetsMinors;
  }

  public ProcessingActivity crossBorder(@Nullable Boolean crossBorder) {
    this.crossBorder = crossBorder;
    return this;
  }

  /**
   * Whether the data leaves India.
   * @return crossBorder
   */
  
  @JsonProperty("crossBorder")
  public @Nullable Boolean getCrossBorder() {
    return crossBorder;
  }

  @JsonProperty("crossBorder")
  public void setCrossBorder(@Nullable Boolean crossBorder) {
    this.crossBorder = crossBorder;
  }

  public ProcessingActivity retentionPolicyCode(@Nullable String retentionPolicyCode) {
    this.retentionPolicyCode = retentionPolicyCode;
    return this;
  }

  /**
   * The retention policy that applies; absent while none is set.
   * @return retentionPolicyCode
   */
  
  @JsonProperty("retentionPolicyCode")
  public @Nullable String getRetentionPolicyCode() {
    return retentionPolicyCode;
  }

  @JsonProperty("retentionPolicyCode")
  public void setRetentionPolicyCode(@Nullable String retentionPolicyCode) {
    this.retentionPolicyCode = retentionPolicyCode;
  }

  public ProcessingActivity version(@Nullable Integer version) {
    this.version = version;
    return this;
  }

  /**
   * Version of the activity; incremented on a material change.
   * @return version
   */
  
  @JsonProperty("version")
  public @Nullable Integer getVersion() {
    return version;
  }

  @JsonProperty("version")
  public void setVersion(@Nullable Integer version) {
    this.version = version;
  }

  public ProcessingActivity categories(List<String> categories) {
    this.categories = categories;
    return this;
  }

  public ProcessingActivity addCategoriesItem(String categoriesItem) {
    if (this.categories == null) {
      this.categories = new ArrayList<>();
    }
    this.categories.add(categoriesItem);
    return this;
  }

  /**
   * Codes of the data categories the activity touches.
   * @return categories
   */
  
  @JsonProperty("categories")
  public List<String> getCategories() {
    return categories;
  }

  @JsonProperty("categories")
  public void setCategories(List<String> categories) {
    this.categories = categories;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ProcessingActivity other = (ProcessingActivity) o;
    return Objects.equals(this.id, other.id) &&
        Objects.equals(this.module, other.module) &&
        Objects.equals(this.code, other.code) &&
        Objects.equals(this.purposeCode, other.purposeCode) &&
        Objects.equals(this.legalBasis, other.legalBasis) &&
        Objects.equals(this.educationExemptionClause, other.educationExemptionClause) &&
        Objects.equals(this.targetsMinors, other.targetsMinors) &&
        Objects.equals(this.crossBorder, other.crossBorder) &&
        Objects.equals(this.retentionPolicyCode, other.retentionPolicyCode) &&
        Objects.equals(this.version, other.version) &&
        Objects.equals(this.categories, other.categories);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, module, code, purposeCode, legalBasis, educationExemptionClause, targetsMinors, crossBorder,
        retentionPolicyCode, version, categories);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ProcessingActivity {\n");
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    module: ").append(toIndentedString(module)).append("\n");
    sb.append("    code: ").append(toIndentedString(code)).append("\n");
    sb.append("    purposeCode: ").append(toIndentedString(purposeCode)).append("\n");
    sb.append("    legalBasis: ").append(toIndentedString(legalBasis)).append("\n");
    sb.append("    educationExemptionClause: ").append(toIndentedString(educationExemptionClause)).append("\n");
    sb.append("    targetsMinors: ").append(toIndentedString(targetsMinors)).append("\n");
    sb.append("    crossBorder: ").append(toIndentedString(crossBorder)).append("\n");
    sb.append("    retentionPolicyCode: ").append(toIndentedString(retentionPolicyCode)).append("\n");
    sb.append("    version: ").append(toIndentedString(version)).append("\n");
    sb.append("    categories: ").append(toIndentedString(categories)).append("\n");
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
