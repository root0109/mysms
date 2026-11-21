// Owned API contract of privacy.
package com.mysms.privacy.api.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.Objects;
import org.springframework.lang.Nullable;

/**
 * The school's grievance officer a notice version names: whom a student or guardian complains to.
 */

public class GrievanceContact {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private String name;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String role;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private String email;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String phone;

  public GrievanceContact() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public GrievanceContact(String name, String email) {
    this.name = name;
    this.email = email;
  }

  public GrievanceContact name(String name) {
    this.name = name;
    return this;
  }

  /**
   * Name of the officer.
   * @return name
   */
  @NotNull @Size(max = 200) 
  @JsonProperty("name")
  public String getName() {
    return name;
  }

  @JsonProperty("name")
  public void setName(String name) {
    this.name = name;
  }

  public GrievanceContact role(@Nullable String role) {
    this.role = role;
    return this;
  }

  /**
   * Role of the officer, e.g. Data Protection Officer.
   * @return role
   */
  @Size(max = 200) 
  @JsonProperty("role")
  public @Nullable String getRole() {
    return role;
  }

  @JsonProperty("role")
  public void setRole(@Nullable String role) {
    this.role = role;
  }

  public GrievanceContact email(String email) {
    this.email = email;
    return this;
  }

  /**
   * E-mail address complaints are sent to.
   * @return email
   */
  @NotNull @Email @Size(max = 320) 
  @JsonProperty("email")
  public String getEmail() {
    return email;
  }

  @JsonProperty("email")
  public void setEmail(String email) {
    this.email = email;
  }

  public GrievanceContact phone(@Nullable String phone) {
    this.phone = phone;
    return this;
  }

  /**
   * Telephone number.
   * @return phone
   */
  @Size(max = 30) 
  @JsonProperty("phone")
  public @Nullable String getPhone() {
    return phone;
  }

  @JsonProperty("phone")
  public void setPhone(@Nullable String phone) {
    this.phone = phone;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    GrievanceContact other = (GrievanceContact) o;
    return Objects.equals(this.name, other.name) &&
        Objects.equals(this.role, other.role) &&
        Objects.equals(this.email, other.email) &&
        Objects.equals(this.phone, other.phone);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, role, email, phone);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class GrievanceContact {\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    role: ").append(toIndentedString(role)).append("\n");
    sb.append("    email: ").append(toIndentedString(email)).append("\n");
    sb.append("    phone: ").append(toIndentedString(phone)).append("\n");
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
