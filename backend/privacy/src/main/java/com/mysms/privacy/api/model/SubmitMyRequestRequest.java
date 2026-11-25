// Owned API contract of privacy.
package com.mysms.privacy.api.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.Objects;
import java.util.UUID;
import org.springframework.lang.Nullable;

/**
 * Body of SubmitMyRequest: a rights request about oneself (an adult) or a child one is the active legal guardian of. The requester is always the caller, never a field.
 */

public class SubmitMyRequestRequest {

  /**
   * The right exercised: access (what the school holds and why), correction, grievance, nomination, erasure, export (a copy of the data) or withdraw_all (every consent the requester gave about the subject).
   */
  public enum TypeEnum {
    ACCESS("access"),
    
    CORRECTION("correction"),
    
    ERASURE("erasure"),
    
    EXPORT("export"),
    
    GRIEVANCE("grievance"),
    
    NOMINATION("nomination"),
    
    WITHDRAW_ALL("withdraw_all");

    private final String value;

    TypeEnum(String value) {
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
    public static TypeEnum fromValue(String value) {
      for (TypeEnum b : TypeEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }

  /**
   * For a grievance: what it is about.
   */
  public enum GrievanceCategoryEnum {
    ACCESS("access"),
    
    COMMUNICATION("communication"),
    
    CONSENT("consent"),
    
    CORRECTION("correction"),
    
    DATA_SECURITY("data_security"),
    
    OTHER("other");

    private final String value;

    GrievanceCategoryEnum(String value) {
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
    public static GrievanceCategoryEnum fromValue(String value) {
      for (GrievanceCategoryEnum b : GrievanceCategoryEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private TypeEnum type;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private UUID subjectPersonId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private String description;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable RightsRequestCorrection correction;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable GrievanceCategoryEnum grievanceCategory;

  public SubmitMyRequestRequest() {
    super();
  }

  public SubmitMyRequestRequest type(TypeEnum type) {
    this.type = type;
    return this;
  }

  /**
   * The right exercised: access (what the school holds and why), correction, grievance, nomination, erasure, export (a copy of the data) or withdraw_all (every consent the requester gave about the subject).
   * @return type
   */
  @NotNull 
  @JsonProperty("type")
  public TypeEnum getType() {
    return type;
  }

  @JsonProperty("type")
  public void setType(TypeEnum type) {
    this.type = type;
  }

  public SubmitMyRequestRequest subjectPersonId(UUID subjectPersonId) {
    this.subjectPersonId = subjectPersonId;
    return this;
  }

  /**
   * The caller, or a minor the caller is the active legal guardian of.
   * @return subjectPersonId
   */
  @NotNull @Valid 
  @JsonProperty("subjectPersonId")
  public UUID getSubjectPersonId() {
    return subjectPersonId;
  }

  @JsonProperty("subjectPersonId")
  public void setSubjectPersonId(UUID subjectPersonId) {
    this.subjectPersonId = subjectPersonId;
  }

  public SubmitMyRequestRequest description(String description) {
    this.description = description;
    return this;
  }

  /**
   * What the requester asks, in their own words.
   * @return description
   */
  @NotNull @NotBlank @Size(max = 4000) 
  @JsonProperty("description")
  public String getDescription() {
    return description;
  }

  @JsonProperty("description")
  public void setDescription(String description) {
    this.description = description;
  }

  public SubmitMyRequestRequest correction(@Nullable RightsRequestCorrection correction) {
    this.correction = correction;
    return this;
  }

  /**
   * For a correction (required): the record and the fields to correct. Not allowed for another type.
   * @return correction
   */
  @Valid 
  @JsonProperty("correction")
  public @Nullable RightsRequestCorrection getCorrection() {
    return correction;
  }

  @JsonProperty("correction")
  public void setCorrection(@Nullable RightsRequestCorrection correction) {
    this.correction = correction;
  }

  public SubmitMyRequestRequest grievanceCategory(@Nullable GrievanceCategoryEnum grievanceCategory) {
    this.grievanceCategory = grievanceCategory;
    return this;
  }

  /**
   * For a grievance (required): what it is about. Not allowed for another type.
   * @return grievanceCategory
   */
   
  @JsonProperty("grievanceCategory")
  public @Nullable GrievanceCategoryEnum getGrievanceCategory() {
    return grievanceCategory;
  }

  @JsonProperty("grievanceCategory")
  public void setGrievanceCategory(@Nullable GrievanceCategoryEnum grievanceCategory) {
    this.grievanceCategory = grievanceCategory;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    SubmitMyRequestRequest other = (SubmitMyRequestRequest) o;
    return Objects.equals(this.type, other.type) &&
        Objects.equals(this.subjectPersonId, other.subjectPersonId) &&
        Objects.equals(this.description, other.description) &&
        Objects.equals(this.correction, other.correction) &&
        Objects.equals(this.grievanceCategory, other.grievanceCategory);
  }

  @Override
  public int hashCode() {
    return Objects.hash(type, subjectPersonId, description, correction, grievanceCategory);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class SubmitMyRequestRequest {\n");
    sb.append("    type: ").append(toIndentedString(type)).append("\n");
    sb.append("    subjectPersonId: ").append(toIndentedString(subjectPersonId)).append("\n");
    sb.append("    description: ").append(toIndentedString(description)).append("\n");
    sb.append("    correction: ").append(toIndentedString(correction)).append("\n");
    sb.append("    grievanceCategory: ").append(toIndentedString(grievanceCategory)).append("\n");
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
