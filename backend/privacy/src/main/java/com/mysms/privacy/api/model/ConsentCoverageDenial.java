// Owned API contract of privacy.
package com.mysms.privacy.api.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import jakarta.validation.constraints.NotNull;
import java.util.Objects;
import org.springframework.lang.Nullable;

/**
 * How many subjects are denied a purpose at a width for one reason.
 */

public class ConsentCoverageDenial {

  /**
   * refused, narrower_grant, expired, reconsent_required, forbidden_for_minor or purpose_disabled.
   */
  public enum ReasonEnum {
    EXPIRED("expired"),
    
    FORBIDDEN_FOR_MINOR("forbidden_for_minor"),
    
    NARROWER_GRANT("narrower_grant"),
    
    PURPOSE_DISABLED("purpose_disabled"),
    
    RECONSENT_REQUIRED("reconsent_required"),
    
    REFUSED("refused");

    private final String value;

    ReasonEnum(String value) {
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
    public static ReasonEnum fromValue(String value) {
      for (ReasonEnum b : ReasonEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private ReasonEnum reason;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private Integer subjects;

  public ConsentCoverageDenial() {
    super();
  }

  public ConsentCoverageDenial reason(ReasonEnum reason) {
    this.reason = reason;
    return this;
  }

  /**
   * refused, narrower_grant, expired, reconsent_required, forbidden_for_minor or purpose_disabled.
   * @return reason
   */
  @NotNull 
  @JsonProperty("reason")
  public ReasonEnum getReason() {
    return reason;
  }

  @JsonProperty("reason")
  public void setReason(ReasonEnum reason) {
    this.reason = reason;
  }

  public ConsentCoverageDenial subjects(Integer subjects) {
    this.subjects = subjects;
    return this;
  }

  /**
   * How many subjects.
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

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ConsentCoverageDenial other = (ConsentCoverageDenial) o;
    return Objects.equals(this.reason, other.reason) &&
        Objects.equals(this.subjects, other.subjects);
  }

  @Override
  public int hashCode() {
    return Objects.hash(reason, subjects);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ConsentCoverageDenial {\n");
    sb.append("    reason: ").append(toIndentedString(reason)).append("\n");
    sb.append("    subjects: ").append(toIndentedString(subjects)).append("\n");
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
