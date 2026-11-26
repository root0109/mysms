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
 * The coverage of a purpose at one width: counts of subjects, never who they are.
 */

public class ConsentCoverageScope {

  /**
   * The width: class, institution, inter_school, public or tenant.
   */
  public enum ScopeEnum {
    CLASS("class"),
    
    INSTITUTION("institution"),
    
    INTER_SCHOOL("inter_school"),
    
    PUBLIC("public"),
    
    TENANT("tenant");

    private final String value;

    ScopeEnum(String value) {
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
    public static ScopeEnum fromValue(String value) {
      for (ScopeEnum b : ScopeEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private ScopeEnum scope;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private Integer allowed;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private Integer notAnswered;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private List<ConsentCoverageDenial> denied = new ArrayList<>();

  public ConsentCoverageScope() {
    super();
  }

  public ConsentCoverageScope scope(ScopeEnum scope) {
    this.scope = scope;
    return this;
  }

  /**
   * The width: class, institution, inter_school, public or tenant.
   * @return scope
   */
  @NotNull 
  @JsonProperty("scope")
  public ScopeEnum getScope() {
    return scope;
  }

  @JsonProperty("scope")
  public void setScope(ScopeEnum scope) {
    this.scope = scope;
  }

  public ConsentCoverageScope allowed(Integer allowed) {
    this.allowed = allowed;
    return this;
  }

  /**
   * Subjects whose effective consent allows the purpose at the width.
   * @return allowed
   */
  @NotNull 
  @JsonProperty("allowed")
  public Integer getAllowed() {
    return allowed;
  }

  @JsonProperty("allowed")
  public void setAllowed(Integer allowed) {
    this.allowed = allowed;
  }

  public ConsentCoverageScope notAnswered(Integer notAnswered) {
    this.notAnswered = notAnswered;
    return this;
  }

  /**
   * Subjects with no decision that counts: nobody answered, or no answer was computed yet.
   * @return notAnswered
   */
  @NotNull 
  @JsonProperty("notAnswered")
  public Integer getNotAnswered() {
    return notAnswered;
  }

  @JsonProperty("notAnswered")
  public void setNotAnswered(Integer notAnswered) {
    this.notAnswered = notAnswered;
  }

  public ConsentCoverageScope denied(List<ConsentCoverageDenial> denied) {
    this.denied = denied;
    return this;
  }

  public ConsentCoverageScope addDeniedItem(ConsentCoverageDenial deniedItem) {
    if (this.denied == null) {
      this.denied = new ArrayList<>();
    }
    this.denied.add(deniedItem);
    return this;
  }

  /**
   * Subjects denied for another reason, one entry per reason (zero included).
   * @return denied
   */
  @NotNull 
  @JsonProperty("denied")
  public List<@Valid ConsentCoverageDenial> getDenied() {
    return denied;
  }

  @JsonProperty("denied")
  public void setDenied(List<ConsentCoverageDenial> denied) {
    this.denied = denied;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ConsentCoverageScope other = (ConsentCoverageScope) o;
    return Objects.equals(this.scope, other.scope) &&
        Objects.equals(this.allowed, other.allowed) &&
        Objects.equals(this.notAnswered, other.notAnswered) &&
        Objects.equals(this.denied, other.denied);
  }

  @Override
  public int hashCode() {
    return Objects.hash(scope, allowed, notAnswered, denied);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ConsentCoverageScope {\n");
    sb.append("    scope: ").append(toIndentedString(scope)).append("\n");
    sb.append("    allowed: ").append(toIndentedString(allowed)).append("\n");
    sb.append("    notAnswered: ").append(toIndentedString(notAnswered)).append("\n");
    sb.append("    denied: ").append(toIndentedString(denied)).append("\n");
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
