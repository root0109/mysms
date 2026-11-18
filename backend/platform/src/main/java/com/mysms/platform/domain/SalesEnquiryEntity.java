package com.mysms.platform.domain;

import com.mysms.core.entity.BaseEntity;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** A row of platform.sales_enquiry. */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class SalesEnquiryEntity extends BaseEntity {

    public static final String TABLE = "platform.sales_enquiry";
    private String contactName;
    private String email;
    private String phone;
    private String organisationName;
    private String designation;
    private String channel;
    private String message;
    private String status;
}
