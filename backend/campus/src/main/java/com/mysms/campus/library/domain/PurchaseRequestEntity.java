package com.mysms.campus.library.domain;

import com.mysms.core.entity.BaseEntity;
import java.time.LocalDate;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** A row of library.purchase_request. */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class PurchaseRequestEntity extends BaseEntity {

    public static final String TABLE = "library.purchase_request";

    private UUID tenantId;
    private String requestNumber;
    private String title;
    private String authorText;
    private String edition;
    private String publisherText;
    private UUID mediaTypeId;
    private UUID requestedByPersonId;
    private LocalDate requestDate;
    private String status;
}
