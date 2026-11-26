package com.mysms.campus.library.domain;

import com.mysms.core.entity.BaseEntity;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** A row of library.purchase_request_subject. */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class PurchaseRequestSubjectEntity extends BaseEntity {

    public static final String TABLE = "library.purchase_request_subject";

    private UUID tenantId;
    private UUID purchaseRequestId;
    private UUID subjectId;
}
