package com.mysms.timetable.domain;

import com.mysms.core.entity.BaseEntity;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** A row of timetable.session_student. */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class SessionStudentEntity extends BaseEntity {

    public static final String TABLE = "timetable.session_student";

    private UUID tenantId;
    private UUID sessionId;
    private UUID studentId;
}
