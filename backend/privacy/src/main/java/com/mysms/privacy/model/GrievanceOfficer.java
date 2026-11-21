package com.mysms.privacy.model;

/**
 * The grievance officer a notice version names, as privacy.notice_version.grievance_contact keeps it (jsonb, written
 * and read by the version's repository).
 */
public record GrievanceOfficer(String name, String role, String email, String phone) {
}
