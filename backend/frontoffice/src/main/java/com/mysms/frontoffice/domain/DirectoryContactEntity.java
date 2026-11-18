package com.mysms.frontoffice.domain;

import com.mysms.core.entity.BaseEntity;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** A row of frontoffice.directory_contact. */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class DirectoryContactEntity extends BaseEntity {

    public static final String TABLE = "frontoffice.directory_contact";

    private UUID tenantId;
    private UUID institutionId;
    private String name;
    private String designation;
    private String phone;
}
