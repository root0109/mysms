package com.mysms.campus.library.domain;

import com.mysms.core.entity.BaseEntity;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** A row of library.purchase_request_course. */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class PurchaseRequestCourseEntity extends BaseEntity {

    public static final String TABLE = "library.purchase_request_course";

    private UUID tenantId;
    private UUID purchaseRequestId;
    private UUID courseId;
}
