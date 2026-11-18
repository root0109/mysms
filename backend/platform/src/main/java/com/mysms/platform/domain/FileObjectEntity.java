package com.mysms.platform.domain;

import com.mysms.core.entity.BaseEntity;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** A row of platform.file_object. */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class FileObjectEntity extends BaseEntity {

    public static final String TABLE = "platform.file_object";

    private UUID tenantId;
    private String storageDisk;
    private String storageKey;
    private Long storageGeneration;
    private String fileName;
    private String originalName;
    private String mimeType;
    private Long sizeBytes;
    private String checksumSha256;
    private String ownerEntityType;
    private UUID ownerEntityId;
    private String collectionName;
    private Integer sortOrder;
    private String fileKind;
    private Boolean isShared;
    private String processingStatus;
    private Integer processingAttempts;
    private String processingError;
    private String status;
    private OffsetDateTime scannedAt;
    private UUID uploadedByUserId;
}
