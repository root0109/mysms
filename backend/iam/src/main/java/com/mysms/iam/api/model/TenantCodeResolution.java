// Owned API contract of iam.
package com.mysms.iam.api.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import java.util.Objects;
import java.util.UUID;

/**
 * The active tenant a school code names (iam.ResolveTenantCode): what the edge of a module serving guests binds for
 * a request carrying {@code X-Tenant-Code}.
 */
public class TenantCodeResolution {

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private UUID tenantId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private String tenantCode;

  public TenantCodeResolution() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public TenantCodeResolution(UUID tenantId, String tenantCode) {
    this.tenantId = tenantId;
    this.tenantCode = tenantCode;
  }

  /**
   * Tenant (platform.tenant.id).
   * @return tenantId
   */
  @NotNull
  @JsonProperty("tenantId")
  public UUID getTenantId() {
    return tenantId;
  }

  @JsonProperty("tenantId")
  public void setTenantId(UUID tenantId) {
    this.tenantId = tenantId;
  }

  /**
   * The code as sent, trimmed.
   * @return tenantCode
   */
  @NotNull
  @JsonProperty("tenantCode")
  public String getTenantCode() {
    return tenantCode;
  }

  @JsonProperty("tenantCode")
  public void setTenantCode(String tenantCode) {
    this.tenantCode = tenantCode;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    TenantCodeResolution other = (TenantCodeResolution) o;
    return Objects.equals(this.tenantId, other.tenantId) && Objects.equals(this.tenantCode, other.tenantCode);
  }

  @Override
  public int hashCode() {
    return Objects.hash(tenantId, tenantCode);
  }

  @Override
  public String toString() {
    return "class TenantCodeResolution {\n    tenantId: " + tenantId + "\n    tenantCode: " + tenantCode + "\n}";
  }
}
