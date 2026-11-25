// Owned API contract of privacy.
package com.mysms.privacy.api.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.springframework.lang.Nullable;

/**
 * The active legal holds that may cover a subject's data: subject-wide holds of the subject and every category-wide or record-set hold of the subject or of everyone.
 */

public class ListActiveHoldsResponse {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private List<ActiveLegalHold> holds;

  public ListActiveHoldsResponse() {
    super();
  }

  public ListActiveHoldsResponse holds(List<ActiveLegalHold> holds) {
    this.holds = holds;
    return this;
  }

  public ListActiveHoldsResponse addHoldsItem(ActiveLegalHold holdsItem) {
    if (this.holds == null) {
      this.holds = new ArrayList<>();
    }
    this.holds.add(holdsItem);
    return this;
  }

  /**
   * The holds, oldest first.
   * @return holds
   */
  @NotNull @Valid 
  @JsonProperty("holds")
  public List<ActiveLegalHold> getHolds() {
    return holds;
  }

  @JsonProperty("holds")
  public void setHolds(List<ActiveLegalHold> holds) {
    this.holds = holds;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ListActiveHoldsResponse other = (ListActiveHoldsResponse) o;
    return Objects.equals(this.holds, other.holds);
  }

  @Override
  public int hashCode() {
    return Objects.hash(holds);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ListActiveHoldsResponse {\n");
    sb.append("    holds: ").append(toIndentedString(holds)).append("\n");
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
