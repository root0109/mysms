package com.mysms.privacy.domain;

import com.mysms.core.entity.BaseEntity;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** A row of privacy.notice_translation. */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class NoticeTranslationEntity extends BaseEntity {

    public static final String TABLE = "privacy.notice_translation";

    private UUID tenantId;
    private UUID noticeVersionId;
    private String language;
    private String bodyMarkdown;
    private String summaryForChildren;
    private String contentSha256;
}
