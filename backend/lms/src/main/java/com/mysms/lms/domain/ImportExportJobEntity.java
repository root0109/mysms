package com.mysms.lms.domain;

import com.mysms.core.entity.BaseEntity;
import java.time.LocalDate;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** A row of lms.import_export_job (column for column; the audit set comes from {@link BaseEntity}). */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class ImportExportJobEntity extends BaseEntity {

    public static final String TABLE = "lms.import_export_job";

    private UUID tenantId;
    private UUID institutionId;
    private String direction;
    private String packageFormat;
    private String scope;
    private UUID courseId;
    private UUID cohortId;
    private UUID targetChapterId;
    private Boolean addToRepository;
    private UUID packageFileId;
    private UUID requestedBy;
    private LocalDate requestedOn;
    private LocalDate startedOn;
    private String status;
    private String unarchiveStatus;
    private String errorLog;
    private String cohortCode;
    private LocalDate cohortStartDate;
    private String courseCode;
    private String courseVersion;
    private String courseTitle;
}
