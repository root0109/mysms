package com.mysms.assessment.config;

/** Service addresses read by assessment workflows for rosters, room capacities, course credits, and student access. */
public record AssessmentProperties(String sisBaseUrl, String facilityBaseUrl, String academicsBaseUrl) {

    public boolean rostersConfigured() {
        return isSet(sisBaseUrl);
    }

    public boolean roomsConfigured() {
        return isSet(facilityBaseUrl);
    }

    public boolean coursePlansConfigured() {
        return isSet(academicsBaseUrl);
    }

    private static boolean isSet(String value) {
        return value != null && !value.isBlank();
    }
}
