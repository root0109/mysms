// Owned API contract of privacy.
package com.mysms.privacy.api.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.springframework.lang.Nullable;

/**
 * One activity of the tenant's register that may concern the data principal of an access summary.
 */

public class AccessSummaryActivity {

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
  private String activityCode;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private String module;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private String purposeCode;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private LegalBasisEnum legalBasis;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private List<String> dataCategories = new ArrayList<>();

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private List<AccessSummaryRecipient> recipients = new ArrayList<>();

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private Boolean crossBorder;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String retentionPolicyCode;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable List<String> consentAllowedScopes;

  public AccessSummaryActivity() {
    super();
  }

  public AccessSummaryActivity activityCode(String activityCode) {
    this.activityCode = activityCode;
    return this;
  }

  /**
   * Activity code {@code <module>.<activity>}, e.g. comms.operational_notice.
   * @return activityCode
   */
  @NotNull 
  @JsonProperty("activityCode")
  public String getActivityCode() {
    return activityCode;
  }

  @JsonProperty("activityCode")
  public void setActivityCode(String activityCode) {
    this.activityCode = activityCode;
  }

  public AccessSummaryActivity module(String module) {
    this.module = module;
    return this;
  }

  /**
   * The module that processes the data.
   * @return module
   */
  @NotNull 
  @JsonProperty("module")
  public String getModule() {
    return module;
  }

  @JsonProperty("module")
  public void setModule(String module) {
    this.module = module;
  }

  public AccessSummaryActivity purposeCode(String purposeCode) {
    this.purposeCode = purposeCode;
    return this;
  }

  /**
   * Code of the catalogue purpose the activity serves.
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

  public AccessSummaryActivity legalBasis(LegalBasisEnum legalBasis) {
    this.legalBasis = legalBasis;
    return this;
  }

  /**
   * The legal basis of the processing.
   * @return legalBasis
   */
  @NotNull 
  @JsonProperty("legalBasis")
  public LegalBasisEnum getLegalBasis() {
    return legalBasis;
  }

  @JsonProperty("legalBasis")
  public void setLegalBasis(LegalBasisEnum legalBasis) {
    this.legalBasis = legalBasis;
  }

  public AccessSummaryActivity dataCategories(List<String> dataCategories) {
    this.dataCategories = dataCategories;
    return this;
  }

  public AccessSummaryActivity addDataCategoriesItem(String dataCategoriesItem) {
    if (this.dataCategories == null) {
      this.dataCategories = new ArrayList<>();
    }
    this.dataCategories.add(dataCategoriesItem);
    return this;
  }

  /**
   * Codes of the data categories the activity touches, sorted.
   * @return dataCategories
   */
   
  @JsonProperty("dataCategories")
  public List<String> getDataCategories() {
    return dataCategories;
  }

  @JsonProperty("dataCategories")
  public void setDataCategories(List<String> dataCategories) {
    this.dataCategories = dataCategories;
  }

  public AccessSummaryActivity recipients(List<AccessSummaryRecipient> recipients) {
    this.recipients = recipients;
    return this;
  }

  public AccessSummaryActivity addRecipientsItem(AccessSummaryRecipient recipientsItem) {
    if (this.recipients == null) {
      this.recipients = new ArrayList<>();
    }
    this.recipients.add(recipientsItem);
    return this;
  }

  /**
   * Who receives the data, as the register names them; empty while none is named.
   * @return recipients
   */
  @Valid 
  @JsonProperty("recipients")
  public List<AccessSummaryRecipient> getRecipients() {
    return recipients;
  }

  @JsonProperty("recipients")
  public void setRecipients(List<AccessSummaryRecipient> recipients) {
    this.recipients = recipients;
  }

  public AccessSummaryActivity crossBorder(Boolean crossBorder) {
    this.crossBorder = crossBorder;
    return this;
  }

  /**
   * Whether the data leaves India.
   * @return crossBorder
   */
  @NotNull 
  @JsonProperty("crossBorder")
  public Boolean getCrossBorder() {
    return crossBorder;
  }

  @JsonProperty("crossBorder")
  public void setCrossBorder(Boolean crossBorder) {
    this.crossBorder = crossBorder;
  }

  public AccessSummaryActivity retentionPolicyCode(@Nullable String retentionPolicyCode) {
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

  public AccessSummaryActivity consentAllowedScopes(@Nullable List<String> consentAllowedScopes) {
    this.consentAllowedScopes = consentAllowedScopes;
    return this;
  }

  public AccessSummaryActivity addConsentAllowedScopesItem(String consentAllowedScopesItem) {
    if (this.consentAllowedScopes == null) {
      this.consentAllowedScopes = new ArrayList<>();
    }
    this.consentAllowedScopes.add(consentAllowedScopesItem);
    return this;
  }

  /**
   * For an activity on the consent basis, the widths the subject's effective consent allows when the summary was generated, narrowest first (class, institution, inter_school, public, or tenant); empty = not allowed. Absent for any other legal basis.
   * @return consentAllowedScopes
   */
   
  @JsonProperty("consentAllowedScopes")
  public @Nullable List<String> getConsentAllowedScopes() {
    return consentAllowedScopes;
  }

  @JsonProperty("consentAllowedScopes")
  public void setConsentAllowedScopes(@Nullable List<String> consentAllowedScopes) {
    this.consentAllowedScopes = consentAllowedScopes;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    AccessSummaryActivity other = (AccessSummaryActivity) o;
    return Objects.equals(this.activityCode, other.activityCode) &&
        Objects.equals(this.module, other.module) &&
        Objects.equals(this.purposeCode, other.purposeCode) &&
        Objects.equals(this.legalBasis, other.legalBasis) &&
        Objects.equals(this.dataCategories, other.dataCategories) &&
        Objects.equals(this.recipients, other.recipients) &&
        Objects.equals(this.crossBorder, other.crossBorder) &&
        Objects.equals(this.retentionPolicyCode, other.retentionPolicyCode) &&
        Objects.equals(this.consentAllowedScopes, other.consentAllowedScopes);
  }

  @Override
  public int hashCode() {
    return Objects.hash(activityCode, module, purposeCode, legalBasis, dataCategories, recipients, crossBorder, retentionPolicyCode, consentAllowedScopes);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class AccessSummaryActivity {\n");
    sb.append("    activityCode: ").append(toIndentedString(activityCode)).append("\n");
    sb.append("    module: ").append(toIndentedString(module)).append("\n");
    sb.append("    purposeCode: ").append(toIndentedString(purposeCode)).append("\n");
    sb.append("    legalBasis: ").append(toIndentedString(legalBasis)).append("\n");
    sb.append("    dataCategories: ").append(toIndentedString(dataCategories)).append("\n");
    sb.append("    recipients: ").append(toIndentedString(recipients)).append("\n");
    sb.append("    crossBorder: ").append(toIndentedString(crossBorder)).append("\n");
    sb.append("    retentionPolicyCode: ").append(toIndentedString(retentionPolicyCode)).append("\n");
    sb.append("    consentAllowedScopes: ").append(toIndentedString(consentAllowedScopes)).append("\n");
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
