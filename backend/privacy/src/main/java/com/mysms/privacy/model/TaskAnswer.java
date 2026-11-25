package com.mysms.privacy.model;

import java.util.UUID;

/**
 * A module's answer to its rights-request task, read from its {@code <module>.privacy_task.completed} event.
 *
 * @param taskId    the task answered
 * @param requestId the request the task fulfils
 * @param module    the module that answered (the event's publisher)
 * @param status    done, partial, failed or not_applicable
 * @param result    what the module did, in counts and codes; null for a failed answer
 * @param errorCode why the module failed; null unless failed
 */
public record TaskAnswer(UUID taskId, UUID requestId, String module, String status, TaskResult result,
                         String errorCode) {
}
