package com.mysms.identity.people.domain;

import com.mysms.core.entity.BaseEntity;
import java.time.LocalDate;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** A row of people.greeting. */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class GreetingEntity extends BaseEntity {

    public static final String TABLE = "people.greeting";

    private UUID tenantId;
    private UUID personId;
    private LocalDate greetedOn;
    private String channel;
}
