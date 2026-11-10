package com.mysms.comms.business;

import com.mysms.authclient.DataScope;
import com.mysms.comms.domain.NotificationPreferenceEntity;
import com.mysms.comms.domain.NotificationPreferenceSetEntity;
import com.mysms.comms.repository.NotificationPreferenceSetRepository;
import com.mysms.core.context.CallContextHolder;
import com.mysms.core.error.ResourceNotFoundException;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** Notification preference rows inherit the privacy boundary of their owning set. */
@Component
public class NotificationPreferenceRules {

    private final NotificationPreferenceSetRepository preferenceSets;

    public NotificationPreferenceRules(NotificationPreferenceSetRepository preferenceSets) {
        this.preferenceSets = preferenceSets;
    }

    /** The caller may reach a preference only through one of their own sets. */
    public void beforeAccess(UUID preferenceSetId, DataScope scope) {
        requireOwnedSet(preferenceSetId);
    }

    /** Every write rechecks the parent set so a preference cannot be attached to another account's set. */
    public void beforeWrite(NotificationPreferenceEntity preference) {
        requireOwnedSet(preference.getPreferenceSetId());
    }

    private void requireOwnedSet(UUID preferenceSetId) {
        NotificationPreferenceSetEntity preferenceSet = preferenceSets.findById(preferenceSetId)
                .orElseThrow(() -> new ResourceNotFoundException(NotificationPreferenceSetEntity.TABLE,
                        preferenceSetId));
        if (!currentUserId().equals(preferenceSet.getUserId())) {
            throw new ResourceNotFoundException(NotificationPreferenceSetEntity.TABLE, preferenceSetId);
        }
    }

    public void beforeUpdate(NotificationPreferenceEntity preference) {
        // The owning set is rechecked by beforeWrite after the requested changes are applied.
    }

    public void beforeDelete(NotificationPreferenceEntity preference) {
        // Ownership was checked by beforeAccess before the row was loaded.
    }

    public void afterDelete(NotificationPreferenceEntity preference) {
        // No additional cleanup is required for a preference row.
    }

    private static UUID currentUserId() {
        return CallContextHolder.require().userId().orElseThrow();
    }
}
