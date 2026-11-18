package com.mysms.lms.repository;

import java.util.UUID;

/**
 * The tenant's LMS settings that learner-facing operations read; {@link #defaults()} stands in when the tenant has
 * no row.
 *
 * @param id the settings row; null when the tenant has none yet
 */
public record SettingsView(UUID id, boolean allowGuestAccess, boolean disableSignup, boolean showCourses,
                           boolean showLiveClass, String textDirection, String contactUsEmail, String contactUsUrl,
                           String metaDescription, String metaKeywords, String customSignupContent,
                           boolean enforceVideoCompletion, boolean enforceQuizCompletion, boolean enforceQuizPassing) {

    /** The column defaults of lms.settings, for a tenant that has not saved its settings yet. */
    public static SettingsView defaults() {
        return new SettingsView(null, true, true, true, true, "auto", null, null, null, null, null, true, true, true);
    }
}
