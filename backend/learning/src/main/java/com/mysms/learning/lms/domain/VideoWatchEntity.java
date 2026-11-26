package com.mysms.learning.lms.domain;

import com.mysms.core.entity.BaseEntity;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** A row of lms.video_watch. */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class VideoWatchEntity extends BaseEntity {

    public static final String TABLE = "lms.video_watch";

    private UUID tenantId;
    private UUID lessonId;
    private UUID userId;
    private String videoSource;
    private BigDecimal watchSeconds;
}
