// Owned API contract of privacy.
package com.mysms.privacy.api.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.lang.Nullable;

/**
 * The decisions asked for at once: one activity and width, up to 1000 subjects.
 */

public class BatchCheckConsentRequest {

  /**
   * The width the processing reaches (tenant for a purpose decided for the whole tenant).
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
  private String activityCode;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private List<UUID> subjectPersonIds = new ArrayList<>();

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private ScopeEnum scope;

  public BatchCheckConsentRequest() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public BatchCheckConsentRequest(String activityCode, List<UUID> subjectPersonIds, ScopeEnum scope) {
    this.activityCode = activityCode;
    this.subjectPersonIds = subjectPersonIds;
    this.scope = scope;
  }

  public BatchCheckConsentRequest activityCode(String activityCode) {
    this.activityCode = activityCode;
    return this;
  }

  /**
   * The processing activity, e.g. comms.marketing_broadcast.
   * @return activityCode
   */
  @NotNull @Size(max = 60) 
  @JsonProperty("activityCode")
  public String getActivityCode() {
    return activityCode;
  }

  @JsonProperty("activityCode")
  public void setActivityCode(String activityCode) {
    this.activityCode = activityCode;
  }

  public BatchCheckConsentRequest subjectPersonIds(List<UUID> subjectPersonIds) {
    this.subjectPersonIds = subjectPersonIds;
    return this;
  }

  public BatchCheckConsentRequest addSubjectPersonIdsItem(UUID subjectPersonIdsItem) {
    if (this.subjectPersonIds == null) {
      this.subjectPersonIds = new ArrayList<>();
    }
    this.subjectPersonIds.add(subjectPersonIdsItem);
    return this;
  }

  /**
   * The persons to decide for (1-1000).
   * @return subjectPersonIds
   */
  @NotNull @Size(min = 1, max = 1000) 
  @JsonProperty("subjectPersonIds")
  public List<UUID> getSubjectPersonIds() {
    return subjectPersonIds;
  }

  @JsonProperty("subjectPersonIds")
  public void setSubjectPersonIds(List<UUID> subjectPersonIds) {
    this.subjectPersonIds = subjectPersonIds;
  }

  public BatchCheckConsentRequest scope(ScopeEnum scope) {
    this.scope = scope;
    return this;
  }

  /**
   * The width the processing reaches.
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

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    BatchCheckConsentRequest other = (BatchCheckConsentRequest) o;
    return Objects.equals(this.activityCode, other.activityCode) &&
        Objects.equals(this.subjectPersonIds, other.subjectPersonIds) &&
        Objects.equals(this.scope, other.scope);
  }

  @Override
  public int hashCode() {
    return Objects.hash(activityCode, subjectPersonIds, scope);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class BatchCheckConsentRequest {\n");
    sb.append("    activityCode: ").append(toIndentedString(activityCode)).append("\n");
    sb.append("    subjectPersonIds: ").append(toIndentedString(subjectPersonIds)).append("\n");
    sb.append("    scope: ").append(toIndentedString(scope)).append("\n");
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
