// Owned API contract of privacy.
package com.mysms.privacy.api.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.lang.Nullable;

/**
 * Body of RecordBreachNotification: a notice the school sent about the breach outside MySMS.
 */

public class RecordBreachNotificationRequest {

  /**
   * board (the Data Protection Board), principals (the affected data principals), processor or tenant.
   */
  public enum AudienceEnum {
    BOARD("board"),
    
    PRINCIPALS("principals"),
    
    PROCESSOR("processor"),
    
    TENANT("tenant");

    private final String value;

    AudienceEnum(String value) {
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
    public static AudienceEnum fromValue(String value) {
      for (AudienceEnum b : AudienceEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }

  /**
   * initial (intimation without delay), detailed (the Board's report, the principals' full notice) or update.
   */
  public enum KindEnum {
    DETAILED("detailed"),
    
    INITIAL("initial"),
    
    UPDATE("update");

    private final String value;

    KindEnum(String value) {
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
    public static KindEnum fromValue(String value) {
      for (KindEnum b : KindEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private AudienceEnum audience;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private KindEnum kind;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private OffsetDateTime sentAt;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable UUID documentFileId;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private @Nullable String etag;

  public RecordBreachNotificationRequest() {
    super();
  }

  public RecordBreachNotificationRequest audience(AudienceEnum audience) {
    this.audience = audience;
    return this;
  }

  /**
   * board (the Data Protection Board), principals (the affected data principals), processor or tenant.
   * @return audience
   */
  @NotNull 
  @JsonProperty("audience")
  public AudienceEnum getAudience() {
    return audience;
  }

  @JsonProperty("audience")
  public void setAudience(AudienceEnum audience) {
    this.audience = audience;
  }

  public RecordBreachNotificationRequest kind(KindEnum kind) {
    this.kind = kind;
    return this;
  }

  /**
   * initial (intimation without delay), detailed (the Board's report, the principals' full notice) or update.
   * @return kind
   */
  @NotNull 
  @JsonProperty("kind")
  public KindEnum getKind() {
    return kind;
  }

  @JsonProperty("kind")
  public void setKind(KindEnum kind) {
    this.kind = kind;
  }

  public RecordBreachNotificationRequest sentAt(OffsetDateTime sentAt) {
    this.sentAt = sentAt;
    return this;
  }

  /**
   * When the notice was sent; not in the future, not before the school became aware.
   * @return sentAt
   */
  @NotNull @Valid 
  @JsonProperty("sentAt")
  public OffsetDateTime getSentAt() {
    return sentAt;
  }

  @JsonProperty("sentAt")
  public void setSentAt(OffsetDateTime sentAt) {
    this.sentAt = sentAt;
  }

  public RecordBreachNotificationRequest documentFileId(@Nullable UUID documentFileId) {
    this.documentFileId = documentFileId;
    return this;
  }

  /**
   * The notice as sent (a platform file).
   * @return documentFileId
   */
  @Valid 
  @JsonProperty("documentFileId")
  public @Nullable UUID getDocumentFileId() {
    return documentFileId;
  }

  @JsonProperty("documentFileId")
  public void setDocumentFileId(@Nullable UUID documentFileId) {
    this.documentFileId = documentFileId;
  }

  public RecordBreachNotificationRequest etag(@Nullable String etag) {
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
    RecordBreachNotificationRequest other = (RecordBreachNotificationRequest) o;
    return Objects.equals(this.audience, other.audience) &&
        Objects.equals(this.kind, other.kind) &&
        Objects.equals(this.sentAt, other.sentAt) &&
        Objects.equals(this.documentFileId, other.documentFileId) &&
        Objects.equals(this.etag, other.etag);
  }

  @Override
  public int hashCode() {
    return Objects.hash(audience, kind, sentAt, documentFileId, etag);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class RecordBreachNotificationRequest {\n");
    sb.append("    audience: ").append(toIndentedString(audience)).append("\n");
    sb.append("    kind: ").append(toIndentedString(kind)).append("\n");
    sb.append("    sentAt: ").append(toIndentedString(sentAt)).append("\n");
    sb.append("    documentFileId: ").append(toIndentedString(documentFileId)).append("\n");
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
