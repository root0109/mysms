package com.mysms.privacy.repository;

import com.mysms.privacy.model.AccessSummarySnapshot;
import java.util.List;

/**
 * One processing activity of the register that applies to a subject, with its purpose's code, its data categories'
 * codes (sorted), its recipients and the widths the subject's effective consent for the purpose allows (unordered).
 */
public record AppliedActivityView(String code, String module, String purposeCode, String legalBasis,
                                  List<String> categories, List<AccessSummarySnapshot.Recipient> recipients,
                                  boolean crossBorder, String retentionPolicyCode, List<String> allowedScopes) {

    public AppliedActivityView {
        categories = List.copyOf(categories);
        recipients = List.copyOf(recipients);
        allowedScopes = List.copyOf(allowedScopes);
    }
}
