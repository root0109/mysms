package com.mysms.privacyclient;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * What one subject's erasure (or one retention sweep of a subject) changed in a module: the rows per table, the rows
 * kept under a hold, and the ids changed per table for work that follows them (iam signs an account out).
 *
 * @param counts  the rows erased or anonymised, per table
 * @param held    the rows kept under a legal hold
 * @param changed the ids erased or anonymised, per table
 */
public record Erasure(Map<String, Integer> counts, int held, Map<String, List<UUID>> changed) {

    public Erasure {
        counts = Map.copyOf(counts);
        changed = Map.copyOf(changed);
    }

    /** The rows erased or anonymised over all tables. */
    public int affected() {
        return counts.values().stream().mapToInt(Integer::intValue).sum();
    }

    /** The ids changed in one table; empty when none. */
    public List<UUID> changed(String table) {
        return changed.getOrDefault(table, List.of());
    }
}
