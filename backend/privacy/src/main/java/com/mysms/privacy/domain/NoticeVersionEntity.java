package com.mysms.privacy.domain;

import com.mysms.core.entity.BaseEntity;
import com.mysms.privacy.model.GrievanceOfficer;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** A row of privacy.notice_version; the table has no soft delete, so deletedAt stays null. */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class NoticeVersionEntity extends BaseEntity {

    public static final String TABLE = "privacy.notice_version";

    private UUID tenantId;
    private UUID noticeId;
    private Integer version;
    private String status;
    private Boolean requiresReconsent;
    private List<String> purposeCodes;
    private GrievanceOfficer grievanceContact;
    private OffsetDateTime publishedAt;
    private UUID supersededById;
    private UUID documentFileId;
}
