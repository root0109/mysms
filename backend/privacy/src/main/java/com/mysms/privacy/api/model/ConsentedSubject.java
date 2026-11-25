// Owned API contract of privacy.
package com.mysms.privacy.api.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Objects;
import java.util.UUID;
import org.springframework.lang.Nullable;

/**
 * A person whose data may be processed for an activity at a width (an allowed effective consent).
 */

public class ConsentedSubject {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID subjectPersonId;

  public ConsentedSubject() {
    super();
  }

  public ConsentedSubject subjectPersonId(@Nullable UUID subjectPersonId) {
    this.subjectPersonId = subjectPersonId;
    return this;
  }

  /**
   * The person.
   * @return subjectPersonId
   */
  
  @JsonProperty("subjectPersonId")
  public @Nullable UUID getSubjectPersonId() {
    return subjectPersonId;
  }

  @JsonProperty("subjectPersonId")
  public void setSubjectPersonId(@Nullable UUID subjectPersonId) {
    this.subjectPersonId = subjectPersonId;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ConsentedSubject other = (ConsentedSubject) o;
    return Objects.equals(this.subjectPersonId, other.subjectPersonId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(subjectPersonId);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ConsentedSubject {\n");
    sb.append("    subjectPersonId: ").append(toIndentedString(subjectPersonId)).append("\n");
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
