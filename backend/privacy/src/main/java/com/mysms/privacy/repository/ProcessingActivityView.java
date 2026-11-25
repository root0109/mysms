package com.mysms.privacy.repository;

import java.util.List;
import java.util.UUID;

/** One processing activity of the catalogue with its purpose's code and its data categories' codes. */
public record ProcessingActivityView(UUID id, String module, String code, String purposeCode, String legalBasis,
                                     String educationExemptionClause, boolean targetsMinors, boolean crossBorder,
                                     String retentionPolicyCode, int version, List<String> categories) {

    public ProcessingActivityView {
        categories = List.copyOf(categories);
    }
}
