package com.mysms.privacy.model;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;
import java.util.UUID;
import java.util.function.Function;

/**
 * The sis.student records of one person as privacy keeps them (privacy.subject.student_records), and the arrays
 * derived from them. An entry changes only to a newer row version, so a late or repeated student event changes
 * nothing; tombstones keep the version of a record that left the person.
 */
public final class StudentRecords {

    private final Map<UUID, StudentRecord> records;

    private StudentRecords(Map<UUID, StudentRecord> records) {
        this.records = records;
    }

    public static StudentRecords of(Map<UUID, StudentRecord> records) {
        return new StudentRecords(new TreeMap<>(records));
    }

    /** The entries by student record id, in id order. */
    public Map<UUID, StudentRecord> records() {
        return Collections.unmodifiableMap(records);
    }

    /** These records with the entry of one student record set; empty when the entry kept is as new or newer. */
    public Optional<StudentRecords> with(UUID studentId, StudentRecord record) {
        StudentRecord kept = records.get(studentId);
        if (kept != null && kept.rowVersion() >= record.rowVersion()) {
            return Optional.empty();
        }
        Map<UUID, StudentRecord> changed = new TreeMap<>(records);
        changed.put(studentId, record);
        return Optional.of(new StudentRecords(changed));
    }

    /** The live student records' ids (privacy.subject.student_ids). */
    public List<UUID> studentIds() {
        return records.entrySet().stream().filter(e -> e.getValue().live()).map(Map.Entry::getKey).toList();
    }

    /** The distinct login accounts of the live records (privacy.subject.user_ids). */
    public List<UUID> userIds() {
        return distinct(StudentRecord::userId);
    }

    /** The distinct institutions of the live records (privacy.subject.institution_ids). */
    public List<UUID> institutionIds() {
        return distinct(StudentRecord::institutionId);
    }

    private List<UUID> distinct(Function<StudentRecord, UUID> field) {
        return records.values().stream().filter(StudentRecord::live).map(field).filter(Objects::nonNull).distinct()
                .sorted().toList();
    }
}
