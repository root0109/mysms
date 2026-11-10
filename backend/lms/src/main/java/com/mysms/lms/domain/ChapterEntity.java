package com.mysms.lms.domain;

import com.mysms.core.entity.BaseEntity;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** A row of lms.chapter (column for column; the audit set comes from {@link BaseEntity}). */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class ChapterEntity extends BaseEntity {

    public static final String TABLE = "lms.chapter";

    private UUID tenantId;
    private UUID courseId;
    private String title;
    private Integer sortOrder;
    private Boolean isScormPackage;
    private UUID scormPackageFileId;
    private String scormPackagePath;
    private String scormManifestFile;
    private String scormLaunchFile;
    private UUID cohortId;
    private UUID parentChapterId;
    private String description;
    private Integer chapterNumber;
    private Integer startDay;
    private LocalTime startTime;
    private Integer dueDay;
    private LocalTime dueTime;
    private OffsetDateTime startsAt;
    private OffsetDateTime endsAt;
    private Boolean allowDelete;
    private Boolean allowEditDetails;
    private Boolean allowEditDates;
    private UUID copiedFromId;
    private Boolean isOriginal;
}
