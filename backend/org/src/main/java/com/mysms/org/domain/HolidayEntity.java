package com.mysms.org.domain;

import com.mysms.core.entity.BaseEntity;
import java.time.LocalDate;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** A row of org.holiday. */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class HolidayEntity extends BaseEntity {

    public static final String TABLE = "org.holiday";

    private UUID tenantId;
    private UUID institutionId;
    private String title;
    private LocalDate startDate;
    private LocalDate endDate;
}
