package com.mysms.privacy.repository;

import com.mysms.privacy.model.AccessSummarySnapshot;
import java.util.List;

/**
 * One activity of the processing register as its export lists it: purpose, basis, whom it reaches, the data
 * categories' codes (sorted), the audiences and the recipients.
 */
public record RegisterEntryView(String code, String module, String purposeCode, String legalBasis,
                                String educationExemptionClause, boolean targetsMinors, List<String> audiences,
                                List<String> categories, List<AccessSummarySnapshot.Recipient> recipients,
                                boolean crossBorder, String retentionPolicyCode) {

    public RegisterEntryView {
        audiences = List.copyOf(audiences);
        categories = List.copyOf(categories);
        recipients = List.copyOf(recipients);
    }
}
