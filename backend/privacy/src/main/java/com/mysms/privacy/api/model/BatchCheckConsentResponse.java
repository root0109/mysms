// Owned API contract of privacy.
package com.mysms.privacy.api.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.springframework.lang.Nullable;

/**
 * One decision per subject asked for, in the order asked.
 */

public class BatchCheckConsentResponse {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private List<ConsentDecision> decisions = new ArrayList<>();

  public BatchCheckConsentResponse() {
    super();
  }

  public BatchCheckConsentResponse decisions(List<ConsentDecision> decisions) {
    this.decisions = decisions;
    return this;
  }

  public BatchCheckConsentResponse addDecisionsItem(ConsentDecision decisionsItem) {
    if (this.decisions == null) {
      this.decisions = new ArrayList<>();
    }
    this.decisions.add(decisionsItem);
    return this;
  }

  /**
   * The decisions, in the order of the request's subjects.
   * @return decisions
   */
  
  @JsonProperty("decisions")
  public List<@Valid ConsentDecision> getDecisions() {
    return decisions;
  }

  @JsonProperty("decisions")
  public void setDecisions(List<ConsentDecision> decisions) {
    this.decisions = decisions;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    BatchCheckConsentResponse other = (BatchCheckConsentResponse) o;
    return Objects.equals(this.decisions, other.decisions);
  }

  @Override
  public int hashCode() {
    return Objects.hash(decisions);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class BatchCheckConsentResponse {\n");
    sb.append("    decisions: ").append(toIndentedString(decisions)).append("\n");
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
