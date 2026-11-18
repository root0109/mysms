package com.mysms.comms.domain;

import com.mysms.core.entity.BaseEntity;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** A row of comms.notice_background. */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class NoticeBackgroundEntity extends BaseEntity {

    public static final String TABLE = "comms.notice_background";

    private UUID tenantId;
    private UUID institutionId;
    private String usage;
    private UUID imageFileId;
}
