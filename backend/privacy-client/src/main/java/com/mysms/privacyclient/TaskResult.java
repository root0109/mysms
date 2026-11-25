package com.mysms.privacyclient;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * A module's answer to its rights-request task, published as {@code <module>.privacy_task.completed} (design §6.3):
 * counts and codes only, never a value of the data it erased or exported.
 *
 * @param status      what came of the task
 * @param counts      the rows the module erased, anonymised or exported, per table ({@code <module>.<table>})
 * @param heldItems   the rows it kept under a legal hold or a legal-retention policy
 * @param policyCodes the retention policies that kept rows
 * @param fileId      the module's export part (a platform file); null when there is none
 * @param errorCode   why the task failed; null unless failed
 */
public record TaskResult(Status status, Map<String, Integer> counts, int heldItems, List<String> policyCodes,
                         UUID fileId, String errorCode) {

    /** What came of a task, as privacy reads it. */
    public enum Status {
        DONE, PARTIAL, FAILED, NOT_APPLICABLE;

        public String value() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    public TaskResult {
        Objects.requireNonNull(status, "status");
        counts = Map.copyOf(counts);
        policyCodes = List.copyOf(policyCodes);
    }

    /**
     * The answer of an erasure: partial when rows were kept, not applicable when the module held nothing of the
     * subject, done otherwise.
     */
    public static TaskResult erased(Erasure erasure, List<String> policyCodes) {
        Status status = erasure.held() > 0 || !policyCodes.isEmpty() ? Status.PARTIAL
                : erasure.affected() == 0 ? Status.NOT_APPLICABLE : Status.DONE;
        return new TaskResult(status, erasure.counts(), erasure.held(), policyCodes, null, null);
    }

    /** The answer of an export: the rows per table and the part's file (null while no file is written). */
    public static TaskResult exported(Map<String, Integer> counts, UUID fileId) {
        boolean none = counts.values().stream().allMatch(rows -> rows == 0);
        return new TaskResult(none ? Status.NOT_APPLICABLE : Status.DONE, counts, 0, List.of(), fileId, null);
    }

    /** The module holds nothing the action applies to. */
    public static TaskResult notApplicable() {
        return new TaskResult(Status.NOT_APPLICABLE, Map.of(), 0, List.of(), null, null);
    }

    /** The task failed and nothing was removed; privacy dispatches it again. */
    public static TaskResult failed(String errorCode) {
        return new TaskResult(Status.FAILED, Map.of(), 0, List.of(), null, Objects.requireNonNull(errorCode));
    }

    /** The payload of {@code <module>.privacy_task.completed}, as privacy's TaskAnswerHandler reads it. */
    public Map<String, Object> answer(PrivacyTask task) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("taskId", task.taskId().toString());
        payload.put("requestId", task.requestId().toString());
        payload.put("status", status.value());
        if (status == Status.FAILED) {
            payload.put("errorCode", errorCode);
            return payload;
        }
        payload.put("counts", counts);
        payload.put("heldItems", heldItems);
        payload.put("policyCodes", policyCodes);
        payload.put("fileId", Objects.toString(fileId, null));
        return payload;
    }
}
