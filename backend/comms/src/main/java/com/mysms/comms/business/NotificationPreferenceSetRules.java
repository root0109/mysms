package com.mysms.comms.business;

import com.mysms.authclient.DataScope;
import com.mysms.comms.domain.NotificationPreferenceSetEntity;
import com.mysms.core.context.CallContextHolder;
import com.mysms.core.error.ResourceNotFoundException;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** Notification preference sets are private to the signed-in account. */
@Component
public class NotificationPreferenceSetRules {

    /** A tenant permission does not grant access to another account's personal notification settings. */
    public void beforeAccess(UUID ownerUserId, UUID preferenceSetId, DataScope scope) {
        if (!currentUserId().equals(ownerUserId)) {
            throw new ResourceNotFoundException(NotificationPreferenceSetEntity.TABLE, preferenceSetId);
        }
    }

    /** Creates and updates must preserve ownership by the signed-in account. */
    public void beforeWrite(NotificationPreferenceSetEntity preferenceSet) {
        if (!currentUserId().equals(preferenceSet.getUserId())) {
            throw new ResourceNotFoundException(NotificationPreferenceSetEntity.TABLE, preferenceSet.getId());
        }
    }

    public void beforeUpdate(NotificationPreferenceSetEntity preferenceSet) {
        // Ownership was checked by beforeAccess; beforeWrite checks it again after applying changes.
    }

    public void beforeDelete(NotificationPreferenceSetEntity preferenceSet) {
        // Ownership was checked by beforeAccess before the row was loaded.
    }

    public void afterDelete(NotificationPreferenceSetEntity preferenceSet) {
        // No additional cleanup is required for a preference set.
    }

    private static UUID currentUserId() {
        return CallContextHolder.require().userId().orElseThrow();
    }
}
