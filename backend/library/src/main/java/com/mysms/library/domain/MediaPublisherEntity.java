package com.mysms.library.domain;

import com.mysms.core.entity.BaseEntity;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** A row of library.media_publisher. */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class MediaPublisherEntity extends BaseEntity {

    public static final String TABLE = "library.media_publisher";

    private UUID tenantId;
    private UUID mediaId;
    private UUID publisherId;
}
