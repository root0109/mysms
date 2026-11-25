// Owned API contract of privacy.
package com.mysms.privacy.api.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import java.util.Objects;
import org.springframework.lang.Nullable;

/**
 * A recipient of a processing activity's data, as the processing register names it.
 */

public class AccessSummaryRecipient {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private String kind;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private String name;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String country;

  public AccessSummaryRecipient() {
    super();
  }

  public AccessSummaryRecipient kind(String kind) {
    this.kind = kind;
    return this;
  }

  /**
   * processor (processes the data on the school's behalf) or fiduciary (decides on its own account).
   * @return kind
   */
  @NotNull 
  @JsonProperty("kind")
  public String getKind() {
    return kind;
  }

  @JsonProperty("kind")
  public void setKind(String kind) {
    this.kind = kind;
  }

  public AccessSummaryRecipient name(String name) {
    this.name = name;
    return this;
  }

  /**
   * The recipient's name.
   * @return name
   */
  @NotNull 
  @JsonProperty("name")
  public String getName() {
    return name;
  }

  @JsonProperty("name")
  public void setName(String name) {
    this.name = name;
  }

  public AccessSummaryRecipient country(@Nullable String country) {
    this.country = country;
    return this;
  }

  /**
   * Where the recipient processes the data; absent when the register does not say.
   * @return country
   */
   
  @JsonProperty("country")
  public @Nullable String getCountry() {
    return country;
  }

  @JsonProperty("country")
  public void setCountry(@Nullable String country) {
    this.country = country;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    AccessSummaryRecipient other = (AccessSummaryRecipient) o;
    return Objects.equals(this.kind, other.kind) &&
        Objects.equals(this.name, other.name) &&
        Objects.equals(this.country, other.country);
  }

  @Override
  public int hashCode() {
    return Objects.hash(kind, name, country);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class AccessSummaryRecipient {\n");
    sb.append("    kind: ").append(toIndentedString(kind)).append("\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    country: ").append(toIndentedString(country)).append("\n");
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
