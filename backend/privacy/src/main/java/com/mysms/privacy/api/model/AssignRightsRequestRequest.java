// Owned API contract of privacy.
package com.mysms.privacy.api.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.Objects;
import java.util.UUID;
import org.springframework.lang.Nullable;

/**
 * Body of AssignRightsRequest: the staff member who handles the request.
 */

public class AssignRightsRequestRequest {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private UUID assigneeUserId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String etag;

  public AssignRightsRequestRequest() {
    super();
  }

  public AssignRightsRequestRequest assigneeUserId(UUID assigneeUserId) {
    this.assigneeUserId = assigneeUserId;
    return this;
  }

  /**
   * The staff member's user account.
   * @return assigneeUserId
   */
  @NotNull @Valid 
  @JsonProperty("assigneeUserId")
  public UUID getAssigneeUserId() {
    return assigneeUserId;
  }

  @JsonProperty("assigneeUserId")
  public void setAssigneeUserId(UUID assigneeUserId) {
    this.assigneeUserId = assigneeUserId;
  }

  public AssignRightsRequestRequest etag(@Nullable String etag) {
    this.etag = etag;
    return this;
  }

  /**
   * Concurrency token from the last read (optional but recommended).
   * @return etag
   */
   
  @JsonProperty("etag")
  public @Nullable String getEtag() {
    return etag;
  }

  @JsonProperty("etag")
  public void setEtag(@Nullable String etag) {
    this.etag = etag;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    AssignRightsRequestRequest other = (AssignRightsRequestRequest) o;
    return Objects.equals(this.assigneeUserId, other.assigneeUserId) &&
        Objects.equals(this.etag, other.etag);
  }

  @Override
  public int hashCode() {
    return Objects.hash(assigneeUserId, etag);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class AssignRightsRequestRequest {\n");
    sb.append("    assigneeUserId: ").append(toIndentedString(assigneeUserId)).append("\n");
    sb.append("    etag: ").append(toIndentedString(etag)).append("\n");
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
