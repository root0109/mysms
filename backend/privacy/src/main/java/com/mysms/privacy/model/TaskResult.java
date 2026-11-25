package com.mysms.privacy.model;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * What a module answered for its rights-request task, as privacy.rights_request_task.result keeps it (jsonb, written
 * and read by the task's repository): counts and codes only, never a value of the data it erased or exported.
 *
 * @param counts      the rows the module erased or exported, per table
 * @param heldItems   the rows it kept under a legal hold or a legal-retention policy
 * @param policyCodes the retention policies that kept rows
 * @param fileId      the module's export part (a platform file); null for an erasure
 */
public record TaskResult(Map<String, Integer> counts, int heldItems, List<String> policyCodes, UUID fileId) {

    public TaskResult {
        counts = Map.copyOf(counts);
        policyCodes = List.copyOf(policyCodes);
    }

    /** The rows erased or exported over all the module's tables. */
    public int affectedRows() {
        return counts.values().stream().mapToInt(Integer::intValue).sum();
    }
}
