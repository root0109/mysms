package com.mysms.platform.domain;

import com.mysms.core.entity.BaseEntity;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** A row of platform.reserved_keyword. */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class ReservedKeywordEntity extends BaseEntity {

    public static final String TABLE = "platform.reserved_keyword";
    private String keyword;
}
